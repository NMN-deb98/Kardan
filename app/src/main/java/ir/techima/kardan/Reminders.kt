package ir.techima.kardan

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/** Real reminders: AlarmManager + notifications, rescheduled after reboot. */
object Reminders {
    const val CHANNEL = "kardan_reminders"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_KIND = "kind" // task | timer

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val m = c.getSystemService(NotificationManager::class.java)
            if (m.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "یادآوری کارها و تایمرهای کاردان"
                m.createNotificationChannel(ch)
            }
        }
    }

    fun canNotify(c: Context): Boolean = NotificationManagerCompat.from(c).areNotificationsEnabled()

    fun canExact(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun pending(c: Context, id: Long, title: String, text: String, kind: String): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_TEXT, text).putExtra(EXTRA_KIND, kind)
        i.action = "ir.techima.kardan.REMIND.$id"
        return PendingIntent.getBroadcast(c, (id and 0x7fffffff).toInt(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Returns true when an exact alarm was set, false when only an inexact one was possible. */
    fun scheduleAt(c: Context, id: Long, at: Long, title: String, text: String, kind: String): Boolean {
        ensureChannel(c)
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pending(c, id, title, text, kind)
        return if (canExact(c)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); true
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); false
        }
    }

    fun cancel(c: Context, id: Long) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(pending(c, id, "", "", "task"))
    }

    fun schedule(c: Context, t: Task): Boolean? {
        cancel(c, t.id)
        val due = t.due ?: return null
        if (!t.remind || t.done || due <= System.currentTimeMillis()) return null
        return scheduleAt(c, t.id, due, t.title, if (t.notes.isBlank()) "زمان انجام این کار رسیده است." else t.notes, "task")
    }

    fun rescheduleAll(c: Context) {
        Repo.loadTasks(c).forEach { schedule(c, it) }
    }

    /** Next occurrence for a repeating task, or null. */
    fun nextDue(due: Long, repeat: Int): Long? {
        if (repeat == 0) return null
        val cal = Calendar.getInstance().apply { timeInMillis = due }
        when (repeat) {
            1 -> cal.add(Calendar.DAY_OF_YEAR, 1)
            2 -> cal.add(Calendar.DAY_OF_YEAR, 7)
            3 -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_ID, 0L)
        val title = intent.getStringExtra(Reminders.EXTRA_TITLE) ?: "یادآوری کاردان"
        val text = intent.getStringExtra(Reminders.EXTRA_TEXT) ?: ""
        val kind = intent.getStringExtra(Reminders.EXTRA_KIND) ?: "task"
        Reminders.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (kind == "timer") "تایمر تمام شد" else title)
            .setContentText(if (kind == "timer") title else text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (kind == "timer") title else text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify((id and 0x7fffffff).toInt(), n)
            }
        } catch (_: SecurityException) { /* permission revoked while running */ }

        if (kind == "task") {
            val tasks = Repo.loadTasks(context).toMutableList()
            val idx = tasks.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val t = tasks[idx]
                val next = t.due?.let { Reminders.nextDue(it, t.repeat) }
                if (next != null) {
                    var n2: Long = next
                    while (n2 <= System.currentTimeMillis()) n2 = Reminders.nextDue(n2, t.repeat) ?: break
                    val moved = t.copy(due = n2, done = false)
                    tasks[idx] = moved
                    Repo.saveTasks(context, tasks)
                    Reminders.schedule(context, moved)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a == Intent.ACTION_BOOT_COMPLETED || a == Intent.ACTION_MY_PACKAGE_REPLACED || a == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" || a == Intent.ACTION_TIMEZONE_CHANGED || a == Intent.ACTION_TIME_CHANGED) {
            Reminders.rescheduleAll(context)
        }
    }
}
