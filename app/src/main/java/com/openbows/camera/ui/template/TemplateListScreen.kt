package com.openbows.camera.ui.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openbows.camera.R
import com.openbows.camera.data.CoordSystem
import com.openbows.camera.data.WatermarkTemplate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateListScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: TemplateListViewModel = viewModel(),
) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val coordSystem by vm.coordSystem.collectAsStateWithLifecycle()
    var renameTarget by remember { mutableStateOf<WatermarkTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<WatermarkTemplate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.create { onEdit(it) } },
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_template))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("定位：天地图（系统 GPS + 逆地理）")
                    Text("坐标系", modifier = Modifier.padding(top = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoordSystem.entries.forEach { system ->
                            val label = when (system) {
                                CoordSystem.WGS84 -> "WGS84（推荐）"
                                CoordSystem.GCJ02 -> "GCJ02"
                                CoordSystem.CGCS2000 -> "CGCS2000"
                            }
                            FilterChip(
                                selected = coordSystem == system,
                                onClick = { vm.setCoordSystem(system) },
                                label = { Text(label) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.storage_hint),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            items(templates, key = { it.id }) { item ->
                var menu by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text(item.fieldSummary().ifBlank { "未选择字段" }) },
                    leadingContent = {
                        RadioButton(
                            selected = current?.id == item.id,
                            onClick = { vm.select(item.id) },
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    menu = false
                                    onEdit(item.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    menu = false
                                    renameTarget = item
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.copy)) },
                                onClick = {
                                    menu = false
                                    vm.duplicate(item)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    menu = false
                                    deleteTarget = item
                                },
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(item.id) },
                )
            }
        }
    }

    val renaming = renameTarget
    if (renaming != null) {
        var name by remember(renaming.id) { mutableStateOf(renaming.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.rename(renaming, name)
                        renameTarget = null
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val deleting = deleteTarget
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除模板") },
            text = { Text("确定删除「${deleting.name}」？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(deleting)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
