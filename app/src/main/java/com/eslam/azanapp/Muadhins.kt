package com.eslam.azanapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

object Muadhins {
    val names = listOf(
        "المدينة المنورة قديم - عصام بخاري",
        "المدينة المنورة جديد - محمد القصاص",
        "ياسر الدوسري",
        "ناصر القطامي",
        "مشاري العفاسي - عجم",
        "الشيخ محمد رفعت رحمه الله",
        "أذان تركي",
        "عراقي - عبدالسميع الشيخ",
        "عبدالرحمن الهندي",
        "الشيخ عبدالباسط عبدالصمد رحمه الله"
    )

    fun rawResId(index: Int): Int = when (index) {
        0 -> R.raw.azan_01
        1 -> R.raw.azan_02
        2 -> R.raw.azan_03
        3 -> R.raw.azan_04
        4 -> R.raw.azan_05
        5 -> R.raw.azan_06
        6 -> R.raw.azan_07
        7 -> R.raw.azan_08
        8 -> R.raw.azan_09
        9 -> R.raw.azan_10
        else -> 0
    }

    fun rawUri(ctx: Context, index: Int): Uri? {
        val id = rawResId(index)
        return if (id != 0) Uri.parse("android.resource://${ctx.packageName}/$id") else null
    }

    fun firstBuiltin(ctx: Context): Uri? {
        for (i in names.indices) rawUri(ctx, i)?.let { return it }
        return null
    }

    fun resolve(ctx: Context, prefs: Prefs): Uri? {
        val sel = prefs.selected
        if (sel.startsWith("c:")) return Uri.parse(sel.removePrefix("c:"))
        val i = sel.removePrefix("b:").toIntOrNull() ?: 0
        return rawUri(ctx, i) ?: firstBuiltin(ctx)
    }
}

object Preview {
    private var mp: MediaPlayer? = null

    fun play(ctx: Context, uri: Uri): Boolean {
        stop()
        return try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            p.setDataSource(ctx, uri)
            p.prepare()
            p.setOnCompletionListener { stop() }
            p.start()
            mp = p
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        try { mp?.stop() } catch (e: Exception) { }
        try { mp?.release() } catch (e: Exception) { }
        mp = null
    }
}

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun last(ctx: Context): Pair<Double, Double>? {
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var best: Location? = null
            for (p in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.time > best.time) best = l
            }
            best?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }
}