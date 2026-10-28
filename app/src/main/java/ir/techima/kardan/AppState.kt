package ir.techima.kardan

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.TimeZone

/** What the host (activity) can do for screens: system dialogs, intents, permissions. */
interface Host {
    fun requestNotifications()
    fun startVoice(onText: (String?) -> Unit)
    fun confirmDeviceCredential(title: String, onResult: (Boolean) -> Unit)
    fun exportBackup()
    fun importBackup()
    fun setTorch(on: Boolean): Boolean
    fun openSettings(target: Target): Boolean
    fun openUrl(url: String)
    fun dial(number: String)
    fun sms(number: String)
    fun copySensitive(label: String, text: String)
    fun share(text: String)
    fun speak(text: String): Boolean
    fun stopSpeaking()
    fun setSecure(secure: Boolean)
    // files, documents, camera
    fun pickTree(onResult: (android.net.Uri?) -> Unit)
    fun pickDocuments(mimes: Array<String>, multiple: Boolean, onResult: (List<android.net.Uri>) -> Unit)
    fun createDocument(name: String, mime: String, onResult: (android.net.Uri?) -> Unit)
    fun takePicture(onResult: (android.net.Uri?) -> Unit)
    fun viewUri(uri: android.net.Uri, mime: String?)
    fun shareUri(uri: android.net.Uri, mime: String?)
    fun openSystemDownloads()
    // contacts
    fun hasContactsPermission(): Boolean
    fun requestContacts(onResult: (Boolean) -> Unit)
    fun insertContact(name: String?, phone: String?)
    fun editContact(uri: android.net.Uri)
}

sealed class Screen {
    data object Home : Screen()
    data object Guides : Screen()
    data object Tools : Screen()
    data object Planner : Screen()
    data object More : Screen()
    data class GuideDetail(val id: String) : Screen()
    data class Tool(val route: AppRoute) : Screen()
    data class TaskEdit(val id: Long?, val prefillTitle: String = "") : Screen()
}

class AppState(val ctx: Context) {
    val tasks = mutableStateListOf<Task>()
    val habits = mutableStateListOf<Habit>()
    val watch = mutableStateListOf<WatchItem>()
    val contacts = mutableStateListOf<QuickContact>()
    val favorites = mutableStateListOf<String>()
    var settings by mutableStateOf(Settings())
    var notifOk by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var timerEnd by mutableStateOf(0L)
    var timerTotal by mutableStateOf(0)
    var torchOn by mutableStateOf(false)
    var vaultUnlockedAt by mutableStateOf(0L)

    var tab by mutableStateOf(0)
    val stack = androidx.compose.runtime.mutableStateListOf<Screen>()

    init { reload() }

    fun reload() {
        tasks.clear(); tasks.addAll(Repo.loadTasks(ctx))
        habits.clear(); habits.addAll(Repo.loadHabits(ctx))
        watch.clear(); watch.addAll(Repo.loadWatch(ctx))
        contacts.clear(); contacts.addAll(Repo.loadContacts(ctx))
        favorites.clear(); favorites.addAll(Repo.loadFavorites(ctx))
        settings = Repo.loadSettings(ctx)
        Persian.latin = settings.latinDigits
        notifOk = Reminders.canNotify(ctx)
        val p = ctx.getSharedPreferences("kardan_local", Context.MODE_PRIVATE)
        timerEnd = p.getLong("timer_end", 0L)
        timerTotal = p.getInt("timer_total", 0)
        if (timerEnd != 0L && timerEnd < System.currentTimeMillis() - 3_600_000L) { timerEnd = 0L }
    }

    // ---------------------------------------------------------------- navigation
    val current: Screen get() = stack.lastOrNull() ?: when (tab) { 1 -> Screen.Guides; 2 -> Screen.Tools; 3 -> Screen.Planner; 4 -> Screen.More; else -> Screen.Home }
    fun push(s: Screen) { stack.add(s) }
    fun back(): Boolean { if (stack.isNotEmpty()) { stack.removeAt(stack.size - 1); return true }; if (tab != 0) { tab = 0; return true }; return false }
    fun goTab(i: Int) { stack.clear(); tab = i }
    fun openRoute(r: AppRoute) {
        when (r) {
            AppRoute.PLANNER -> goTab(3)
            else -> push(Screen.Tool(r))
        }
    }

