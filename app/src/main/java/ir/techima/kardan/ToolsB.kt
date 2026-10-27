package ir.techima.kardan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private fun clock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val x = s % 60
    return Persian.digits(if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, x) else String.format(java.util.Locale.US, "%02d:%02d", m, x))
}

@Composable
fun TimerTool(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }
    var minutes by remember { mutableStateOf("5") }
    val running = state.timerEnd > now
    val left = if (running) (state.timerEnd - now + 999) / 1000 else 0
    Page(title, "با بستن برنامه هم اعلان می‌دهد (اگر مجوز اعلان داده باشی)", onBack) {
        KCard {
            Text(if (running) clock(left) else if (state.timerEnd != 0L) "تمام شد" else "آماده", fontSize = 48.sp, fontWeight = FontWeight.Black, color = if (running) c.primary else c.text, modifier = Modifier.fillMaxWidth())
            if (running && state.timerTotal > 0) {
                LinearProgressIndicator(progress = { (1f - left.toFloat() / state.timerTotal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.cardHigh)
            }
            if (!state.notifOk) Text("اعلان‌ها بسته است؛ در پس‌زمینه صدایی نمی‌آید.", color = c.warn, style = MaterialTheme.typography.bodySmall)
        }
        if (running) KOutlineButton("لغو تایمر", { state.cancelTimer() }, Modifier.fillMaxWidth(), danger = true) else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10, 15, 25, 45, 60).forEach { m -> Chip(Persian.digits(m) + " دقیقه", false) { state.say(state.startTimer(m * 60)) } }
            }
            KField(minutes, { minutes = it }, "دقیقهٔ دلخواه", keyboard = KeyboardType.Number)
            KButton("شروع", {
                val m = Persian.latinDigits(minutes).toDoubleOrNull()
                if (m == null || m <= 0 || m > 1440) state.say("عددی بین ۱ تا ۱۴۴۰ دقیقه وارد کن.") else state.say(state.startTimer((m * 60).toInt()))
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StopwatchTool(title: String, onBack: () -> Unit) {
    val c = K.c
    var running by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(0L) }      // accumulated millis while paused
    var startedAt by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running) { while (running) { now = System.currentTimeMillis(); delay(50) } }
    val elapsed = base + if (running) now - startedAt else 0L
    fun text(ms: Long): String {
        val t = ms / 10
        return Persian.digits(String.format(java.util.Locale.US, "%02d:%02d٫%02d", t / 6000, (t / 100) % 60, t % 100)).replace("٫", "٫")
    }
    Page(title, null, onBack) {
        KCard { Text(text(elapsed), fontSize = 44.sp, fontWeight = FontWeight.Black, color = c.text, modifier = Modifier.fillMaxWidth()) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (!running) KButton(if (elapsed == 0L) "شروع" else "ادامه", { startedAt = System.currentTimeMillis(); now = startedAt; running = true }, Modifier.weight(1f))
            else KButton("توقف", { base += System.currentTimeMillis() - startedAt; running = false }, Modifier.weight(1f))
            KOutlineButton(if (running) "ثبت دور" else "صفر کردن", {
                if (running) laps.add(0, elapsed) else { base = 0; laps.clear() }
            }, Modifier.weight(1f))
        }
        laps.forEachIndexed { i, l ->
            KCard { Row { Text("دور " + Persian.digits(laps.size - i), color = c.muted, modifier = Modifier.weight(1f)); Text(text(l), color = c.text, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun WorldTool(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(10_000) } }
    var q by remember { mutableStateOf("") }
    val pinned = WorldClock.cities.filter { state.favorites.contains("city:" + it.name) }
    val others = WorldClock.cities.filter { !state.favorites.contains("city:" + it.name) && (q.isBlank() || Persian.containsLoose(it.name + " " + it.country + " " + it.aliases, q)) }
    @Composable fun row(ci: City) {
        val pin = state.favorites.contains("city:" + ci.name)
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ci.name + "، " + ci.country, style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(WorldClock.dateText(ci.zone, now) + " · " + WorldClock.diffText(WorldClock.diffToTehranMinutes(ci.zone, now)), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Text(WorldClock.timeText(ci.zone, now), fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.primary)
                IconButton(onClick = { state.toggleFavorite("city:" + ci.name) }) { Icon(if (pin) Icons.Filled.Star else Icons.Filled.StarBorder, "سنجاق", tint = if (pin) c.warn else c.muted) }
            }
        }
    }
    Page(title, "ساعت ۲۴ ساعته؛ تغییر ساعت تابستانی خودکار لحاظ می‌شود", onBack) {
        KField(q, { q = it }, "جست‌وجوی شهر یا کشور")
        if (pinned.isNotEmpty()) { SectionTitle("سنجاق‌شده‌ها"); pinned.forEach { row(it) } }
        SectionTitle("شهرها")
        others.forEach { row(it) }
    }
}
