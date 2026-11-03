package ir.techima.kardan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Only secure (https) web pages may be shown inside the in-app panel. */
object WebRules {
    fun allowed(url: String?): Boolean {
        val u = url?.trim()?.lowercase() ?: return false
        return u.startsWith("https://")
    }
}

/**
 * Full-screen in-app browser panel (for Google search / Google Translate).
 * JavaScript is needed by those sites; file and content access are off, and only http(s) is followed.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPanel(url: String, title: String, onClose: () -> Unit, onOpenExternal: (String) -> Unit) {
    val c = K.c
    var web by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf(url) }
    DisposableEffect(Unit) { onDispose { web?.destroy(); web = null } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = c.bg) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { val w = web; if (w != null && w.canGoBack()) w.goBack() else onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "برگشت", tint = c.text)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { onOpenExternal(current) }) { Icon(Icons.Filled.OpenInNew, "باز کردن در مرورگر", tint = c.muted) }
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "بستن", tint = c.text) }
                }
                if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.cardHigh)
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = false
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                                    !WebRules.allowed(request?.url?.toString())
                                override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) { if (u != null) current = u }
                            }
                            loadUrl(url)
                            web = this
                        }
                    }
                )
            }
        }
    }
}

/** Builds the search/translate URLs the user explicitly asks for. Text is URL-encoded; nothing else is added. */
object WebUrls {
    val movieScopes = listOf("همه نتیجه‌ها", "IMDb", "ویکی‌پدیا")
    private val typeWords = listOf("فیلم", "سریال", "انیمیشن", "مستند")

    /** adds the content type word when the user did not write one, so results stay about films and series */
    fun movieQuery(query: String, type: String): String {
        val q = query.trim()
        return if (typeWords.any { q.contains(it) }) q else "$type $q".trim()
    }

    fun google(query: String, scope: String = movieScopes[0]): String {
        val q = when (scope) {
            "IMDb" -> query.trim() + " site:imdb.com"
            "ویکی‌پدیا" -> query.trim() + " site:fa.wikipedia.org"
            else -> query.trim()
        }
        return "https://www.google.com/search?hl=fa&q=" + java.net.URLEncoder.encode(q, "UTF-8")
    }

    fun translate(text: String, targetLang: String): String =
        "https://translate.google.com/?sl=auto&tl=" + targetLang + "&op=translate&text=" + java.net.URLEncoder.encode(text.take(3000), "UTF-8")
}
