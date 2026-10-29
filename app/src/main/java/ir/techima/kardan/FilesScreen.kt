package ir.techima.kardan

import android.net.Uri
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun iconOf(e: FileEntry): ImageVector = when (FileOps.kind(e)) {
    "پوشه" -> Icons.Filled.Folder
    "عکس" -> Icons.Filled.Image
    "ویدیو" -> Icons.Filled.Movie
    "صدا" -> Icons.Filled.Audiotrack
    "PDF" -> Icons.Filled.PictureAsPdf
    "سند" -> Icons.Filled.Description
    "جدول" -> Icons.Filled.TableChart
    "فشرده" -> Icons.Filled.Archive
    else -> Icons.Filled.InsertDriveFile
}

@Composable
fun FilesScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    val scope = rememberCoroutineScope()
    val trees = remember { mutableStateListOf<Uri>().apply { addAll(FileOps.validTrees(state.ctx)) } }
    var tree by remember { mutableStateOf<Uri?>(null) }
    val stack = remember { mutableStateListOf<Pair<String, String>>() } // docId to name
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(0) }
    var q by remember { mutableStateOf("") }
    var action by remember { mutableStateOf<FileEntry?>(null) }
    var renaming by remember { mutableStateOf<FileEntry?>(null) }
    var newName by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<FileEntry?>(null) }
    var all by remember { mutableStateOf<List<FileEntry>?>(null) }
    var dups by remember { mutableStateOf<List<List<FileEntry>>?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    val t = tree
    val currentId = if (t == null) "" else (stack.lastOrNull()?.first ?: FileOps.rootId(t))

    LaunchedEffect(t, currentId, refresh) {
        if (t != null) {
            loading = true
            entries = withContext(Dispatchers.IO) { FileOps.children(state.ctx, t, currentId) }
            loading = false
        }
    }

    fun openTree(u: Uri) { tree = u; stack.clear(); mode = 0; all = null; dups = null; q = "" }
    fun ensureScan(after: (List<FileEntry>) -> Unit) {
        val tr = tree ?: return
        val have = all
        if (have != null) { after(have); return }
        busy = true; progress = 0f
        scope.launch {
            val list = withContext(Dispatchers.IO) { FileOps.scan(state.ctx, tr, FileOps.rootId(tr)) }
            all = list; busy = false; after(list)
        }
    }

    val backAction: () -> Unit = {
        if (t != null && stack.isNotEmpty()) { stack.removeAt(stack.size - 1) }
        else if (t != null) { tree = null }
        else onBack()
    }

    // دکمهٔ برگشت سیستم باید اول یک پوشه عقب برود، نه کل بخش فایل را ببندد
    BackHandler(enabled = t != null) { backAction() }

    Page(title, if (t == null) "فقط پوشه‌هایی که خودت اجازه می‌دهی" else FileOps.treeLabel(t) + stack.joinToString("") { " ‹ " + it.second }, backAction) {
        if (t == null) {
            KCard {
                Text("کاردان به کل حافظه دسترسی نمی‌گیرد. هر پوشه‌ای را که اینجا اضافه کنی، فقط همان پوشه (و زیرپوشه‌هایش) در دسترس است و هر وقت خواستی می‌توانی دسترسی را از تنظیمات گوشی بگیری.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
                KButton("افزودن پوشه", {
                    host.pickTree { u ->
                        if (u != null) {
                            if (!trees.contains(u)) { trees.add(u); FileOps.saveTrees(state.ctx, trees.toList()) }
                            openTree(u)
                        }
                    }
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
                KOutlineButton("باز کردن دانلودهای سیستم", { host.openSystemDownloads() }, Modifier.fillMaxWidth())
                Text("نکته: در اندروید ۱۱ و بالاتر، سیستم اجازه نمی‌دهد خودِ پوشهٔ «Download» یا ریشهٔ حافظه انتخاب شود؛ یک زیرپوشه (مثل Documents، DCIM، Pictures یا پوشهٔ پیام‌رسان‌ها) را انتخاب کن.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            }
            if (trees.isNotEmpty()) SectionTitle("پوشه‌های من")
            trees.toList().forEach { u ->
                KCard(onClick = { openTree(u) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Filled.Folder, c.warn)
                        Text(FileOps.treeLabel(u), style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            try { state.ctx.contentResolver.releasePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) { }
                            trees.remove(u); FileOps.saveTrees(state.ctx, trees.toList())
                        }) { Icon(Icons.Filled.Delete, "حذف دسترسی", tint = c.muted) }
                    }
                }
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("مرور", "فایل‌های حجیم", "تکراری‌ها", "آمار حجم").forEachIndexed { i, n -> Chip(n, mode == i) { mode = i } }
            }
            if (busy) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.cardHigh); Text("در حال بررسی پوشه…", color = c.muted) }

            when (mode) {
                0 -> {
                    KField(q, { q = it }, "جست‌وجو در این پوشه")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("نام", "حجم", "تاریخ").forEachIndexed { i, n -> Chip("مرتب: $n", sort == i) { sort = i } }
                        Chip("نوسازی", false) { refresh++ }
                    }
                    val list = entries.filter { q.isBlank() || Persian.containsLoose(it.name, q) }
                        .sortedWith(compareBy<FileEntry> { !it.isDir }.thenComparator { a, b ->
                            when (sort) { 1 -> b.size.compareTo(a.size); 2 -> b.modified.compareTo(a.modified); else -> a.name.compareTo(b.name, ignoreCase = true) }
                        })
                    if (loading) Text("در حال خواندن…", color = c.muted)
                    else if (list.isEmpty()) EmptyState(Icons.Filled.Folder, "چیزی پیدا نشد", "این پوشه خالی است یا با جست‌وجوی تو نمی‌خواند.")
                    list.forEach { e ->
                        KCard(onClick = { if (e.isDir) { stack.add(e.docId to e.name); q = "" } else action = e }, padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBadge(iconOf(e), if (e.isDir) c.warn else c.accent, 40.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text((if (e.isDir) "پوشه" else FileOps.sizeText(e.size)) + (if (e.modified > 0) " · " + Jalali.format(e.modified) else ""), style = MaterialTheme.typography.bodySmall, color = c.muted)
                                }
                                if (!e.isDir) Tag(FileOps.kind(e))
                            }
                        }
                    }
                }
                1 -> {
                    if (all == null && !busy) KButton("پیدا کردن فایل‌های حجیم", { ensureScan { } }, Modifier.fillMaxWidth())
                    val big = all?.sortedByDescending { it.size }?.take(50)
                    if (big != null) {
                        Text("حجیم‌ترین فایل‌های این پوشه و زیرپوشه‌ها (" + Persian.digits(all!!.size) + " فایل بررسی شد). پیش از حذف، هر فایل را باز کن و مطمئن شو.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                        big.forEach { e ->
                            KCard(onClick = { action = e }, padding = 12.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconBadge(iconOf(e), c.danger, 40.dp)
                                    Column(Modifier.weight(1f)) { Text(e.name, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium); Text(FileOps.kind(e), color = c.muted, style = MaterialTheme.typography.bodySmall) }
                                    Text(FileOps.sizeText(e.size), color = c.warn, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (dups == null && !busy) KButton("پیدا کردن فایل‌های تکراری", {
                        busy = true; progress = 0f
                        scope.launch {
                            val files = all ?: withContext(Dispatchers.IO) { FileOps.scan(state.ctx, t, FileOps.rootId(t)) }.also { all = it }
                            dups = withContext(Dispatchers.IO) { FileOps.duplicates(state.ctx, files) { d, tot -> progress = if (tot == 0) 1f else d.toFloat() / tot } }
                            busy = false
                        }
                    }, Modifier.fillMaxWidth())
                    if (busy && progress > 0f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = c.primary, trackColor = c.cardHigh)
                    val d = dups
                    if (d != null) {
                        if (d.isEmpty()) EmptyState(Icons.Filled.Folder, "تکراری پیدا نشد", "فایلی با محتوای دقیقاً یکسان پیدا نشد.")
                        else {
                            val waste = d.sumOf { it[0].size * (it.size - 1) }
                            Text("${Persian.digits(d.size)} گروه فایل یکسان؛ حدود ${FileOps.sizeText(waste)} فضای قابل آزاد‌سازی. فقط فایل‌های با محتوای کاملاً یکسان (بر پایهٔ اثر انگشت SHA-256) نشان داده می‌شوند.", color = c.muted, style = MaterialTheme.typography.bodySmall)
                            d.take(40).forEach { g ->
                                KCard {
                                    Text(g[0].name + " · " + FileOps.sizeText(g[0].size) + " · " + Persian.digits(g.size) + " نسخه", style = MaterialTheme.typography.titleMedium, color = c.text)
                                    g.forEachIndexed { i, f ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text((if (i == 0) "قدیمی‌ترین: " else "") + f.name + (if (f.modified > 0) " (" + Jalali.format(f.modified) + ")" else ""), color = c.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                            TextButton(onClick = { action = f }) { Text("گزینه‌ها") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (all == null && !busy) KButton("بررسی حجم بر حسب نوع", { ensureScan { } }, Modifier.fillMaxWidth())
                    val a = all
                    if (a != null) {
                        val groups = a.groupBy { FileOps.kind(it) }.map { (k, v) -> Triple(k, v.size, v.sumOf { it.size }) }.sortedByDescending { it.third }
                        val total = a.sumOf { it.size }.coerceAtLeast(1)
                        Text("جمع: " + FileOps.sizeText(total) + " در " + Persian.digits(a.size) + " فایل", color = c.text, style = MaterialTheme.typography.titleMedium)
                        groups.forEach { (k, n, sz) ->
                            KCard {
                                Row { Text(k + " (" + Persian.digits(n) + ")", color = c.text, modifier = Modifier.weight(1f)); Text(FileOps.sizeText(sz), color = c.muted) }
                                LinearProgressIndicator(progress = { (sz.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.cardHigh)
                            }
                        }
                    }
                }
            }
        }
    }

    action?.let { e ->
        AlertDialog(
            onDismissRequest = { action = null },
            title = { Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(FileOps.kind(e) + " · " + FileOps.sizeText(e.size)) },
            confirmButton = {
                Column {
                    TextButton(onClick = { host.viewUri(e.uri, e.mime); action = null }) { Text("باز کردن") }
                    TextButton(onClick = { host.shareUri(e.uri, e.mime); action = null }) { Text("اشتراک‌گذاری") }
                    TextButton(onClick = { renaming = e; newName = e.name; action = null }) { Text("تغییر نام") }
                    TextButton(onClick = { deleting = e; action = null }) { Text("حذف…", color = c.danger) }
                }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("بستن") } }
        )
    }
    renaming?.let { e ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("تغییر نام") },
            text = { KField(newName, { newName = it }, "نام جدید (با پسوند)") },
            confirmButton = { TextButton(onClick = {
                val ok = newName.isNotBlank() && newName != e.name && FileOps.rename(state.ctx, e.uri, newName.trim()) != null
                state.say(if (ok) "نام فایل عوض شد." else "تغییر نام انجام نشد (ممکن است نام تکراری باشد یا پوشه فقط‌خواندنی باشد).")
                renaming = null; all = null; dups = null; refresh++
            }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("انصراف") } }
        )
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف فایل؟") },
            text = { Text("«${e.name}» (${FileOps.sizeText(e.size)}) برای همیشه از گوشی پاک می‌شود و به سطل زباله نمی‌رود.") },
            confirmButton = { TextButton(onClick = {
                val ok = FileOps.delete(state.ctx, e.uri)
                state.say(if (ok) "فایل حذف شد." else "حذف انجام نشد.")
                deleting = null; all = null; dups = null; refresh++
            }) { Text("حذف کن", color = c.danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}
