/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.ui.album

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.openbows.camera.R
import com.openbows.camera.data.PhotoRecord

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AlbumScreen(
    onBack: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    vm: AlbumViewModel = viewModel(),
) {
    val photos by vm.photos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectedPhotos = remember(photos, selected) { photos.filter { it.id in selected } }

    fun exitSelection() {
        selecting = false
        selected = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selecting) {
                            "已选 ${selected.size} 张"
                        } else {
                            "${stringResource(R.string.album)}（${photos.size}）"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selecting) exitSelection() else onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!selecting && photos.isNotEmpty()) {
                        TextButton(onClick = { selecting = true }) {
                            Text("选择")
                        }
                    } else if (selecting) {
                        TextButton(
                            onClick = {
                                selected = if (selected.size == photos.size) {
                                    emptySet()
                                } else {
                                    photos.map { it.id }.toSet()
                                }
                            },
                        ) {
                            Text(if (selected.size == photos.size) "取消全选" else "全选")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (selecting && selected.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(
                        onClick = { confirmDelete = true },
                    ) { Text(stringResource(R.string.delete)) }
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    vm.shareIntent(selectedPhotos),
                                    context.getString(R.string.share),
                                ),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.share))
                    }
                }
            }
        },
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.empty_album), textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    AlbumThumb(
                        photo = photo,
                        selected = photo.id in selected,
                        selecting = selecting,
                        onOpen = { onOpenPhoto(photo.id) },
                        onToggle = {
                            selected = if (photo.id in selected) {
                                selected - photo.id
                            } else {
                                selected + photo.id
                            }
                        },
                        onEnterSelect = {
                            selecting = true
                            selected = setOf(photo.id)
                        },
                    )
                }
            }
        }
    }

    if (confirmDelete && selectedPhotos.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("删除选中的 ${selectedPhotos.size} 张照片？文件将从本机移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAll(selectedPhotos)
                        confirmDelete = false
                        exitSelection()
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AlbumThumb(
    photo: PhotoRecord,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onEnterSelect: () -> Unit,
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = {
                    if (selecting) onToggle() else onOpen()
                },
                onLongClick = {
                    if (selecting) onToggle() else onEnterSelect()
                },
            )
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            ),
    ) {
        AsyncImage(
            model = Uri.parse(photo.uri),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
