package com.eslam.azanapp

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Calendar
import java.util.Date

data class City(val name: String, val lat: Double, val lon: Double, val defaultMethod: Int = 1)

object Cities {
    val list = listOf(
        City("مكة المكرمة", 21.4225, 39.8262, 0),
        City("القاهرة", 30.0444, 31.2357, 1),
        City("القدس", 31.7683, 35.2137, 1),
        City("دبي", 25.2048, 55.2708, 3),
        City("عمّان", 31.9539, 35.9106, 2),
        City("الإسكندرية", 31.2001, 29.9187, 1),
        City("المنيا", 28.0871, 30.7618, 1),
        City("أسيوط", 27.1783, 31.1859, 1),
        City("الأقصر", 25.6872, 32.6396, 1),
        City("أسوان", 24.0889, 32.8998, 1),
        City("المنصورة", 31.0364, 31.3807, 1),
        City("طنطا", 30.7865, 31.0004, 1)
    )
}

data class PrayerState(
    val lastName: String, val lastTime: Long,
    val nextName: String, val nextTime: Long
)

object PrayerEngine {
    val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    val methods = listOf(
        "أم القرى (مكة المكرمة - السعودية)",
        "الهيئة المصرية العامة للمساحة (مصر والدول المجاورة)",
        "رابطة العالم الإسلامي (عام)",
        "دائرة الشؤون الإسلامية بدبي (الإمارات)",
        "وزارة الأوقاف والشؤون الإسلامية بقطر",
        "وزارة الأوقاف والشؤون الإسلامية بالكويت",
        "جامعة العلوم الإسلامية بكراتشي (باكستان والهند)",
        "الجمعية الإسلامية لشمال أمريكا (ISNA)",
        "لجنة رؤية الهلال (Moon Sighting Committee)",
        "المجلس الإسلامي الإندونيسي / سينغافورة"
    )

    val madhabs = listOf(
        "جمهور الفقهاء (الشافعي، المالكي، الحنبلي)",
        "المذهب الحنفي"
    )

    private fun params(methodIndex: Int, madhabIndex: Int): CalculationParameters {
        val p = when (methodIndex) {
            0 -> CalculationMethod.UMM_AL_QURA.parameters
            1 -> CalculationMethod.EGYPTIAN.parameters
            2 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            3 -> CalculationMethod.DUBAI.parameters
            4 -> CalculationMethod.QATAR.parameters
            5 -> CalculationMethod.KUWAIT.parameters
            6 -> CalculationMethod.KARACHI.parameters
            7 -> CalculationMethod.NORTH_AMERICA.parameters
            8 -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            9 -> CalculationMethod.SINGAPORE.parameters
            else -> CalculationMethod.EGYPTIAN.parameters
        }
        p.madhab = if (madhabIndex == 1) Madhab.HANAFI else Madhab.SHAFI
        return p
    }

    fun timesFor(prefs: Prefs, date: Date): LongArray {
        val (lat, lon) = prefs.coords()
        val cal = Calendar.getInstance()
        cal.time = date
        val components = DateComponents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val p = params(prefs.methodIndex, prefs.madhabIndex)
        val pt = PrayerTimes(Coordinates(lat, lon), components, p)
        val base = longArrayOf(
            pt.fajr.time, pt.sunrise.time, pt.dhuhr.time,
            pt.asr.time, pt.maghrib.time, pt.isha.time
        )
        val adj = prefs.adjustments
        return LongArray(6) { base[it] + adj[it] * 60_000L }
    }

    fun state(prefs: Prefs, now: Long): PrayerState {
        val all = mutableListOf<Pair<String, Long>>()
        val cal = Calendar.getInstance()
        for (d in -1..1) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, d)
            val t = timesFor(prefs, cal.time)
            for (i in 0..5) if (i != 1) all.add(names[i] to t[i])
        }
        all.sortBy { it.second }
        val last = all.lastOrNull { it.second <= now } ?: all.first()
        val next = all.firstOrNull { it.second > now } ?: all.last()
        return PrayerState(last.first, last.second, next.first, next.second)
    }
}
