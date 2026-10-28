package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "صبح بخیر"
    in 12..16 -> "ظهر بخیر"
    in 17..20 -> "عصر بخیر"
    else -> "شب بخیر"
}

/** Runs one assistant action honestly; returns a message for the user. */
fun runAction(state: AppState, host: Host, a: AssistantAction) {
    when (a) {
        is AssistantAction.Open -> state.openRoute(a.route)
        is AssistantAction.OpenGuide -> state.push(Screen.GuideDetail(a.id))
        is AssistantAction.OpenSettings -> host.openSettings(a.target)
        is AssistantAction.CreateTask -> {
            val t = Task(id = state.newId(), title = a.title, due = a.due, remind = a.remind && a.due != null)
            state.say(state.saveTask(t))
        }
        is AssistantAction.StartTimer -> { state.say(state.startTimer(a.seconds)); state.openRoute(AppRoute.TIMER) }
        is AssistantAction.Torch -> {
            val ok = host.setTorch(a.on)
            state.say(if (ok) (if (a.on) "چراغ‌قوه روشن شد." else "چراغ‌قوه خاموش شد.") else "این گوشی چراغ‌قوه ندارد یا دسترسی داده نشد.")
        }
    }
}

@Composable
fun HomeScreen(state: AppState) {
    val c = K.c
    val host = LocalHost.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    var input by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<AssistantReply?>(null) }
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    fun ask(text: String) {
        if (text.isBlank()) return
        reply = Assistant.reply(text, System.currentTimeMillis(), state.zone)
    }

    Page("کاردان", "کارهایت را هوشمندانه انجام بده", scroll = true) {
        // hero
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.hero).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(greeting(hour), style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color.White)
            Text(Jalali.full(now), style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f))
            val open = state.tasks.count { !it.done }
            val today = state.tasks.count { !it.done && it.due != null && Jalali.startOfDay(it.due) == Jalali.startOfDay(now) }
            Text(
                if (open == 0) "کار بازی نداری؛ می‌توانی یک کار جدید بسازی." else "${Persian.digits(open)} کار باز داری" + if (today > 0) "، ${Persian.digits(today)} تا برای امروز." else ".",
                style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
        }

        // assistant
        KCard {
            Text("دستیار کاردان", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("بنویس یا بگو چه کاری می‌خواهی. دستیار آفلاین است و فقط کارهایی را که واقعاً از پسشان برمی‌آید انجام می‌دهد.", style = MaterialTheme.typography.bodySmall, color = c.muted)
            KField(input, { input = it }, "مثلاً: فردا ساعت ۹ یادآوری بساز: خرید نان", trailing = {
                Row {
                    IconButton(onClick = { host.startVoice { t -> if (t != null) { input = t; ask(t) } else state.say("چیزی شنیده نشد.") } }) { Icon(Icons.Filled.Mic, "گفتار", tint = c.accent) }
                    IconButton(onClick = { ask(input) }) { Icon(Icons.AutoMirrored.Filled.Send, "ارسال", tint = c.primary) }
                }
            })
            val r = reply
            if (r != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusTag(r.status) }
                Text(r.message, style = MaterialTheme.typography.bodyLarge, color = c.text)
                if (r.actions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        r.actions.forEach { a -> Chip(a.label, true) { runAction(state, host, a) } }
                    }
                }
                if (r.suggestions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        r.suggestions.forEach { s -> Chip(s, false) { input = s; ask(s) } }
                    }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Assistant.examples.forEach { e -> Chip(e, false) { input = e; ask(e) } }
                }
            }
        }

        if (!state.notifOk) {
            KCard(onClick = { host.requestNotifications() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Filled.Notifications, c.warn)
                    Column(Modifier.weight(1f)) {
                        Text("اعلان‌ها بسته است", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text("بدون مجوز اعلان، یادآوری‌ها و تایمر وقتی برنامه بسته است به تو نمی‌رسند. برای فعال‌سازی لمس کن.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }

        // next tasks
        val upcoming = state.tasks.filter { !it.done }.sortedWith(compareBy({ it.due ?: Long.MAX_VALUE }, { -it.priority })).take(3)
        SectionTitle("کارهای پیش رو")
        if (upcoming.isEmpty()) {
            KCard(onClick = { state.goTab(3) }) { Text("هنوز کاری ثبت نکرده‌ای. برای ساختن کار جدید لمس کن.", color = c.muted, style = MaterialTheme.typography.bodyMedium) }
        } else upcoming.forEach { t ->
            KCard(onClick = { state.push(Screen.TaskEdit(t.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(Icons.Filled.CheckCircle, if (t.priority == 2) c.danger else c.primary, 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(t.due?.let { Jalali.full(it) } ?: "بدون زمان", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    if (t.remind) Tag("یادآور", c.accent)
                }
            }
        }

        // favourites / quick tools
        val favTools = Tools.all.filter { state.favorites.contains("tool:" + it.route.name) }
        SectionTitle(if (favTools.isEmpty()) "ابزارهای پرکاربرد" else "ابزارهای محبوب تو")
        val shown = if (favTools.isEmpty()) Tools.all.take(6) else favTools
        shown.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t -> ToolTile(t, Modifier.weight(1f)) { state.openRoute(t.route) } }
                if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        Text("مرور راهنماها: ${Persian.digits(GuideData.all.size)} راهنمای مرحله‌به‌مرحله، بازبینی ${GuideData.REVIEWED}.", style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
fun ToolTile(t: ToolInfo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = K.c
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(c.card).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconBadge(t.icon, t.tint(c))
        Text(t.title, style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(t.desc, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
