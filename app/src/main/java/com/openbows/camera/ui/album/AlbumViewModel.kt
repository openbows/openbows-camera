package com.openbows.camera.ui.album

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openbows.camera.OpenbowsApplication
import com.openbows.camera.data.PhotoRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OpenbowsApplication
    private val repo = app.container.photoRepository
    val photos: StateFlow<List<PhotoRecord>> = repo.photos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun get(id: Long): PhotoRecord? = repo.get(id)

    fun delete(photo: PhotoRecord) {
        viewModelScope.launch { repo.delete(photo) }
    }

    fun deleteAll(photos: List<PhotoRecord>) {
        viewModelScope.launch {
            photos.forEach { repo.delete(it) }
        }
    }

    fun shareIntent(photos: List<PhotoRecord>): Intent {
        val uris = ArrayList(photos.map { Uri.parse(it.uri) })
        return if (uris.size <= 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris.firstOrNull())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun shareIntent(photo: PhotoRecord): Intent = shareIntent(listOf(photo))
}
