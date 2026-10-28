package ir.techima.kardan

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import java.security.MessageDigest

data class FileEntry(
    val docId: String, val uri: Uri, val name: String, val mime: String,
    val size: Long, val modified: Long, val isDir: Boolean
) {
    val ext: String get() = name.substringAfterLast('.', "").lowercase()
}

/** File browsing on folders the user explicitly granted (Storage Access Framework). No blanket storage permission is used. */
object FileOps {
    private const val PREFS = "kardan_local"
    private const val KEY = "file_trees"

    fun loadTrees(c: Context): List<Uri> {
        val raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try { val a = JSONArray(raw); (0 until a.length()).map { Uri.parse(a.getString(it)) } } catch (e: Exception) { emptyList() }
    }

    fun saveTrees(c: Context, list: List<Uri>) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, JSONArray(list.map { it.toString() }).toString()).apply()
    }

    /** Keeps only folders whose permission is still granted. */
    fun validTrees(c: Context): List<Uri> {
        val granted = c.contentResolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri }.toSet()
        val list = loadTrees(c).filter { granted.contains(it) }
        if (list.size != loadTrees(c).size) saveTrees(c, list)
        return list
    }

    fun rootId(tree: Uri): String = DocumentsContract.getTreeDocumentId(tree)

    fun docUri(tree: Uri, docId: String): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)

    fun treeLabel(tree: Uri): String {
        val id = rootId(tree)
        val last = id.substringAfter(':', id).trimEnd('/').substringAfterLast('/')
        return if (last.isBlank()) "حافظهٔ داخلی" else last
    }

    fun children(c: Context, tree: Uri, parentId: String): List<FileEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val out = ArrayList<FileEntry>()
        try {
            c.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ), null, null, null
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val id = cur.getString(0); val mime = cur.getString(2) ?: ""
                    out.add(FileEntry(id, docUri(tree, id), cur.getString(1) ?: "?", mime, if (cur.isNull(3)) 0L else cur.getLong(3), if (cur.isNull(4)) 0L else cur.getLong(4), mime == DocumentsContract.Document.MIME_TYPE_DIR))
                }
            }
        } catch (e: Exception) { /* folder removed or permission lost */ }
        return out
    }

    /** All files under [rootId], walking sub-folders; stops at [limit] files. */
    fun scan(c: Context, tree: Uri, rootId: String, limit: Int = 20000): List<FileEntry> {
        val out = ArrayList<FileEntry>()
        val stack = ArrayDeque<String>(); stack.add(rootId)
        while (stack.isNotEmpty() && out.size < limit) {
            for (e in children(c, tree, stack.removeLast())) {
                if (e.isDir) stack.add(e.docId) else out.add(e)
                if (out.size >= limit) break
            }
        }
        return out
    }

    fun delete(c: Context, uri: Uri): Boolean = try { DocumentsContract.deleteDocument(c.contentResolver, uri) } catch (e: Exception) { false }

    fun rename(c: Context, uri: Uri, newName: String): Uri? = try { DocumentsContract.renameDocument(c.contentResolver, uri, newName) } catch (e: Exception) { null }

    /** Groups of byte-identical files (same size, then same SHA-256). Only files of at least [minSize] bytes are hashed. */
    fun duplicates(c: Context, files: List<FileEntry>, minSize: Long = 1024, maxSize: Long = 300L * 1024 * 1024, progress: (Int, Int) -> Unit = { _, _ -> }): List<List<FileEntry>> {
        val bySize = files.filter { it.size in minSize..maxSize }.groupBy { it.size }.filter { it.value.size > 1 }
        val total = bySize.values.sumOf { it.size }
        var done = 0
        val groups = ArrayList<List<FileEntry>>()
        for ((_, same) in bySize) {
            val byHash = HashMap<String, MutableList<FileEntry>>()
            for (f in same) {
                val h = sha256(c, f.uri)
                if (h != null) byHash.getOrPut(h) { ArrayList() }.add(f)
                done++; progress(done, total)
            }
            byHash.values.filter { it.size > 1 }.forEach { groups.add(it.sortedBy { f -> f.modified }) }
        }
        return groups.sortedByDescending { it[0].size * (it.size - 1) }
    }

    private fun sha256(c: Context, uri: Uri): String? = try {
        val md = MessageDigest.getInstance("SHA-256")
        c.contentResolver.openInputStream(uri)?.use { s ->
            val buf = ByteArray(64 * 1024); var n = s.read(buf)
            while (n > 0) { md.update(buf, 0, n); n = s.read(buf) }
        } ?: null
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { null }

    fun kind(e: FileEntry): String = when {
        e.isDir -> "پوشه"
        e.mime.startsWith("image/") -> "عکس"
        e.mime.startsWith("video/") -> "ویدیو"
        e.mime.startsWith("audio/") -> "صدا"
        e.mime == "application/pdf" -> "PDF"
        e.ext in listOf("doc", "docx", "odt", "txt", "rtf") -> "سند"
        e.ext in listOf("xls", "xlsx", "csv", "ods") -> "جدول"
        e.ext in listOf("ppt", "pptx", "odp") -> "ارائه"
        e.ext in listOf("zip", "rar", "7z", "tar", "gz") -> "فشرده"
        e.ext == "apk" -> "برنامه"
        else -> "فایل"
    }

    fun sizeText(bytes: Long): String = Persian.bytes(bytes)
}
