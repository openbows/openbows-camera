/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 定位：系统 GPS（WGS84，与天地图一致）。
 * 地址：天地图逆地理；天气：Open-Meteo（天地图无天气接口）。
 */
class LocationTracker(
    private val context: Context,
    private val tianditu: TiandituRestClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snapshot = MutableStateFlow(FieldSnapshot())
    val snapshot: StateFlow<FieldSnapshot> = _snapshot.asStateFlow()

    private var systemManager: LocationManager? = null
    private val systemListener = LocationListener { location -> onWgsLocation(location) }
    private var enrichJob: Job? = null
    private var lastGeoLat = 0.0
    private var lastGeoLng = 0.0
    private var lastWeatherAt = 0L
    private var systemStarted = false

    fun start() {
        if (systemStarted) return
        _snapshot.value = FieldSnapshot(locating = true, weatherPending = true)
        startSystem()
    }

    fun stop() {
        stopEngine()
        enrichJob?.cancel()
    }

    private fun stopEngine() {
        systemStarted = false
        systemManager?.let { mgr ->
            runCatching { mgr.removeUpdates(systemListener) }
        }
        systemManager = null
    }

    @SuppressLint("MissingPermission")
    private fun startSystem() {
        if (systemStarted) return
        if (!hasLocationPermission()) return
        systemStarted = true
        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        systemManager = mgr
        val provider = when {
            mgr.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        val last = provider?.let { mgr.getLastKnownLocation(it) }
            ?: mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        last?.let { onWgsLocation(it) }
        runCatching {
            if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mgr.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    5f,
                    systemListener,
                    Looper.getMainLooper(),
                )
            }
            if (mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mgr.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    systemListener,
                    Looper.getMainLooper(),
                )
            }
        }
    }

    private fun onWgsLocation(location: Location) {
        val wgsLat = location.latitude
        val wgsLng = location.longitude
        val gcj = CoordConvert.wgs84ToGcj02(wgsLat, wgsLng)
        applyFix(
            gcjLat = gcj.lat,
            gcjLng = gcj.lng,
            wgsLat = wgsLat,
            wgsLng = wgsLng,
            altitude = if (location.hasAltitude()) location.altitude else null,
        )
    }

    private fun applyFix(
        gcjLat: Double,
        gcjLng: Double,
        wgsLat: Double,
        wgsLng: Double,
        altitude: Double?,
    ) {
        val prev = _snapshot.value
        _snapshot.value = prev.copy(
            timeMillis = System.currentTimeMillis(),
            gcjLat = gcjLat,
            gcjLng = gcjLng,
            wgsLat = wgsLat,
            wgsLng = wgsLng,
            altitudeMeters = altitude ?: prev.altitudeMeters,
            locating = false,
        )
        maybeEnrich(wgsLat, wgsLng)
    }

    private fun maybeEnrich(wgsLat: Double, wgsLng: Double) {
        val moved = abs(wgsLat - lastGeoLat) + abs(wgsLng - lastGeoLng) > 0.0008
        val needAddress = moved || _snapshot.value.address == null
        val needWeather = _snapshot.value.weather == null ||
            System.currentTimeMillis() - lastWeatherAt > 10 * 60 * 1000
        if (!needAddress && !needWeather) return
        lastGeoLat = wgsLat
        lastGeoLng = wgsLng
        enrichJob?.cancel()
        enrichJob = scope.launch(Dispatchers.IO) {
            var address = _snapshot.value.address
            if (needAddress) {
                val geo = tianditu.reverseGeocode(wgsLng, wgsLat)
                address = geo?.address ?: address
            }
            var weather = _snapshot.value.weather
            if (needWeather) {
                weather = tianditu.weather(wgsLat, wgsLng) ?: weather
                lastWeatherAt = System.currentTimeMillis()
            }
            _snapshot.value = _snapshot.value.copy(
                address = address,
                weather = weather,
                weatherPending = weather == null,
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
