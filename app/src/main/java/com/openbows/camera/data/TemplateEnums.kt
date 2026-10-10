package com.openbows.camera.data

enum class CoordSystem {
    WGS84,
    GCJ02,
    CGCS2000,
}

enum class FontChoice {
    SANS,
    SERIF,
    MONO,
}

enum class FontSizeChoice {
    SMALL,
    MEDIUM,
    LARGE,
}

fun FontChoice.toTypeface(): android.graphics.Typeface = when (this) {
    FontChoice.SANS -> android.graphics.Typeface.SANS_SERIF
    FontChoice.SERIF -> android.graphics.Typeface.SERIF
    FontChoice.MONO -> android.graphics.Typeface.MONOSPACE
}

fun FontSizeChoice.scale(): Float = when (this) {
    FontSizeChoice.SMALL -> 1.0f
    FontSizeChoice.MEDIUM -> 1.5f
    FontSizeChoice.LARGE -> 2.0f
}

fun CoordSystem.label(): String = when (this) {
    CoordSystem.WGS84 -> "WGS84"
    CoordSystem.GCJ02 -> "GCJ02"
    CoordSystem.CGCS2000 -> "CGCS2000"
}
