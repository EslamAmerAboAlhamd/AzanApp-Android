package com.eslam.azanapp

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale

val LocalLang = compositionLocalOf { "ar" }
val PURPLE = Color(0xFF9B6BFF)
val RED_GLASS = Color(0xFFFF6B6B)

fun appFont(ctx: Context): FontFamily {
    return try {
        FontFamily(
            Font(R.font.cairo_regular, FontWeight.Normal),
            Font(R.font.cairo_bold, FontWeight.Bold)
        )
    } catch (e: Exception) {
        FontFamily.Default
    }
}

class MainActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            afterPermissions()
        }

    private fun afterPermissions() {
        val prefs = Prefs(this)
        if (prefs.autoLocation) {
            LocationHelper.last(this)?.let { prefs.lat = it.first; prefs.lon = it.second }
        }
        AzanScheduler.scheduleNext(this)
        refreshTick++

        // المنبّهات الدقيقة (أندرويد 12+)
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName"))
                    )
                } catch (e: Exception) { }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        // طلب الأذونات عند أول دخول فقط
        if (!prefs.permissionsAsked) {
            prefs.permissionsAsked = true
            val perms = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permLauncher.launch(perms.toTypedArray())
        }

        AzanScheduler.scheduleNext(this)
        val font = appFont(this)

        setContent {
            var lang by remember { mutableStateOf(prefs.lang) }
            val dir = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            MaterialTheme(colorScheme = darkColorScheme()) {
                ProvideTextStyle(TextStyle(fontFamily = font)) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides dir,
                        LocalLang provides lang
                    ) {
                        Surface(Modifier.fillMaxSize()) {
                            App(prefs, refreshTick) { lang = it; prefs.lang = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun tr(ar: String, en: String): String = if (LocalLang.current == "ar") ar else en

// ============================ عناصر Glass ============================

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color.White,
    strength: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var m = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(tint.copy(alpha = 0.34f * strength), tint.copy(alpha = 0.10f * strength))
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))),
            shape
        )
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, contentAlignment = Alignment.Center, content = content)
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, tint: Color = PURPLE, onClick: () -> Unit) {
    GlassSurface(modifier.height(46.dp), RoundedCornerShape(14.dp), tint, onClick = onClick) {
        Text(
            text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

@Composable
fun GlassIcon(icon: ImageVector, onClick: () -> Unit) {
    GlassSurface(Modifier.size(48.dp), CircleShape, PURPLE, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

// ============================ التطبيق ============================

@Composable
fun App(prefs: Prefs, tick: Int, onLang: (String) -> Unit) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        if (prefs.autoLocation) {
            LocationHelper.last(ctx)?.let {
                prefs.lat = it.first
                prefs.lon = it.second
                AzanScheduler.scheduleNext(ctx)
                version++
            }
        }
    }

    BackHandler(enabled = screen != "home") { screen = "home" }

    when (screen) {
        "settings" -> SettingsScreen(prefs, onChange = {
            version++
            AzanScheduler.scheduleNext(ctx)
        }, onLang = onLang)
        "calendar" -> CalendarScreen()
        else -> HomeScreen(
            prefs, version,
            onSettings = { screen = "settings" },
            onCalendar = { screen = "calendar" }
        )
    }
}

// ============================ الشاشة الرئيسية ============================

private fun hijriToday(lang: String): String {
    val h = HijrahDate.from(LocalDate.now())
    val months = Strings.hijriMonths(lang)
    val suffix = if (lang == "ar") "هـ" else "AH"
    return "${h.get(ChronoField.DAY_OF_MONTH)} " +
            "${months[h.get(ChronoField.MONTH_OF_YEAR) - 1]} " +
            "${h.get(ChronoField.YEAR)} $suffix"
}

private fun gradientFor(name: String): List<Color> = when (name) {
    "الفجر" -> listOf(Color(0xFF1B2A49), Color(0xFF4A5C8A))
    "الظهر" -> listOf(Color(0xFF1E88E5), Color(0xFF90CAF9))
    "العصر" -> listOf(Color(0xFF6D4C41), Color(0xFFFFB74D))
    "المغرب" -> listOf(Color(0xFF4A148C), Color(0xFFFF7043))
    else -> listOf(Color(0xFF0B0F1A), Color(0xFF1A237E))
}

private fun bgResFor(name: String): Int = when (name) {
    "الفجر" -> R.drawable.bg_fajr
    "الشروق" -> R.drawable.bg_sunrise
    "الظهر" -> R.drawable.bg_dhuhr
    "العصر" -> R.drawable.bg_asr
    "المغرب" -> R.drawable.bg_maghrib
    "العشاء" -> R.drawable.bg_isha
    else -> R.drawable.bg_isha
}

@Composable
fun HomeScreen(prefs: Prefs, version: Int, onSettings: () -> Unit, onCalendar: () -> Unit) {
    val lang = LocalLang.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val st = remember(version, now / 1000) { PrayerEngine.state(prefs, now) }
    val remaining = ((st.nextTime - now) / 1000).coerceAtLeast(0)
    val countdown = "%02d:%02d:%02d".format(Locale.US, remaining / 3600, (remaining % 3600) / 60, remaining % 60)
    val timeFmt = SimpleDateFormat("hh:mm a", Locale(lang))

    val todayTimes = remember(version, now / 86400000) { PrayerEngine.timesFor(prefs, Date(now)) }

    val defaultIndex = remember(st.lastName) { PrayerEngine.names.indexOf(st.lastName).coerceAtLeast(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val displayIndex = if (selectedIndex < 0) defaultIndex else selectedIndex

    val displayName = PrayerEngine.names[displayIndex]
    val displayTime = todayTimes[displayIndex]
    val bgRes = remember(displayName) { bgResFor(displayName) }
    val shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 4f), 14f)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(gradientFor(displayName)))) {
        if (bgRes != 0) {
            Image(
                painter = painterResource(id = bgRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.fillMaxSize().background(Color(0x55000000)))

        // مناطق الضغط على الناحية الشمال والناحية اليمين للتنقل بين الصلوات
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
            ) {
                // الناحية الشمال -> الصلاة السابقة
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedIndex = (displayIndex - 1 + 6) % 6
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        Modifier
                            .padding(start = 12.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "الصلاة السابقة",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // طرف الشاشة الايمن -> الصلاة التالية
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedIndex = (displayIndex + 1) % 6
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        Modifier
                            .padding(end = 12.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "الصلاة التالية",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // الإعدادات يسار والتقويم يمين دايماً
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIcon(Icons.Default.Settings, onSettings)
                Text(
                    hijriToday(lang),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = shadow)
                )
                GlassIcon(Icons.Default.DateRange, onCalendar)
            }
        }

        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (displayIndex == defaultIndex) tr("آخر صلاة", "Last prayer") else tr("الصلاة", "Prayer"),
                color = Color(0xFFE8E8E8), fontSize = 20.sp,
                style = TextStyle(shadow = shadow)
            )
            Text(
                Strings.prayer(displayName, lang), color = Color.White, fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = shadow, letterSpacing = if (lang == "ar") 0.sp else 2.sp)
            )
            Text(
                timeFmt.format(Date(displayTime)), color = Color(0xFFF5F5F5), fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold, style = TextStyle(shadow = shadow)
            )

            if (displayIndex != defaultIndex) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { selectedIndex = -1 },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        tr("العودة للصلاة الحالية ↺", "Reset to current ↺"),
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // مربع العدّاد شفاف
            GlassSurface(shape = RoundedCornerShape(20.dp), strength = 0.45f) {
                Column(
                    Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tr("الوقت المتبقي لصلاة ", "Time until ") + Strings.prayer(st.nextName, lang),
                        color = Color(0xFFEDEDED), fontSize = 16.sp,
                        style = TextStyle(shadow = shadow)
                    )
                    Text(
                        countdown, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = shadow)
                    )
                }
            }
        }
    }
}

