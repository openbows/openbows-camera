/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.openbows.camera.data.CoordSystem
import com.openbows.camera.data.WatermarkTemplate
import com.openbows.camera.data.label
import com.openbows.camera.data.scale
import com.openbows.camera.data.toTypeface
import com.openbows.camera.location.FieldSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkLines {
    const val REMARK_PREFIX = "备注："

    fun build(
        template: WatermarkTemplate,
        field: FieldSnapshot,
        coordSystem: CoordSystem = CoordSystem.WGS84,
    ): List<String> {
        val lines = mutableListOf<String>()
        if (template.showLatLng) {
            val point = field.displayPoint(coordSystem)
            lines += fieldLine("经度", point?.let { "%.6f".format(Locale.US, it.lng) }.orEmpty())
            lines += fieldLine("纬度", point?.let { "%.6f".format(Locale.US, it.lat) }.orEmpty())
        }
        if (template.showCoordSystem) {
            lines += fieldLine("坐标系", coordSystem.label())
        }
        if (template.showAddress) {
            lines += fieldLine("地址", field.address?.trim().orEmpty())
        }
        if (template.showTime) {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            lines += fieldLine("时间", fmt.format(Date(field.timeMillis)))
        }
        if (template.showAltitude) {
            val altitude = field.altitudeMeters?.let { "%.1f".format(Locale.US, it) + "m" }.orEmpty()
            lines += fieldLine("海拔", altitude)
        }
        if (template.showWeather) {
            lines += fieldLine("天气", field.weather?.trim().orEmpty())
        }
        if (template.showRemark) {
            lines += REMARK_PREFIX + template.remark.trim()
        }
        return lines
    }

    /** 字段名后空格、不用冒号；空值仍保留字段名。 */
    private fun fieldLine(name: String, value: String): String = "$name $value"
}

/** 续行用 indentPx 与本字段名后对齐，不受其它行字段名长度影响。 */
data class WatermarkDrawLine(
    val text: String,
    val indentPx: Float = 0f,
)

data class WatermarkLayout(
    val bounds: RectF,
    val lines: List<WatermarkDrawLine>,
)

class WatermarkRenderer(@Suppress("unused") context: Context) {
    fun layout(
        width: Float,
        height: Float,
        template: WatermarkTemplate,
        field: FieldSnapshot,
        coordSystem: CoordSystem,
    ): WatermarkLayout {
        val scale = (width / 1080f).coerceIn(0.55f, 2.4f)
        val textSize = 28f * scale * template.fontSize.scale()
        val pad = 18f * scale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = template.fontChoice.toTypeface()
        }
        val maxTextWidth = (width - 48f * scale - pad * 2).coerceAtLeast(120f)
        val lines = wrapLines(WatermarkLines.build(template, field, coordSystem), paint, maxTextWidth)
        val fm = paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * 1.12f
        val display = lines.ifEmpty { listOf(WatermarkDrawLine("长按添加备注")) }
        var maxWidth = 0f
        display.forEach { line ->
            maxWidth = maxOf(maxWidth, line.indentPx + paint.measureText(line.text))
        }
        val boxW = maxWidth + pad * 2
        val boxH = lineHeight * display.size + pad * 2
        val left = 24f * scale
        val top = height - boxH - 24f * scale
        return WatermarkLayout(RectF(left, top, left + boxW, top + boxH), lines)
    }

    fun burn(
        source: Bitmap,
        template: WatermarkTemplate,
        field: FieldSnapshot,
        coordSystem: CoordSystem,
    ): Bitmap {
        val mutable = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        draw(canvas, mutable.width.toFloat(), mutable.height.toFloat(), template, field, coordSystem)
        return mutable
    }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        template: WatermarkTemplate,
        field: FieldSnapshot,
        coordSystem: CoordSystem,
    ): RectF {
        val layout = layout(width, height, template, field, coordSystem)
        val lines = layout.lines
        if (lines.isEmpty()) return layout.bounds
        val scale = (width / 1080f).coerceIn(0.55f, 2.4f)
        val textSize = 28f * scale * template.fontSize.scale()
        val pad = 18f * scale
        val radius = 12f * scale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.fontColorArgb
            this.textSize = textSize
            typeface = template.fontChoice.toTypeface()
            setShadowLayer(2f * scale, 0f, 1f * scale, 0x88000000.toInt())
        }
        val fm = paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * 1.12f
        val bounds = layout.bounds
        if (Color.alpha(template.bgColorArgb) > 0) {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = template.bgColorArgb }
            canvas.drawRoundRect(bounds, radius, radius, bg)
        }
        var y = bounds.top + pad - fm.ascent
        lines.forEach { line ->
            canvas.drawText(line.text, bounds.left + pad + line.indentPx, y, paint)
            y += lineHeight
        }
        return bounds
    }

    private fun wrapLines(raw: List<String>, paint: Paint, maxWidth: Float): List<WatermarkDrawLine> {
        val out = mutableListOf<WatermarkDrawLine>()
        raw.forEach { line ->
            val prefix = fieldPrefixOf(line) ?: ""
            val body = if (prefix.isEmpty()) line else line.removePrefix(prefix)
            val maxLines = when (prefix) {
                WatermarkLines.REMARK_PREFIX, "地址 " -> Int.MAX_VALUE
                else -> 2
            }
            out += wrapPrefixed(prefix, body, paint, maxWidth, maxLines)
        }
        return out
    }

    private fun fieldPrefixOf(line: String): String? {
        val prefixes = listOf(
            WatermarkLines.REMARK_PREFIX,
            "时间 ",
            "地址 ",
            "经度 ",
            "纬度 ",
            "坐标系 ",
            "海拔 ",
            "天气 ",
        )
        return prefixes.firstOrNull { line.startsWith(it) }
    }

    /**
     * 首行：字段名 + 正文；续行：仅正文，x 起点与本字段名后对齐
     *（备注对齐冒号后，地址对齐空格后），不参考其它行字段名宽度。
     */
    private fun wrapPrefixed(
        prefix: String,
        body: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int,
    ): List<WatermarkDrawLine> {
        if (body.isEmpty()) return listOf(WatermarkDrawLine(prefix))

        val prefixWidth = paint.measureText(prefix)
        val bodyWidth = (maxWidth - prefixWidth).coerceAtLeast(40f)
        val lines = mutableListOf<WatermarkDrawLine>()
        var remain = body
        var first = true
        while (remain.isNotEmpty() && lines.size < maxLines) {
            val last = lines.size == maxLines - 1
            var end = remain.length
            while (end > 0 && paint.measureText(remain.substring(0, end)) > bodyWidth) {
                end--
            }
            if (end <= 0) end = 1
            val truncated = last && end < remain.length
            val chunk = if (truncated) {
                var cut = end
                while (cut > 1 && paint.measureText(remain.substring(0, cut) + "…") > bodyWidth) cut--
                remain.substring(0, cut.coerceAtLeast(1)) + "…"
            } else {
                remain.substring(0, end)
            }
            if (first) {
                lines += WatermarkDrawLine(prefix + chunk, indentPx = 0f)
            } else {
                lines += WatermarkDrawLine(chunk, indentPx = prefixWidth)
            }
            remain = if (truncated) "" else remain.substring(end)
            first = false
        }
        return lines
    }
}
