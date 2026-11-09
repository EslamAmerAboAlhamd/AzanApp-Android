package com.eslam.azanapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomAdhan(val name: String, val uri: String)

class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)

    var autoLocation: Boolean
        get() = sp.getBoolean("auto", true)
        set(v) { sp.edit().putBoolean("auto", v).apply() }

    var lat: Double
        get() = sp.getFloat("lat", 30.0444f).toDouble()
        set(v) { sp.edit().putFloat("lat", v.toFloat()).apply() }

    var lon: Double
        get() = sp.getFloat("lon", 31.2357f).toDouble()
        set(v) { sp.edit().putFloat("lon", v.toFloat()).apply() }

    var cityIndex: Int
        get() = sp.getInt("city", 1)
        set(v) { sp.edit().putInt("city", v).apply() }

    var methodIndex: Int
        get() = sp.getInt("method", 1)
        set(v) { sp.edit().putInt("method", v).apply() }

    var madhabIndex: Int
        get() = sp.getInt("madhab", 0)
        set(v) { sp.edit().putInt("madhab", v).apply() }

    var adjustments: IntArray
        get() {
            val l = (sp.getString("adj", "0,0,0,0,0,0") ?: "").split(",")
                .map { it.toIntOrNull() ?: 0 }
            return if (l.size == 6) l.toIntArray() else IntArray(6)
        }
        set(v) { sp.edit().putString("adj", v.joinToString(",")).apply() }

    // "b:0" = مؤذن مدمج رقم 0 ، "c:<uri>" = أذان أضافه المستخدم
    var selected: String
        get() = sp.getString("selected", "b:0") ?: "b:0"
        set(v) { sp.edit().putString("selected", v).apply() }

    var customs: List<CustomAdhan>
        get() {
            val out = mutableListOf<CustomAdhan>()
            try {
                val arr = JSONArray(sp.getString("customs", "[]"))
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(CustomAdhan(o.getString("name"), o.getString("uri")))
                }
            } catch (e: Exception) { }
            return out
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("name", it.name).put("uri", it.uri)) }
            sp.edit().putString("customs", arr.toString()).apply()
        }
    var lang: String
        get() = sp.getString("lang", null)
            ?: if (java.util.Locale.getDefault().language == "ar") "ar" else "en"
        set(v) { sp.edit().putString("lang", v).apply() }

    var permissionsAsked: Boolean
        get() = sp.getBoolean("perm_asked", false)
        set(v) { sp.edit().putBoolean("perm_asked", v).apply() }

    fun coords(): Pair<Double, Double> =
        if (autoLocation) lat to lon
        else Cities.list[cityIndex.coerceIn(0, Cities.list.size - 1)].let { it.lat to it.lon }
}