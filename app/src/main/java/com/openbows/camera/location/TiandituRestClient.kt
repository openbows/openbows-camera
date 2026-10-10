package com.openbows.camera.location

import com.openbows.camera.BuildConfig
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 天地图 Web 服务：逆地理（WGS84/CGCS2000）。
 * 支持多 tk 轮询以提高配额；天气走 Open-Meteo。
 */
class TiandituRestClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
) {
    data class ReverseGeo(val address: String?, val county: String?)

    private val keys: List<String> = BuildConfig.TIANDITU_TKS.filter { it.isNotBlank() }
    private val keyCursor = AtomicInteger(0)

    suspend fun reverseGeocode(wgsLng: Double, wgsLat: Double): ReverseGeo? {
        if (keys.isEmpty()) return null
        val postStr = """{"lon":$wgsLng,"lat":$wgsLat,"ver":1}"""
        val encoded = URLEncoder.encode(postStr, Charsets.UTF_8.name())
        val start = Math.floorMod(keyCursor.getAndIncrement(), keys.size)
        for (offset in keys.indices) {
            val tk = keys[(start + offset) % keys.size]
            val url = "https://api.tianditu.gov.cn/geocoder?postStr=$encoded&type=geocode&tk=$tk"
            val json = getJson(url) ?: continue
            if (json.optString("status") != "0") continue
            val result = json.optJSONObject("result") ?: continue
            val address = result.optString("formatted_address").takeIf { it.isNotBlank() }
            val county = result.optJSONObject("addressComponent")
                ?.optString("county")
                ?.takeIf { it.isNotBlank() }
            if (address == null && county == null) continue
            return ReverseGeo(address, county)
        }
        return null
    }

    /** Open-Meteo 实时天气（无需 Key，坐标为 WGS84）。 */
    suspend fun weather(wgsLat: Double, wgsLng: Double): String? {
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$wgsLat&longitude=$wgsLng" +
                "&current=temperature_2m,weather_code&timezone=Asia%2FShanghai"
        val json = getJson(url) ?: return null
        val current = json.optJSONObject("current") ?: return null
        val code = current.optInt("weather_code", -1)
        val temp = current.optDouble("temperature_2m", Double.NaN)
        val desc = weatherCodeLabel(code)
        return when {
            desc != null && !temp.isNaN() -> "$desc ${"%.0f".format(temp)}℃"
            desc != null -> desc
            else -> null
        }
    }

    private fun weatherCodeLabel(code: Int): String? = when (code) {
        0 -> "晴"
        1, 2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 65, 66, 67 -> "雨"
        71, 73, 75, 77 -> "雪"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷阵雨"
        else -> null
    }

    private suspend fun getJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                JSONObject(body)
            }
        }.getOrNull()
    }
}
