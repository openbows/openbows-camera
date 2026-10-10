package com.openbows.camera.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openbows.camera.data.FontChoice
import com.openbows.camera.data.FontSizeChoice
import com.openbows.camera.location.FieldSnapshot
import com.openbows.camera.watermark.WatermarkLines

private val palette = listOf(
    Color.White,
    Color(0xFFF5C15C),
    Color(0xFFFFEB3B),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFFF44336),
    Color(0xFF000000),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: Long,
    onBack: () -> Unit,
    vm: TemplateEditorViewModel = viewModel(),
) {
    val template by vm.template.collectAsStateWithLifecycle()
    val coordSystem by vm.coordSystem.collectAsStateWithLifecycle()
    LaunchedEffect(templateId) { vm.load(templateId) }
    val item = template

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "编辑模板") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (item == null) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text("水印预览", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            val previewLines = WatermarkLines.build(
                item,
                FieldSnapshot(
                    address = "示例路88号",
                    gcjLat = 39.908823,
                    gcjLng = 116.397470,
                    wgsLat = 39.907431,
                    wgsLng = 116.391278,
                    altitudeMeters = 45.2,
                    weather = "晴 26℃",
                    locating = false,
                    weatherPending = false,
                ),
                coordSystem,
            )
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(
                        if (android.graphics.Color.alpha(item.bgColorArgb) == 0) {
                            Color(0x22000000)
                        } else {
                            Color(item.bgColorArgb)
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
            ) {
                if (previewLines.isEmpty()) {
                    Text("未选择任何字段", color = Color.White)
                } else {
                    previewLines.forEach { Text(it, color = Color(item.fontColorArgb)) }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
            Text("显示字段")
            ToggleRow("时间", item.showTime) { vm.update { t -> t.copy(showTime = it) } }
            ToggleRow("地址", item.showAddress) { vm.update { t -> t.copy(showAddress = it) } }
            ToggleRow("经纬度", item.showLatLng) { vm.update { t -> t.copy(showLatLng = it) } }
            ToggleRow("坐标系", item.showCoordSystem) { vm.update { t -> t.copy(showCoordSystem = it) } }
            ToggleRow("海拔", item.showAltitude) { vm.update { t -> t.copy(showAltitude = it) } }
            ToggleRow("天气", item.showWeather) { vm.update { t -> t.copy(showWeather = it) } }
            ToggleRow("备注", item.showRemark) { vm.update { t -> t.copy(showRemark = it) } }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = item.remark,
                onValueChange = { v -> vm.update { t -> t.copy(remark = v.take(500), showRemark = true) } },
                label = { Text("备注（最多500字）") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                minLines = 3,
                maxLines = 3,
            )

            Spacer(Modifier.height(16.dp))
            Text("字体")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontChoice.entries.forEach { font ->
                    val label = when (font) {
                        FontChoice.SANS -> "黑体"
                        FontChoice.SERIF -> "衬线"
                        FontChoice.MONO -> "等宽"
                    }
                    FilterChip(
                        selected = item.fontChoice == font,
                        onClick = { vm.setFont(font) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("字号")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontSizeChoice.entries.forEach { size ->
                    val label = when (size) {
                        FontSizeChoice.SMALL -> "小"
                        FontSizeChoice.MEDIUM -> "中"
                        FontSizeChoice.LARGE -> "大"
                    }
                    FilterChip(
                        selected = item.fontSize == size,
                        onClick = { vm.setSize(size) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("字体颜色")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { color ->
                    val selected = item.fontColorArgb == color.toArgb()
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color(0xFFF5C15C) else Color.Gray,
                                shape = CircleShape,
                            )
                            .clickable { vm.setColor(color.toArgb()) },
                    )
                }
            }
            OutlinedTextField(
                value = String.format("#%08X", item.fontColorArgb),
                onValueChange = { raw ->
                    val hex = raw.removePrefix("#")
                    if (hex.length == 8) {
                        runCatching { vm.setColor(hex.toLong(16).toInt()) }
                    } else if (hex.length == 6) {
                        runCatching { vm.setColor(("FF$hex").toLong(16).toInt()) }
                    }
                },
                label = { Text("自定义色 ARGB") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))
            Text("水印背景色")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val bgPalette = listOf(
                    0x00000000 to "透明",
                    0x99000000.toInt() to "半透明黑",
                    0x66FFFFFF to "半透明白",
                    0xCC000000.toInt() to "深黑",
                )
                bgPalette.forEach { (argb, label) ->
                    val selected = item.bgColorArgb == argb
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (argb == 0) Color.Transparent else Color(argb),
                                )
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color(0xFFF5C15C) else Color.Gray,
                                    shape = CircleShape,
                                )
                                .clickable { vm.setBgColor(argb) },
                        )
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            OutlinedTextField(
                value = String.format("#%08X", item.bgColorArgb),
                onValueChange = { raw ->
                    val hex = raw.removePrefix("#")
                    if (hex.length == 8) {
                        runCatching { vm.setBgColor(hex.toLong(16).toInt()) }
                    } else if (hex.length == 6) {
                        runCatching { vm.setBgColor(("FF$hex").toLong(16).toInt()) }
                    }
                },
                label = { Text("自定义背景 ARGB（默认透明）") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
