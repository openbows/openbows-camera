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
