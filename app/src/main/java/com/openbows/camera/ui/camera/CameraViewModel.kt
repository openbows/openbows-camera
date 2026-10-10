package com.openbows.camera.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openbows.camera.OpenbowsApplication
import com.openbows.camera.R
import com.openbows.camera.data.PhotoRecord
import com.openbows.camera.data.WatermarkTemplate
import com.openbows.camera.location.FieldSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class CameraUiState(
    val capturing: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashEnabled: Boolean = false,
    val linearZoom: Float = 0f,
    val message: String? = null,
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OpenbowsApplication
    private val container = app.container
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null

    val template: StateFlow<WatermarkTemplate?> = container.templateRepository.currentTemplate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val field: StateFlow<FieldSnapshot> = container.locationTracker.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FieldSnapshot())

    val latestPhoto: StateFlow<PhotoRecord?> = container.photoRepository.latest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coordSystem: StateFlow<com.openbows.camera.data.CoordSystem> =
        container.preferences.coordSystemFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.openbows.camera.data.CoordSystem.WGS84)

    val remarkHistory: StateFlow<List<com.openbows.camera.data.RemarkHistory>> =
        container.remarkHistoryRepository.history
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(CameraUiState())
    val ui: StateFlow<CameraUiState> = _ui

    fun startLocation() = container.locationTracker.start()
    fun stopLocation() = container.locationTracker.stop()

    fun attachCamera(bound: Camera?) {
        camera = bound
        applyZoom(_ui.value.linearZoom)
    }

    fun setLinearZoom(value: Float) {
        val zoom = value.coerceIn(0f, 1f)
        _ui.value = _ui.value.copy(linearZoom = zoom)
        applyZoom(zoom)
    }

    fun onPinchZoom(zoomDelta: Float) {
        setLinearZoom(_ui.value.linearZoom + (zoomDelta - 1f) * 0.85f)
    }

    private fun applyZoom(linear: Float) {
        runCatching { camera?.cameraControl?.setLinearZoom(linear) }
    }

    fun focusAt(factory: MeteringPointFactory, x: Float, y: Float) {
        val cam = camera ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        if (!cam.cameraInfo.isFocusMeteringSupported(action)) return
        runCatching { cam.cameraControl.startFocusAndMetering(action) }
    }

    fun toggleLens() {
        val next = if (_ui.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _ui.value = _ui.value.copy(lensFacing = next, flashEnabled = false, linearZoom = 0f)
        applyZoom(0f)
    }

    fun toggleFlash() {
        _ui.value = _ui.value.copy(flashEnabled = !_ui.value.flashEnabled)
    }

    fun updateRemark(text: String) {
        val current = template.value ?: return
        val clipped = text.take(500)
        viewModelScope.launch {
            container.templateRepository.save(
                current.copy(
                    remark = clipped,
                    showRemark = true,
                ),
            )
            container.remarkHistoryRepository.remember(clipped)
        }
    }

    fun deleteRemarkHistory(item: com.openbows.camera.data.RemarkHistory) {
        viewModelScope.launch { container.remarkHistoryRepository.delete(item) }
    }

    fun capture(imageCapture: ImageCapture?) {
        if (imageCapture == null || _ui.value.capturing) return
        val tmpl = template.value ?: return
        _ui.value = _ui.value.copy(capturing = true)
        imageCapture.flashMode = if (_ui.value.flashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        imageCapture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    viewModelScope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            runCatching {
                                val bitmap = imageProxyToBitmap(image)
                                val burned = container.watermarkRenderer.burn(
                                    bitmap,
                                    tmpl,
                                    field.value,
                                    coordSystem.value,
                                )
                                if (burned !== bitmap) bitmap.recycle()
                                val saved = container.photoRepository.saveJpeg(burned, tmpl.remark)
                                if (!burned.isRecycled) burned.recycle()
                                saved
                            }.isSuccess
                        }
                        image.close()
                        _ui.value = _ui.value.copy(capturing = false)
                        val msg = if (ok) {
                            app.getString(R.string.saved)
                        } else {
                            app.getString(R.string.save_failed)
                        }
                        Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _ui.value = _ui.value.copy(capturing = false)
                    Toast.makeText(app, app.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    override fun onCleared() {
        captureExecutor.shutdown()
        camera = null
        container.locationTracker.stop()
        super.onCleared()
    }
}
