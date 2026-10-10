package com.openbows.camera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watermark_templates")
data class WatermarkTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val showTime: Boolean = true,
    val showAddress: Boolean = true,
    val showLatLng: Boolean = true,
    val showCoordSystem: Boolean = true,
    val showAltitude: Boolean = true,
    val showWeather: Boolean = true,
    val showRemark: Boolean = true,
    val remark: String = "",
    val fontChoice: FontChoice = FontChoice.SANS,
    val fontSize: FontSizeChoice = FontSizeChoice.MEDIUM,
    val fontColorArgb: Int = 0xFF2196F3.toInt(),
    val bgColorArgb: Int = 0x00000000,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun fieldSummary(): String {
        val parts = buildList {
            if (showTime) add("时间")
            if (showAddress) add("地址")
            if (showLatLng) add("经纬度")
            if (showCoordSystem) add("坐标系")
            if (showAltitude) add("海拔")
            if (showWeather) add("天气")
            if (showRemark) add("备注")
        }
        return parts.joinToString("·")
    }
}

@Entity(tableName = "photos")
data class PhotoRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val takenAt: Long,
    val width: Int,
    val height: Int,
)

@Entity(tableName = "remark_history")
data class RemarkHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
