/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

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