// ============================ التقويم الهجري ============================

@Composable
fun CalendarScreen() {
    val lang = LocalLang.current
    val months = Strings.hijriMonths(lang)
    val days = if (lang == "ar") Strings.daysAr else Strings.daysEn
    val today = LocalDate.now()
    val th = HijrahDate.from(today)
    var y by remember { mutableIntStateOf(th.get(ChronoField.YEAR)) }
    var m by remember { mutableIntStateOf(th.get(ChronoField.MONTH_OF_YEAR)) }

    val first = HijrahDate.of(y, m, 1)
    val len = first.lengthOfMonth()
    val firstG = LocalDate.from(first)
    val offset = (firstG.dayOfWeek.value + 1) % 7

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (m == 1) { if (y > 1350) { y--; m = 12 } } else m--
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(tr("السابق", "Previous"), maxLines = 1)
            }

            Text(
                "${months[m - 1]} $y ${if (lang == "ar") "هـ" else "AH"}",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            TextButton(
                onClick = {
                    if (m == 12) { if (y < 1550) { y++; m = 1 } } else m++
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(tr("التالي", "Next"), maxLines = 1)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            days.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp,
                    color = Color(0xFF89B4FA))
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(top = 8.dp)) {
            items(offset) { Spacer(Modifier.height(1.dp)) }
            items(len) { i ->
                val g = firstG.plusDays(i.toLong())
                val isToday = g == today
                Column(
                    Modifier.padding(2.dp)
                        .background(
                            if (isToday) Color(0xFF89B4FA) else Color(0xFF1E1E2E),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${i + 1}", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = if (isToday) Color(0xFF11111B) else Color.White)
                    Text("${g.dayOfMonth}", fontSize = 10.sp,
                        color = if (isToday) Color(0xFF313244) else Color(0xFF7F849C))
                }
            }
        }
    }
}