    fun say(text: String) { message = text }

    // ---------------------------------------------------------------- tasks
    private fun persistTasks() = Repo.saveTasks(ctx, tasks.toList())

    /** Saves [t] and (re)schedules its reminder. Returns a human message about what really happened. */
    fun saveTask(t: Task): String {
        val i = tasks.indexOfFirst { it.id == t.id }
        if (i >= 0) tasks[i] = t else tasks.add(0, t)
        persistTasks()
        if (!t.remind || t.due == null) { Reminders.cancel(ctx, t.id); return "کار ذخیره شد." }
        if (t.due <= System.currentTimeMillis()) return "کار ذخیره شد، اما زمان یادآوری گذشته است و اعلانی ساخته نشد."
        notifOk = Reminders.canNotify(ctx)
        val exact = Reminders.schedule(ctx, t)
        return when {
            !notifOk -> "کار ذخیره شد، اما اعلان‌ها برای کاردان بسته است؛ برای دریافت یادآوری، مجوز اعلان را بده."
            exact == false -> "یادآوری ثبت شد، ولی گوشی اجازهٔ زمان‌بندی دقیق نداد؛ ممکن است چند دقیقه دیرتر برسد."
            else -> "یادآوری برای ${Jalali.full(t.due)} ثبت شد."
        }
    }

    fun toggleTask(id: Long) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = tasks[i]
        val doneNow = !t.done
        val next = if (doneNow) t.due?.let { Reminders.nextDue(it, t.repeat) } else null
        val updated = if (doneNow && next != null) t.copy(due = next, done = false, steps = t.steps.map { it.copy(done = false) }) else t.copy(done = doneNow)
        tasks[i] = updated
        persistTasks()
        if (doneNow && next == null) Reminders.cancel(ctx, id) else Reminders.schedule(ctx, updated)
        if (doneNow && next != null) say("کار انجام شد و برای ${Jalali.full(next)} دوباره تنظیم شد.")
    }

    fun deleteTask(id: Long) {
        Reminders.cancel(ctx, id)
        tasks.removeAll { it.id == id }
        persistTasks()
    }

    fun newId(): Long = System.currentTimeMillis() * 1000 + (0..999).random()

    // ---------------------------------------------------------------- habits / watch / contacts / favourites
    fun saveHabits() = Repo.saveHabits(ctx, habits.toList())
    fun saveWatch() = Repo.saveWatch(ctx, watch.toList())
    fun saveContacts() = Repo.saveContacts(ctx, contacts.toList())

    fun toggleFavorite(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        Repo.saveFavorites(ctx, favorites.toList())
    }

    fun updateSettings(s: Settings) { settings = s; Persian.latin = s.latinDigits; Repo.saveSettings(ctx, s) }

    // ---------------------------------------------------------------- timer (also rings with the app closed)
    fun startTimer(seconds: Int): String {
        val end = System.currentTimeMillis() + seconds * 1000L
        timerEnd = end; timerTotal = seconds
        ctx.getSharedPreferences("kardan_local", Context.MODE_PRIVATE).edit().putLong("timer_end", end).putInt("timer_total", seconds).apply()
        notifOk = Reminders.canNotify(ctx)
        val exact = Reminders.scheduleAt(ctx, TIMER_ID, end, "زمان تایمر شما تمام شد.", "", "timer")
        return when {
            !notifOk -> "تایمر شروع شد، اما اعلان‌ها بسته است؛ فقط وقتی برنامه باز است می‌بینی. مجوز اعلان را بده."
            !exact -> "تایمر شروع شد (ممکن است اعلان چند دقیقه دیرتر برسد)."
            else -> "تایمر شروع شد؛ حتی با بستن برنامه اعلان می‌دهد."
        }
    }

    fun cancelTimer() {
        timerEnd = 0L
        Reminders.cancel(ctx, TIMER_ID)
        ctx.getSharedPreferences("kardan_local", Context.MODE_PRIVATE).edit().remove("timer_end").apply()
    }

    companion object { const val TIMER_ID = 9_000_000_000_001L }

    val zone: TimeZone get() = TimeZone.getDefault()
}
