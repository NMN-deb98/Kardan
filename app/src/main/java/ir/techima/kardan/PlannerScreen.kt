package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val DAY = 86_400_000L

private fun dayKey(millis: Long): String { val d = Jalali.fromMillis(millis); return "${d.year}-${d.month}-${d.day}" }

@Composable
fun PlannerScreen(state: AppState) {
    var sub by remember { mutableStateOf(0) }
    Page("برنامه‌ریز", "کارها، عادت‌ها و تقویم شمسی") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("کارها", sub == 0) { sub = 0 }
            Chip("عادت‌ها", sub == 1) { sub = 1 }
            Chip("تقویم", sub == 2) { sub = 2 }
        }
        when (sub) { 0 -> TasksPane(state); 1 -> HabitsPane(state); else -> CalendarPane(state) }
    }
}

@Composable
private fun TaskRow(state: AppState, t: Task) {
    val c = K.c
    val overdue = !t.done && t.due != null && t.due < System.currentTimeMillis()
    KCard(onClick = { state.push(Screen.TaskEdit(t.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { state.toggleTask(t.id) }) {
                Icon(if (t.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, if (t.done) "انجام‌شده" else "انجام نشده", tint = if (t.done) c.ok else c.muted)
            }
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.titleMedium, color = if (t.done) c.muted else c.text)
                val sub = buildString {
                    append(t.due?.let { Jalali.full(it) } ?: "بدون زمان")
                    if (t.repeat != 0) append(" · ").append(Categories.repeatNames[t.repeat])
                    if (t.steps.isNotEmpty()) append(" · ").append(Persian.digits(t.steps.count { it.done })).append("/").append(Persian.digits(t.steps.size)).append(" گام")
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = if (overdue) c.danger else c.muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Tag(Categories.priorityNames[t.priority], when (t.priority) { 2 -> c.danger; 0 -> c.muted; else -> c.accent })
                if (t.remind) Tag("یادآور", c.ok)
            }
        }
    }
}

@Composable
private fun TasksPane(state: AppState) {
    val c = K.c
    var filter by remember { mutableStateOf(0) }
    var cat by remember { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()
    val sod = Jalali.startOfDay(now)
    val list = state.tasks.filter { t ->
        (cat == null || t.category == cat) && when (filter) {
            1 -> !t.done && t.due != null && t.due >= sod && t.due < sod + DAY
            2 -> !t.done
            3 -> t.done
            else -> true
        }
    }.sortedWith(compareBy({ it.done }, { it.due ?: Long.MAX_VALUE }, { -it.priority }))
    KButton("کار جدید", { state.push(Screen.TaskEdit(null)) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("همه", "امروز", "باز", "انجام‌شده").forEachIndexed { i, n -> Chip(n, filter == i) { filter = i } }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("همهٔ دسته‌ها", cat == null) { cat = null }
        Categories.task.forEach { k -> Chip(k, cat == k) { cat = if (cat == k) null else k } }
    }
    if (list.isEmpty()) EmptyState(Icons.Filled.CheckCircle, "کاری نیست", "با «کار جدید» اولین کارت را بساز. می‌توانی یادآور هم بگذاری.")
    else list.forEach { TaskRow(state, it) }
    val done = state.tasks.count { it.done }
    if (done > 0 && filter == 3) KOutlineButton("پاک کردن همهٔ انجام‌شده‌ها", {
        state.tasks.filter { it.done }.forEach { state.deleteTask(it.id) }
    }, Modifier.fillMaxWidth(), danger = true)
}

@Composable
private fun HabitsPane(state: AppState) {
    val c = K.c
    var name by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    KCard {
        Text("عادت تازه", style = MaterialTheme.typography.titleMedium, color = c.text)
        KField(name, { name = it }, "مثلاً: ۳۰ دقیقه پیاده‌روی")
        KButton("افزودن عادت", {
            if (name.isBlank()) state.say("نام عادت را بنویس.") else {
                state.habits.add(Habit(state.newId(), name.trim())); state.saveHabits(); name = ""
            }
        }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
    }
    if (state.habits.isEmpty()) EmptyState(Icons.Filled.CheckCircle, "هنوز عادتی نداری", "عادت‌های کوچک و روزانه را اینجا پیگیری کن؛ هر روز با یک لمس علامت بزن.")
    state.habits.toList().forEachIndexed { idx, h ->
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(h.title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                var streak = 0
                var k = if (h.days.contains(dayKey(now))) 0 else 1
                while (h.days.contains(dayKey(now - k * DAY)) && streak < 3650) { streak++; k++ }
                Tag(Persian.digits(streak) + " روز پیاپی", if (streak > 0) c.ok else c.muted)
                IconButton(onClick = { state.habits.removeAt(idx); state.saveHabits() }) { Icon(Icons.Filled.Delete, "حذف عادت", tint = c.muted) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (back in 6 downTo 0) {
                    val m = now - back * DAY
                    val key = dayKey(m)
                    val on = h.days.contains(key)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Jalali.weekDays[Jalali.weekDayIndex(m)].take(1), style = MaterialTheme.typography.bodySmall, color = c.muted)
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(if (on) c.primary else c.cardHigh)
                                .border(1.dp, if (back == 0) c.primary else c.line, CircleShape)
                                .clickable {
                                    val nd = if (on) h.days - key else h.days + key
                                    state.habits[idx] = h.copy(days = nd); state.saveHabits()
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(Persian.digits(Jalali.fromMillis(m).day), color = if (on) c.onPrimary else c.text, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarPane(state: AppState) {
    val c = K.c
    val today = Jalali.fromMillis(System.currentTimeMillis())
    var y by remember { mutableStateOf(today.year) }
    var m by remember { mutableStateOf(today.month) }
    var sel by remember { mutableStateOf(today.day) }
    val dim = Jalali.daysInMonth(y, m)
    val first = Jalali.weekDayIndex(Jalali.millisOf(Jalali.Date(y, m, 1), 12, 0))
    fun tasksOn(day: Int): List<Task> {
        val s = Jalali.millisOf(Jalali.Date(y, m, day), 0, 0)
        return state.tasks.filter { it.due != null && it.due >= s && it.due < s + DAY }
    }
    KCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (m == 1) { m = 12; y -= 1 } else m -= 1; sel = 1 }) { Icon(Icons.Filled.ChevronRight, "ماه قبل", tint = c.text) }
            Text(Jalali.monthNames[m - 1] + " " + Persian.digits(y), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, color = c.text)
            IconButton(onClick = { if (m == 12) { m = 1; y += 1 } else m += 1; sel = 1 }) { Icon(Icons.Filled.ChevronLeft, "ماه بعد", tint = c.text) }
        }
        Row(Modifier.fillMaxWidth()) {
            Jalali.weekDays.forEach { Text(it.take(1), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = c.muted, style = MaterialTheme.typography.labelMedium) }
        }
        val cells = first + dim
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = r * 7 + col - first + 1
                    if (day < 1 || day > dim) { Box(Modifier.weight(1f).height(44.dp)) } else {
                        val isToday = y == today.year && m == today.month && day == today.day
                        val has = tasksOn(day).isNotEmpty()
                        val isSel = day == sel
                        val friday = col == 6
                        Box(
                            Modifier.weight(1f).height(44.dp).padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) c.primary else if (isToday) c.cardHigh else c.card)
                                .clickable { sel = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(Persian.digits(day), color = if (isSel) c.onPrimary else if (friday) c.danger else c.text, fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal)
                                if (has) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) c.onPrimary else c.accent))
                            }
                        }
                    }
                }
            }
        }
        Text("جمعه‌ها با رنگ متفاوت نشان داده می‌شوند. تعطیلات رسمی در این نسخه درج نشده تا اطلاعات نادرست نشان داده نشود.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
    SectionTitle(Persian.digits(sel) + " " + Jalali.monthNames[m - 1])
    val list = tasksOn(sel)
    if (list.isEmpty()) Text("کاری برای این روز نیست.", color = c.muted) else list.forEach { TaskRow(state, it) }
    KButton("افزودن کار", {
        state.push(Screen.TaskEdit(null, ""))
    }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
}

@Composable
fun TaskEditScreen(state: AppState, id: Long?, prefill: String) {
    val c = K.c
    val host = LocalHost.current
    val existing = remember(id) { state.tasks.firstOrNull { it.id == id } }
    val base = existing?.due ?: (Jalali.startOfDay(System.currentTimeMillis()) + DAY + 9 * 3_600_000L)
    val bd = Jalali.fromMillis(base)
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = base }
    var title by remember(id) { mutableStateOf(existing?.title ?: prefill) }
    var notes by remember(id) { mutableStateOf(existing?.notes ?: "") }
    var category by remember(id) { mutableStateOf(existing?.category ?: "شخصی") }
    var priority by remember(id) { mutableStateOf(existing?.priority ?: 1) }
    var repeat by remember(id) { mutableStateOf(existing?.repeat ?: 0) }
    var remind by remember(id) { mutableStateOf(existing?.remind ?: false) }
    var hasDue by remember(id) { mutableStateOf(existing?.due != null) }
    var dy by remember(id) { mutableStateOf(Persian.digits(bd.year)) }
    var dm by remember(id) { mutableStateOf(Persian.digits(bd.month)) }
    var dd by remember(id) { mutableStateOf(Persian.digits(bd.day)) }
    var hh by remember(id) { mutableStateOf(Persian.digits(String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.HOUR_OF_DAY)))) }
    var mm by remember(id) { mutableStateOf(Persian.digits(String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MINUTE)))) }
    val steps = remember(id) { mutableStateListOf<TaskStep>().apply { addAll(existing?.steps ?: emptyList()) } }
    var newStep by remember(id) { mutableStateOf("") }
    var est by remember(id) { mutableStateOf(if ((existing?.estimateMin ?: 0) > 0) Persian.digits(existing!!.estimateMin) else "") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun setDay(offset: Int) {
        val d = Jalali.fromMillis(Jalali.startOfDay(System.currentTimeMillis()) + offset * DAY + 12 * 3_600_000L)
        dy = Persian.digits(d.year); dm = Persian.digits(d.month); dd = Persian.digits(d.day); hasDue = true
    }

    fun save() {
        if (title.isBlank()) { state.say("عنوان کار را بنویس."); return }
        var due: Long? = null
        if (hasDue) {
            val y = Persian.latinDigits(dy).toIntOrNull(); val mo = Persian.latinDigits(dm).toIntOrNull(); val d = Persian.latinDigits(dd).toIntOrNull()
            val h = Persian.latinDigits(hh).toIntOrNull(); val mi = Persian.latinDigits(mm).toIntOrNull()
            if (y == null || mo == null || d == null || h == null || mi == null || mo !in 1..12 || y !in 1300..1600 || d < 1 || d > Jalali.daysInMonth(y, mo) || h !in 0..23 || mi !in 0..59) {
                state.say("تاریخ یا ساعت درست نیست. ساعت را ۲۴ساعته بنویس (مثلاً ۱۴:۳۰)."); return
            }
            due = Jalali.millisOf(Jalali.Date(y, mo, d), h, mi)
        }
        val t = Task(
            id = existing?.id ?: state.newId(), title = title.trim(), notes = notes.trim(), due = due, priority = priority,
            category = category, repeat = if (due == null) 0 else repeat, remind = remind && due != null, done = existing?.done ?: false,
            steps = steps.toList(), estimateMin = Persian.latinDigits(est).toIntOrNull() ?: 0, createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        state.say(state.saveTask(t))
        state.back()
    }

    Page(if (existing == null) "کار جدید" else "ویرایش کار", null, { state.back() }) {
        KField(title, { title = it }, "عنوان کار")
        KField(notes, { notes = it }, "یادداشت (اختیاری)", singleLine = false, minLines = 2)
        SectionTitle("دسته")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Categories.task.forEach { k -> Chip(k, category == k) { category = k } } }
        SectionTitle("اولویت")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Categories.priorityNames.forEachIndexed { i, n -> Chip(n, priority == i) { priority = i } } }
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("زمان‌بندی", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                Switch(checked = hasDue, onCheckedChange = { hasDue = it; if (!it) remind = false })
            }
            if (hasDue) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("امروز", false) { setDay(0) }; Chip("فردا", false) { setDay(1) }; Chip("پس‌فردا", false) { setDay(2) }; Chip("یک هفته بعد", false) { setDay(7) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KField(dy, { dy = it }, "سال", Modifier.weight(1.3f), keyboard = KeyboardType.Number)
                    KField(dm, { dm = it }, "ماه", Modifier.weight(1f), keyboard = KeyboardType.Number)
                    KField(dd, { dd = it }, "روز", Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KField(hh, { hh = it }, "ساعت (۰ تا ۲۳)", Modifier.weight(1f), keyboard = KeyboardType.Number)
                    KField(mm, { mm = it }, "دقیقه", Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("یادآوری با اعلان", color = c.text, modifier = Modifier.weight(1f))
                    Switch(checked = remind, onCheckedChange = { remind = it; if (it && !state.notifOk) host.requestNotifications() })
                }
                if (remind && !state.notifOk) Text("اعلان‌ها هنوز مجاز نیست؛ بعد از ذخیره، مجوز اعلان را بده تا یادآوری برسد.", color = c.warn, style = MaterialTheme.typography.bodySmall)
                SectionTitle("تکرار")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Categories.repeatNames.forEachIndexed { i, n -> Chip(n, repeat == i) { repeat = i } } }
            }
        }
        KField(est, { est = it }, "زمان تخمینی (دقیقه، اختیاری)", keyboard = KeyboardType.Number)
        SectionTitle("گام‌های کار")
        steps.toList().forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { steps[i] = s.copy(done = !s.done) }) { Icon(if (s.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (s.done) c.ok else c.muted) }
                Text(s.text, color = if (s.done) c.muted else c.text, modifier = Modifier.weight(1f))
                IconButton(onClick = { steps.removeAt(i) }) { Icon(Icons.Filled.Close, "حذف گام", tint = c.muted) }
            }
        }
        KField(newStep, { newStep = it }, "گام تازه", trailing = {
            IconButton(onClick = { if (newStep.isNotBlank()) { steps.add(TaskStep(newStep.trim())); newStep = "" } }) { Icon(Icons.Filled.Add, "افزودن گام", tint = c.primary) }
        })
        KButton("ذخیره", { save() }, Modifier.fillMaxWidth())
        if (existing != null) KOutlineButton("حذف این کار", { confirmDelete = true }, Modifier.fillMaxWidth(), danger = true, icon = Icons.Filled.Delete)
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف کار؟") },
            text = { Text("«${existing.title}» برای همیشه حذف می‌شود و یادآورش هم لغو می‌شود.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; state.deleteTask(existing.id); state.say("کار حذف شد."); state.back() }) { Text("حذف", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
}