// ============================ الإعدادات ============================

private fun displayName(ctx: Context, uri: Uri): String {
    try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) return c.getString(0).substringBeforeLast('.') }
    } catch (e: Exception) { }
    return "Custom"
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color(0xFFB69CFF), fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, fontSize = 15.sp)
    }
}

@Composable
private fun FacebookIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Box(
        modifier = modifier.background(Color(0xFF1877F2), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstagramIcon(modifier: Modifier = Modifier.size(24.dp)) {
    val instaGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF833AB4),
                Color(0xFFE1306C),
                Color(0xFFFD1D1D),
                Color(0xFFF56040),
                Color(0xFFFFDC80)
            )
        )
    }
    Box(
        modifier = modifier.background(instaGradient, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color.White,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.25f, h * 0.25f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.24f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 1.2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.28f)
            )
        }
    }
}

@Composable
fun SettingsScreen(prefs: Prefs, onChange: () -> Unit, onLang: (String) -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalLang.current
    val ar = lang == "ar"
    fun t(a: String, e: String) = if (ar) a else e
    val scope = rememberCoroutineScope()

    var auto by remember { mutableStateOf(prefs.autoLocation) }
    var city by remember { mutableIntStateOf(prefs.cityIndex) }
    var method by remember { mutableIntStateOf(prefs.methodIndex) }
    var madhab by remember { mutableIntStateOf(prefs.madhabIndex) }
    var adj by remember { mutableStateOf(prefs.adjustments.toList()) }
    var selected by remember { mutableStateOf(prefs.selected) }
    var customs by remember { mutableStateOf(prefs.customs) }
    var status by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var updateAvail by remember { mutableStateOf<UpdateResult.Available?>(null) }

    DisposableEffect(Unit) { onDispose { Preview.stop() } }

    fun refreshLocation() {
        val l = LocationHelper.last(ctx)
        if (l != null) {
            prefs.lat = l.first
            prefs.lon = l.second
            status = t("✅ تم تحديد الموقع", "✅ Location updated")
        } else {
            status = t("⚠️ تعذّر تحديد الموقع. فعّل GPS وجرّب، أو اختر مدينتك يدوياً.",
                "⚠️ Couldn't get location. Turn on GPS and retry, or pick your city manually.")
        }
        onChange()
    }

    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshLocation() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            val list = customs + CustomAdhan(displayName(ctx, uri), uri.toString())
            customs = list
            prefs.customs = list
            selected = "c:$uri"
            prefs.selected = selected
            status = t("✅ تمت إضافة الأذان", "✅ Adhan added")
            onChange()
        }
    }

    fun labelOf(id: String): String {
        if (id.startsWith("c:"))
            return customs.firstOrNull { "c:${it.uri}" == id }?.name ?: t("أذان مخصص", "Custom adhan")
        val i = (id.removePrefix("b:").toIntOrNull() ?: 0).coerceIn(0, Muadhins.names.size - 1)
        val n = if (ar) Muadhins.names[i] else Strings.muadhinsEn[i]
        val missing = Muadhins.rawUri(ctx, i) == null
        return "%02d. %s%s".format(i + 1, n, if (missing) t("  (غير موجود)", "  (missing)") else "")
    }

    fun previewSelected() {
        val uri: Uri? = if (selected.startsWith("c:")) Uri.parse(selected.removePrefix("c:"))
        else Muadhins.rawUri(ctx, selected.removePrefix("b:").toIntOrNull() ?: 0)
        if (uri == null) status = t("⚠️ ملف هذا المؤذن غير موجود", "⚠️ This adhan file is missing")
        else if (!Preview.play(ctx, uri)) status = t("⚠️ تعذّر تشغيل الملف", "⚠️ Couldn't play the file")
    }

    val allIds = buildList {
        Muadhins.names.indices.forEach { add("b:$it") }
        customs.forEach { add("c:${it.uri}") }
    }
    val version = remember { UpdateChecker.currentVersion(ctx) }

    updateAvail?.let { u ->
        AlertDialog(
            onDismissRequest = { updateAvail = null },
            title = { Text(t("تحديث جديد", "Update available")) },
            text = {
                Text(t("الإصدار ${u.version} متاح (إصدارك الحالي $version).",
                    "Version ${u.version} is available (current: $version)."))
            },
            confirmButton = {
                TextButton(onClick = {
                    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) } catch (e: Exception) { }
                    updateAvail = null
                }) { Text(t("تحميل", "Download")) }
            },
            dismissButton = {
                TextButton(onClick = { updateAvail = null }) { Text(t("لاحقاً", "Later")) }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(t("الإعدادات", "Settings"), fontSize = 26.sp, fontWeight = FontWeight.Bold)

        // ---------- اللغة ----------
        SectionTitle(t("اللغة", "Language"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("العربية", Modifier.weight(1f), if (ar) PURPLE else Color.White) { onLang("ar") }
            GlassButton("English", Modifier.weight(1f), if (!ar) PURPLE else Color.White) { onLang("en") }
        }

        // ---------- الموقع ----------
        SectionTitle(t("الموقع", "Location"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = auto, onCheckedChange = {
                auto = it; prefs.autoLocation = it; onChange()
                if (it) locLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            })
            Spacer(Modifier.width(8.dp))
            Text(t("تحديد الموقع تلقائياً", "Detect location automatically"))
        }
        if (auto) {
            val c = prefs.coords()
            Text(t("الإحداثيات الحالية: ", "Current coordinates: ") +
                    "%.4f, %.4f".format(Locale.US, c.first, c.second),
                fontSize = 12.sp, color = Color.Gray)
            TextButton(onClick = {
                locLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            }) { Text(t("تحديث الموقع الآن", "Update location now")) }
        } else {
            Cities.list.forEachIndexed { i, c ->
                RadioRow(if (ar) c.name else Strings.citiesEn[i], city == i) {
                    city = i; prefs.cityIndex = i
                    method = c.defaultMethod; prefs.methodIndex = c.defaultMethod
                    onChange()
                }
            }
        }

        // ---------- طريقة الحساب ----------
        SectionTitle(t("طريقة حساب المواقيت", "Calculation method"))
        PrayerEngine.methods.forEachIndexed { i, n ->
            RadioRow(if (ar) n else Strings.methodsEn[i], method == i) {
                method = i; prefs.methodIndex = i; onChange()
            }
        }

        // ---------- المذهب الفقهي ----------
        SectionTitle(t("المذهب الفقهي لصلاة العصر", "Juristic method for Asr"))
        PrayerEngine.madhabs.forEachIndexed { i, n ->
            RadioRow(if (ar) n else Strings.madhabsEn[i], madhab == i) {
                madhab = i; prefs.madhabIndex = i; onChange()
            }
        }

        // ---------- ضبط الدقائق ----------
        SectionTitle(t("ضبط الدقائق لكل صلاة (+ تأخير / - تقديم)", "Minute adjustment per prayer (+ later / - earlier)"))
        PrayerEngine.names.forEachIndexed { i, n ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Strings.prayer(n, lang), fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        val l = adj.toMutableList(); l[i] = (l[i] - 1).coerceAtLeast(-30)
                        adj = l; prefs.adjustments = l.toIntArray(); onChange()
                    }) { Text("−") }
                    Text("${adj[i]}", Modifier.width(48.dp), textAlign = TextAlign.Center, fontSize = 16.sp)
                    OutlinedButton(onClick = {
                        val l = adj.toMutableList(); l[i] = (l[i] + 1).coerceAtMost(30)
                        adj = l; prefs.adjustments = l.toIntArray(); onChange()
                    }) { Text("+") }
                }
            }
        }

        // ---------- المؤذن (قائمة منسدلة) ----------
        SectionTitle(t("المؤذن", "Muadhin"))
        Box {
            GlassSurface(
                Modifier.fillMaxWidth().height(54.dp), RoundedCornerShape(14.dp), PURPLE,
                onClick = { expanded = true }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(labelOf(selected), Modifier.weight(1f), maxLines = 1, fontSize = 14.sp,
                        color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allIds.forEach { id ->
                    DropdownMenuItem(
                        text = { Text(labelOf(id), fontSize = 14.sp) },
                        onClick = {
                            selected = id; prefs.selected = id; expanded = false
                            Preview.stop(); onChange()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(t("تجربة المؤذن", "Preview"), Modifier.weight(1f), PURPLE) { previewSelected() }
            GlassButton(t("إيقاف المؤذن", "Stop"), Modifier.weight(1f), RED_GLASS) { Preview.stop() }
        }

        Spacer(Modifier.height(10.dp))
        GlassButton(t("إضافة أذان من الجهاز", "Add adhan from device"), Modifier.fillMaxWidth(), Color.White) {
            picker.launch(arrayOf("audio/*"))
        }
        if (selected.startsWith("c:")) {
            Spacer(Modifier.height(10.dp))
            GlassButton(t("حذف الأذان المختار", "Delete selected adhan"), Modifier.fillMaxWidth(), RED_GLASS) {
                val uriStr = selected.removePrefix("c:")
                val list = customs.filter { it.uri != uriStr }
                customs = list; prefs.customs = list
                try {
                    ctx.contentResolver.releasePersistableUriPermission(
                        Uri.parse(uriStr), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { }
                Preview.stop()
                selected = "b:0"; prefs.selected = selected
                onChange()
            }
        }

        // ---------- التحديثات ----------
        SectionTitle(t("التحديثات", "Updates"))
        GlassButton(
            if (checking) t("جاري البحث...", "Checking...") else t("البحث عن تحديثات", "Check for updates"),
            Modifier.fillMaxWidth(), PURPLE
        ) {
            if (!checking) {
                checking = true
                scope.launch {
                    when (val r = UpdateChecker.check(ctx)) {
                        is UpdateResult.Available -> updateAvail = r
                        UpdateResult.UpToDate -> status = t("✅ أنت تستخدم أحدث إصدار ($version)",
                            "✅ You're on the latest version ($version)")
                        UpdateResult.Failed -> status = t("⚠️ تعذّر البحث عن تحديثات. تأكد من الإنترنت.",
                            "⚠️ Couldn't check for updates. Check your connection.")
                    }
                    checking = false
                }
            }
        }
        Text(t("الإصدار الحالي: ", "Current version: ") + version, fontSize = 12.sp,
            color = Color.Gray, modifier = Modifier.padding(top = 6.dp))

        if (status.isNotEmpty()) {
            Text(status, Modifier.padding(top = 12.dp), fontSize = 13.sp, color = Color(0xFFF9E2AF))
        }

        // ---------- وسائل التواصل الاجتماعي ----------
        SectionTitle(t("وسائل التواصل الاجتماعي", "Social Media"))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://web.facebook.com/eslam.amer.98871174/")))
                    } catch (e: Exception) { }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1877F2).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.5f))
            ) {
                Row(
                    Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FacebookIcon(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Facebook", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Surface(
                onClick = {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/eslam_amer_elomrany/")))
                    } catch (e: Exception) { }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE1306C).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFE1306C).copy(alpha = 0.5f))
            ) {
                Row(
                    Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    InstagramIcon(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Instagram", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(t("للتواصل: ", "Contact: ") + "eslamae420@gmail.com", fontSize = 12.sp, color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}