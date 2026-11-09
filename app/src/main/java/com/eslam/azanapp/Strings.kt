package com.eslam.azanapp

object Strings {
    private val prayersEn = mapOf(
        "الفجر" to "Fajr", "الشروق" to "Sunrise", "الظهر" to "Dhuhr",
        "العصر" to "Asr", "المغرب" to "Maghrib", "العشاء" to "Isha"
    )

    fun prayer(name: String, lang: String): String =
        if (lang == "ar") name else (prayersEn[name] ?: name)

    val hijriMonthsAr = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    val hijriMonthsEn = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Awwal",
        "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal",
        "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )
    fun hijriMonths(lang: String) = if (lang == "ar") hijriMonthsAr else hijriMonthsEn

    // الأسبوع بيبدأ بالسبت
    val daysAr = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    val daysEn = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

    val muadhinsEn = listOf(
        "Madinah (old) - Issam Bukhari",
        "Madinah (new) - Mohammed Al-Qassas",
        "Yasser Al-Dosari",
        "Nasser Al-Qatami",
        "Mishary Alafasy - Ajam",
        "Sheikh Mohamed Refaat",
        "Turkish Adhan",
        "Iraqi - Abdulsamee Al-Sheikh",
        "Abdulrahman Al-Hindi",
        "Sheikh Abdulbasit Abdulsamad"
    )

    // نفس ترتيب Cities.list
    val citiesEn = listOf(
        "Makkah", "Cairo", "Jerusalem", "Dubai", "Amman", "Alexandria",
        "Minya", "Asyut", "Luxor", "Aswan", "Mansoura", "Tanta"
    )

    val methodsEn = listOf(
        "Umm Al-Qura (Makkah - Saudi Arabia)",
        "Egyptian General Authority of Survey (Egypt)",
        "Muslim World League (Global)",
        "Dubai Islamic Affairs (UAE)",
        "Ministry of Awqaf & Islamic Affairs (Qatar)",
        "Ministry of Awqaf & Islamic Affairs (Kuwait)",
        "University of Islamic Sciences, Karachi (Pakistan)",
        "ISNA (North America)",
        "Moon Sighting Committee",
        "Majlis Ugama Islam (Singapore / Indonesia)"
    )

    val madhabsEn = listOf(
        "Standard / Shafi'i, Maliki, Hanbali",
        "Hanafi"
    )
}