package ir.techima.kardan

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.Locale

val LocalHost = staticCompositionLocalOf<Host> { error("Host not provided") }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }

class MainActivity : ComponentActivity(), Host {
    private lateinit var state: AppState
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var voiceCallback: ((String?) -> Unit)? = null
    private var credCallback: ((Boolean) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.notifOk = granted || Reminders.canNotify(this)
        state.say(if (state.notifOk) "اعلان‌ها فعال شد." else "اعلان‌ها فعال نشد؛ برای یادآوری باید از تنظیمات گوشی مجوز اعلان را بدهی.")
        if (state.notifOk) Reminders.rescheduleAll(this)
    }
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = if (r.resultCode == Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() else null
        voiceCallback?.invoke(text); voiceCallback = null
    }
    private val credLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        credCallback?.invoke(r.resultCode == Activity.RESULT_OK); credCallback = null
    }
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(Repo.exportJson(this).toByteArray(Charsets.UTF_8)) }
                state.say("پشتیبان ذخیره شد. گاوصندوق رمزها در این فایل نیست (برای امنیت).")
            } catch (e: Exception) { state.say("ذخیرهٔ پشتیبان ناموفق بود.") }
        }
    }
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
                val r = if (raw == null) null else Repo.importJson(this, raw)
                if (r == null) state.say("این فایل پشتیبان معتبر کاردان نیست.") else {
                    state.reload(); Reminders.rescheduleAll(this); state.say(r)
                }
            } catch (e: Exception) { state.say("خواندن فایل ناموفق بود.") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state = AppState(this)
        Reminders.ensureChannel(this)
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        setContent {
            CompositionLocalProvider(LocalHost provides this, LocalApp provides state) {
                KardanTheme(state.settings) { Root(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        state.reload()
    }

    override fun onStop() {
        super.onStop()
        VaultLock.lock()
    }

    override fun onDestroy() {
        tts?.shutdown(); tts = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- Host
    override fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            state.notifOk = Reminders.canNotify(this)
            if (!state.notifOk) openSettings(Target.NOTIFICATIONS)
            else state.say("اعلان‌ها برای کاردان فعال است.")
        }
    }

    override fun startVoice(onText: (String?) -> Unit) {
        voiceCallback = onText
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "بگو چه کاری می‌خواهی")
        try { voiceLauncher.launch(i) } catch (e: ActivityNotFoundException) {
            voiceCallback = null
            state.say("سرویس تشخیص گفتار روی این گوشی پیدا نشد. می‌توانی متن را تایپ کنی.")
        }
    }

    override fun confirmDeviceCredential(title: String, onResult: (Boolean) -> Unit) {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isDeviceSecure) { onResult(false); state.say("برای باز کردن گاوصندوق، ابتدا برای گوشی قفل صفحه (رمز، الگو یا اثر انگشت) تنظیم کن."); return }
        @Suppress("DEPRECATION")
        val i = km.createConfirmDeviceCredentialIntent(title, "برای دیدن رمزها هویتت را تأیید کن")
        if (i == null) { onResult(false); return }
        credCallback = onResult
        try { credLauncher.launch(i) } catch (e: Exception) { credCallback = null; onResult(false) }
    }

    override fun exportBackup() { exportLauncher.launch("kardan-backup.json") }
    override fun importBackup() { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

    override fun setTorch(on: Boolean): Boolean {
        return try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return false
            cm.setTorchMode(id, on)
            state.torchOn = on
            true
        } catch (e: Exception) { false }
    }

    override fun openSettings(target: Target): Boolean {
        val action = when (target) {
            Target.WIFI -> Settings.ACTION_WIFI_SETTINGS
            Target.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            Target.WIRELESS -> Settings.ACTION_WIRELESS_SETTINGS
            Target.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            Target.SOUND -> Settings.ACTION_SOUND_SETTINGS
            Target.STORAGE -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            Target.BATTERY_SAVER -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            Target.APPS -> Settings.ACTION_APPLICATION_SETTINGS
            Target.SECURITY -> Settings.ACTION_SECURITY_SETTINGS
            Target.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            Target.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            Target.DATE -> Settings.ACTION_DATE_SETTINGS
            Target.ROAMING -> Settings.ACTION_DATA_ROAMING_SETTINGS
            Target.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            Target.DEVICE_INFO -> Settings.ACTION_DEVICE_INFO_SETTINGS
            Target.BATTERY_OPT -> Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            Target.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            Target.GENERAL -> Settings.ACTION_SETTINGS
        }
        return try {
            val i = Intent(action)
            if (target == Target.NOTIFICATIONS) i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(i); true
        } catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_SETTINGS)); state.say("صفحهٔ دقیق روی این گوشی باز نشد؛ تنظیمات کلی باز شد."); false }
            catch (e2: Exception) { state.say("باز کردن تنظیمات ممکن نشد."); false }
        }
    }

    override fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { state.say("برنامه‌ای برای باز کردن این لینک پیدا نشد.") }
    }

    override fun dial(number: String) {
        try { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))) }
        catch (e: Exception) { state.say("برنامهٔ تماس پیدا نشد.") }
    }

    override fun sms(number: String) {
        try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))) }
        catch (e: Exception) { state.say("برنامهٔ پیامک پیدا نشد.") }
    }

    override fun copySensitive(label: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        cm.setPrimaryClip(clip)
        state.say("کپی شد؛ ۳۰ ثانیهٔ دیگر از کلیپ‌بورد پاک می‌شود.")
        handler.postDelayed({
            try {
                val cur = cm.primaryClip
                if (cur != null && cur.itemCount > 0 && cur.getItemAt(0).text?.toString() == text) {
                    if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip() else cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (_: Exception) { }
        }, 30_000L)
    }

    override fun share(text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        try { startActivity(Intent.createChooser(i, "اشتراک‌گذاری")) } catch (e: Exception) { state.say("اشتراک‌گذاری ممکن نشد.") }
    }

    override fun speak(text: String): Boolean {
        val t = tts ?: return false
        if (!ttsReady) return false
        val r = t.setLanguage(Locale("fa", "IR"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kardan")
        return true
    }

    override fun stopSpeaking() { tts?.stop() }

    override fun setSecure(secure: Boolean) {
        if (secure) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/** Auto-lock timestamp for the vault; cleared whenever the app leaves the foreground. */
object VaultLock {
    @Volatile var unlockedAt: Long = 0L
    const val TIMEOUT_MS = 2 * 60 * 1000L
    fun lock() { unlockedAt = 0L }
    fun isOpen(): Boolean = unlockedAt != 0L && System.currentTimeMillis() - unlockedAt < TIMEOUT_MS
    fun unlock() { unlockedAt = System.currentTimeMillis() }
}

@Composable
private fun Root(state: AppState) {
    val c = K.c
    val view = LocalView.current
    val snack = androidx.compose.runtime.remember { SnackbarHostState() }
    SideEffect {
        val w = (view.context as Activity).window
        val ctl = WindowCompat.getInsetsController(w, view)
        ctl.isAppearanceLightStatusBars = !c.dark
        ctl.isAppearanceLightNavigationBars = !c.dark
    }
    LaunchedEffect(state.message) {
        val m = state.message
        if (m != null) { snack.showSnackbar(m); state.message = null }
    }
    BackHandler(enabled = state.stack.isNotEmpty() || state.tab != 0) { state.back() }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().imePadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Navigator(state)
            SnackbarHost(snack, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
        }
        BottomBar(state)
    }
}
