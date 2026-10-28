package ir.techima.kardan

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfToolsScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    val images = remember { mutableStateListOf<Uri>() }
    val pdfs = remember { mutableStateListOf<Uri>() }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<Uri?>(null) }
    var resultMime by remember { mutableStateOf("application/pdf") }
    var resultText by remember { mutableStateOf("") }

    // image compression
    var one by remember { mutableStateOf<Uri?>(null) }
    var maxSide by remember { mutableStateOf(1600) }
    var quality by remember { mutableStateOf(75f) }
    var before by remember { mutableStateOf(0L) }
    var after by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(one, maxSide, quality) {
        val u = one
        if (u != null) {
            before = withContext(Dispatchers.IO) { PdfOps.sizeOf(state.ctx, u) }
            after = withContext(Dispatchers.IO) { PdfOps.compressJpeg(state.ctx, u, maxSide, quality.toInt())?.size?.toLong() }
        }
    }

    fun nameStamp(): String = "kardan-" + Jalali.fromMillis(System.currentTimeMillis()).let { String.format(java.util.Locale.US, "%04d%02d%02d", it.year, it.month, it.day) } + "-" + (System.currentTimeMillis() % 100000)

    @Composable fun listBlock(list: androidx.compose.runtime.snapshots.SnapshotStateList<Uri>, isPdf: Boolean) {
        list.toList().forEachIndexed { i, u ->
            KCard(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Persian.digits(i + 1) + ". " + PdfOps.nameOf(state.ctx, u), color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { if (i > 0) { val t = list[i]; list[i] = list[i - 1]; list[i - 1] = t } }) { Icon(Icons.Filled.ArrowUpward, "بالا", tint = c.muted) }
                    IconButton(onClick = { if (i < list.size - 1) { val t = list[i]; list[i] = list[i + 1]; list[i + 1] = t } }) { Icon(Icons.Filled.ArrowDownward, "پایین", tint = c.muted) }
                    IconButton(onClick = { list.removeAt(i) }) { Icon(Icons.Filled.Close, "حذف از فهرست", tint = c.muted) }
                }
            }
        }
    }

    Page(title, "همه‌چیز روی خود گوشی انجام می‌شود", onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("عکس ← PDF", "ادغام PDF", "کاهش حجم عکس").forEachIndexed { i, n -> Chip(n, tab == i) { tab = i; result = null } }
        }
        if (busy) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.cardHigh); Text("در حال ساخت… (${Persian.digits(progress)})", color = c.muted) }

        when (tab) {
            0 -> {
                Text("چند عکس را به ترتیب انتخاب کن (یا با دوربین اسکن کن) تا هر عکس یک صفحهٔ A4 شود.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KOutlineButton("انتخاب عکس‌ها", { host.pickDocuments(arrayOf("image/*"), true) { images.addAll(it) } }, Modifier.weight(1f), icon = Icons.Filled.Image)
                    KOutlineButton("اسکن با دوربین", { host.takePicture { u -> if (u != null) images.add(u) } }, Modifier.weight(1f), icon = Icons.Filled.CameraAlt)
                }
                listBlock(images, false)
                if (images.isNotEmpty()) KButton("ساخت PDF (${Persian.digits(images.size)} صفحه)", {
                    host.createDocument(nameStamp() + ".pdf", "application/pdf") { out ->
                        if (out != null) {
                            busy = true; progress = 0; result = null
                            scope.launch {
                                val n = withContext(Dispatchers.IO) { PdfOps.imagesToPdf(state.ctx, images.toList(), out) { progress = it } }
                                busy = false
                                if (n > 0) { result = out; resultMime = "application/pdf"; resultText = "PDF با ${Persian.digits(n)} صفحه ساخته شد." } else state.say("ساخت PDF ناموفق بود.")
                            }
                        }
                    }
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.PictureAsPdf)
            }
            1 -> {
                Text("چند فایل PDF را انتخاب کن تا به ترتیب پشت هم قرار بگیرند. هر صفحه به‌صورت تصویر ذخیره می‌شود؛ پس متنِ فایل نهایی قابل انتخاب و جست‌وجو نیست و حجم آن بیشتر می‌شود.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
                KOutlineButton("انتخاب PDFها", { host.pickDocuments(arrayOf("application/pdf"), true) { pdfs.addAll(it) } }, Modifier.fillMaxWidth(), icon = Icons.Filled.PictureAsPdf)
                listBlock(pdfs, true)
                if (pdfs.size >= 2) KButton("ادغام ${Persian.digits(pdfs.size)} فایل", {
                    host.createDocument(nameStamp() + "-merged.pdf", "application/pdf") { out ->
                        if (out != null) {
                            busy = true; progress = 0; result = null
                            scope.launch {
                                val n = withContext(Dispatchers.IO) { PdfOps.mergePdfs(state.ctx, pdfs.toList(), out) { progress = it } }
                                busy = false
                                if (n > 0) { result = out; resultMime = "application/pdf"; resultText = "فایل ادغام‌شده با ${Persian.digits(n)} صفحه ساخته شد." } else state.say("ادغام ناموفق بود (ممکن است فایلی رمزدار یا خراب باشد).")
                            }
                        }
                    }
                }, Modifier.fillMaxWidth())
            }
            else -> {
                KOutlineButton(if (one == null) "انتخاب عکس" else "انتخاب عکس دیگر", { host.pickDocuments(arrayOf("image/*"), false) { l -> if (l.isNotEmpty()) { one = l[0]; result = null } } }, Modifier.fillMaxWidth(), icon = Icons.Filled.Image)
                if (one != null) {
                    KCard {
                        Text(PdfOps.nameOf(state.ctx, one!!), color = c.text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("بزرگ‌ترین ضلع (پیکسل)", color = c.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf(1080, 1600, 2400, 4000).forEach { m -> Chip(Persian.digits(m), maxSide == m) { maxSide = m } }
                        }
                        Text("کیفیت: " + Persian.digits(quality.toInt()) + "٪", color = c.muted)
                        Slider(value = quality, onValueChange = { quality = it }, valueRange = 40f..95f, colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary))
                        val a = after
                        Text(if (a == null) "در حال محاسبه…" else "اندازه: " + Persian.bytes(before) + " ← " + Persian.bytes(a) + if (before > 0) " (" + Persian.digits(((1.0 - a.toDouble() / before) * 100).toInt().coerceIn(-999, 99)) + "٪ کمتر)" else "", color = c.primary, style = MaterialTheme.typography.titleMedium)
                    }
                    KButton("ذخیرهٔ عکس کم‌حجم", {
                        val u = one!!
                        host.createDocument(nameStamp() + ".jpg", "image/jpeg") { out ->
                            if (out != null) scope.launch {
                                busy = true
                                val ok = withContext(Dispatchers.IO) {
                                    val bytes = PdfOps.compressJpeg(state.ctx, u, maxSide, quality.toInt())
                                    if (bytes == null) false else try { state.ctx.contentResolver.openOutputStream(out)?.use { it.write(bytes) }; true } catch (e: Exception) { false }
                                }
                                busy = false
                                if (ok) { result = out; resultMime = "image/jpeg"; resultText = "عکس کم‌حجم ذخیره شد." } else state.say("ذخیره ناموفق بود.")
                            }
                        }
                    }, Modifier.fillMaxWidth())
                    Text("خروجی JPEG است؛ شفافیت عکس (مثل PNG) به زمینهٔ سفید تبدیل می‌شود.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        val r = result
        if (r != null) KCard {
            Text(resultText, color = c.ok, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KOutlineButton("باز کردن", { host.viewUri(r, resultMime) }, Modifier.weight(1f))
                KOutlineButton("اشتراک", { host.shareUri(r, resultMime) }, Modifier.weight(1f))
            }
        }
    }
}
