package com.openbows.camera.ui.camera

import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.openbows.camera.BuildConfig
import com.openbows.camera.R
import com.openbows.camera.watermark.WatermarkRenderer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    onOpenAlbum: () -> Unit,
    onOpenTemplates: () -> Unit,
    vm: CameraViewModel = viewModel(),
) {
    val template by vm.template.collectAsStateWithLifecycle()
    val field by vm.field.collectAsStateWithLifecycle()
    val latest by vm.latestPhoto.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val coordSystem by vm.coordSystem.collectAsStateWithLifecycle()
    val remarkHistory by vm.remarkHistory.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var watermarkBounds by remember { mutableStateOf(RectF()) }
    var showRemarkDialog by remember { mutableStateOf(false) }
    var remarkDraft by remember { mutableStateOf("") }
    var focusIndicator by remember { mutableStateOf<Offset?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }
    val renderer = remember { WatermarkRenderer(context) }

    DisposableEffect(Unit) {
        vm.startLocation()
        onDispose {
            vm.stopLocation()
            vm.attachCamera(null)
        }
    }

    LaunchedEffect(ui.lensFacing, lifecycleOwner) {
        val cameraProvider = suspendCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                { cont.resume(future.get()) },
                ContextCompat.getMainExecutor(context),
            )
        }
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture
        val selector = CameraSelector.Builder()
            .requireLensFacing(ui.lensFacing)
            .build()
        runCatching {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            vm.attachCamera(camera)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color(0xFF1B1F24),
            ) {
                AboutDrawerContent()
            }
        },
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101216)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0xFF1B1F24))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .clickable { scope.launch { drawerState.open() } },
            ) {
                Text(
                    "${stringResource(R.string.app_name)} Openbows Camera",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOf(
                        template?.name ?: "模板",
                        if (field.hasFix) "定位已就绪" else "定位中",
                        if (!field.weatherPending && field.weather != null) "天气已就绪" else "天气未就绪",
                    ).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = vm::toggleFlash) {
                Icon(
                    if (ui.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(R.string.flash),
                    tint = Color.White,
                )
            }
            IconButton(onClick = vm::toggleLens) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.flip_camera),
                    tint = Color.White,
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            val tmpl = template
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(ui.lensFacing) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) vm.onPinchZoom(zoom)
                        }
                    }
                    .pointerInput(template, field, coordSystem) {
                        detectTapGestures(
                            onTap = { offset ->
                                vm.focusAt(previewView.meteringPointFactory, offset.x, offset.y)
                                focusIndicator = offset
                            },
                            onLongPress = { offset ->
                                if (watermarkBounds.contains(offset.x, offset.y)) {
                                    remarkDraft = template?.remark.orEmpty()
                                    showRemarkDialog = true
                                }
                            },
                        )
                    },
            ) {
                if (tmpl != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            watermarkBounds = renderer.draw(
                                canvas.nativeCanvas,
                                size.width,
                                size.height,
                                tmpl,
                                field,
                                coordSystem,
                            )
                        }
                    }
                }
                val indicator = focusIndicator
                if (indicator != null) {
                    val ring = with(density) { 28.dp.toPx() }
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White,
                            radius = ring,
                            center = indicator,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                    LaunchedEffect(indicator) {
                        delay(900)
                        if (focusIndicator == indicator) focusIndicator = null
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1F24))
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onOpenAlbum),
                contentAlignment = Alignment.Center,
            ) {
                if (latest != null) {
                    AsyncImage(
                        model = Uri.parse(latest!!.uri),
                        contentDescription = stringResource(R.string.album),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = stringResource(R.string.album), tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !ui.capturing) { vm.capture(imageCapture) }
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5C15C)),
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onOpenTemplates),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "水印",
                    color = Color(0xFFF5C15C),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showRemarkDialog) {
        AlertDialog(
            onDismissRequest = { showRemarkDialog = false },
            title = { Text("备注") },
            text = {
                Column {
                    OutlinedTextField(
                        value = remarkDraft,
                        onValueChange = { remarkDraft = it.take(500) },
                        label = { Text("备注内容（最多500字）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 3,
                    )
                    if (remarkHistory.isNotEmpty()) {
                        Text(
                            "历史备注",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        LazyColumn(Modifier.heightIn(max = 180.dp)) {
                            items(remarkHistory, key = { it.id }) { item ->
                                ListItem(
                                    headlineContent = { Text(item.text, maxLines = 2) },
                                    trailingContent = {
                                        IconButton(onClick = { vm.deleteRemarkHistory(item) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                                        }
                                    },
                                    modifier = Modifier.clickable { remarkDraft = item.text.take(500) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.updateRemark(remarkDraft)
                        showRemarkDialog = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRemarkDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    }
}

@Composable
private fun AboutDrawerContent() {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            stringResource(R.string.about_title),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        AboutRow("软件名称", stringResource(R.string.about_software_name))
        AboutRow("版本号", BuildConfig.VERSION_NAME)
        AboutRow("适用平台", stringResource(R.string.about_platform))
        AboutRow("系统要求", stringResource(R.string.about_system_req))
        AboutRow("版权所有", stringResource(R.string.about_copyright))
        AboutRow("发布日期", stringResource(R.string.about_release_date))
        AboutRow("开发者", stringResource(R.string.about_developer))
        AboutRow("邮箱", stringResource(R.string.about_email))
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.about_oss_tip),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
