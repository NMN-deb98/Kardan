package ir.techima.kardan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local persistence (JSON in private SharedPreferences). Nothing here leaves the device. */
object Repo {
    private const val PREFS = "kardan_local"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private inline fun <T> parse(raw: String?, map: (JSONObject) -> T): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i -> try { map(a.getJSONObject(i)) } catch (_: Exception) { null } }
        } catch (_: Exception) { emptyList() }
    }

    // ---------- tasks
    fun taskToJson(t: Task): JSONObject = JSONObject()
        .put("id", t.id).put("title", t.title).put("notes", t.notes)
        .put("due", t.due ?: JSONObject.NULL).put("priority", t.priority).put("category", t.category)
        .put("repeat", t.repeat).put("remind", t.remind).put("done", t.done).put("est", t.estimateMin)
        .put("created", t.createdAt)
        .put("steps", JSONArray().apply { t.steps.forEach { put(JSONObject().put("t", it.text).put("d", it.done)) } })

    fun taskFromJson(o: JSONObject): Task {
        val steps = o.optJSONArray("steps")
        return Task(
            id = o.getLong("id"), title = o.getString("title"), notes = o.optString("notes", ""),
            due = if (o.isNull("due")) null else o.getLong("due"),
            priority = o.optInt("priority", 1), category = o.optString("category", "شخصی"),
            repeat = o.optInt("repeat", 0), remind = o.optBoolean("remind", false), done = o.optBoolean("done", false),
            steps = if (steps == null) emptyList() else (0 until steps.length()).map {
                val s = steps.getJSONObject(it); TaskStep(s.getString("t"), s.optBoolean("d", false))
            },
            estimateMin = o.optInt("est", 0), createdAt = o.optLong("created", System.currentTimeMillis())
        )
    }

    fun loadTasks(c: Context): List<Task> = parse(prefs(c).getString("tasks", null), ::taskFromJson)
    fun saveTasks(c: Context, list: List<Task>) {
        prefs(c).edit().putString("tasks", JSONArray().apply { list.forEach { put(taskToJson(it)) } }.toString()).apply()
    }

    // ---------- habits
    fun loadHabits(c: Context): List<Habit> = parse(prefs(c).getString("habits", null)) { o ->
        val d = o.optJSONArray("days")
        Habit(o.getLong("id"), o.getString("title"), if (d == null) emptySet() else (0 until d.length()).map { d.getString(it) }.toSet())
    }
    fun saveHabits(c: Context, list: List<Habit>) {
        prefs(c).edit().putString("habits", JSONArray().apply {
            list.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("days", JSONArray(it.days.toList()))) }
        }.toString()).apply()
    }

    // ---------- watchlist
    fun loadWatch(c: Context): List<WatchItem> = parse(prefs(c).getString("watch", null)) { o ->
        WatchItem(o.getLong("id"), o.getString("title"), o.optString("type", "فیلم"), o.optString("status", "می‌خواهم ببینم"), o.optString("note", ""), o.optInt("rating", 0))
    }
    fun saveWatch(c: Context, list: List<WatchItem>) {
        prefs(c).edit().putString("watch", JSONArray().apply {
            list.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("type", it.type).put("status", it.status).put("note", it.note).put("rating", it.rating)) }
        }.toString()).apply()
    }

    // ---------- quick contacts
    fun loadContacts(c: Context): List<QuickContact> = parse(prefs(c).getString("contacts", null)) { o ->
        QuickContact(o.getLong("id"), o.getString("name"), o.getString("number"))
    }
    fun saveContacts(c: Context, list: List<QuickContact>) {
        prefs(c).edit().putString("contacts", JSONArray().apply {
            list.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("number", it.number)) }
        }.toString()).apply()
    }

    // ---------- favourites / settings
    fun loadFavorites(c: Context): List<String> {
        val raw = prefs(c).getString("favorites", null) ?: return emptyList()
        return try { val a = JSONArray(raw); (0 until a.length()).map { a.getString(it) } } catch (_: Exception) { emptyList() }
    }
    fun saveFavorites(c: Context, list: List<String>) {
        prefs(c).edit().putString("favorites", JSONArray(list).toString()).apply()
    }

    fun loadSettings(c: Context): Settings {
        val p = prefs(c)
        return Settings(
            theme = p.getInt("s_theme", 0), senior = p.getBoolean("s_senior", false),
            fontScale = p.getFloat("s_font", 1f), reduceMotion = p.getBoolean("s_motion", false)
        )
    }
    fun saveSettings(c: Context, s: Settings) {
        prefs(c).edit().putInt("s_theme", s.theme).putBoolean("s_senior", s.senior)
            .putFloat("s_font", s.fontScale).putBoolean("s_motion", s.reduceMotion).apply()
    }

    // ---------- backup (everything except the password vault)
    fun exportJson(c: Context): String = JSONObject()
        .put("app", "kardan").put("version", 1).put("exportedAt", System.currentTimeMillis())
        .put("tasks", JSONArray().apply { loadTasks(c).forEach { put(taskToJson(it)) } })
        .put("habits", JSONArray().apply {
            loadHabits(c).forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("days", JSONArray(it.days.toList()))) }
        })
        .put("watch", JSONArray().apply {
            loadWatch(c).forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("type", it.type).put("status", it.status).put("note", it.note).put("rating", it.rating)) }
        })
        .put("contacts", JSONArray().apply {
            loadContacts(c).forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("number", it.number)) }
        })
        .put("favorites", JSONArray(loadFavorites(c)))
        .toString(2)

    /** Imports a backup produced by [exportJson]. Returns a human readable summary or null if invalid. */
    fun importJson(c: Context, raw: String): String? {
        return try {
            val o = JSONObject(raw)
            if (o.optString("app") != "kardan") return null
            val p = prefs(c).edit()
            fun arr(key: String) = o.optJSONArray(key)?.toString()
            arr("tasks")?.let { p.putString("tasks", it) }
            arr("habits")?.let { p.putString("habits", it) }
            arr("watch")?.let { p.putString("watch", it) }
            arr("contacts")?.let { p.putString("contacts", it) }
            arr("favorites")?.let { p.putString("favorites", it) }
            p.apply()
            val t = o.optJSONArray("tasks")?.length() ?: 0
            val w = o.optJSONArray("watch")?.length() ?: 0
            "${Persian.digits(t)} کار و ${Persian.digits(w)} عنوان تماشا بازیابی شد."
        } catch (_: Exception) { null }
    }

    fun wipeAll(c: Context) { prefs(c).edit().clear().apply() }
}
