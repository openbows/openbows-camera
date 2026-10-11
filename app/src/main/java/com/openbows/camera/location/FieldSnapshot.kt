/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.location

data class FieldSnapshot(
    val timeMillis: Long = System.currentTimeMillis(),
    val address: String? = null,
    val gcjLat: Double? = null,
    val gcjLng: Double? = null,
    val wgsLat: Double? = null,
    val wgsLng: Double? = null,
    val altitudeMeters: Double? = null,
    val weather: String? = null,
    val adcode: String? = null,
    val locating: Boolean = true,
    val weatherPending: Boolean = true,
) {
    val hasFix: Boolean get() = (gcjLat != null && gcjLng != null) || (wgsLat != null && wgsLng != null)

    fun displayPoint(system: com.openbows.camera.data.CoordSystem): LatLng? {
        val wgsLat = wgsLat
        val wgsLng = wgsLng
        val gcjLat = gcjLat
        val gcjLng = gcjLng
        if (wgsLat == null || wgsLng == null || gcjLat == null || gcjLng == null) return null
        return CoordConvert.display(LatLng(wgsLat, wgsLng), LatLng(gcjLat, gcjLng), system)
    }
}
