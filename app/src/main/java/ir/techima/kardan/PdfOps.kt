package ir.techima.kardan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Image and PDF tools that run fully on the device, without extra libraries. */
object PdfOps {
    /** Decodes an image no larger than [maxSide] pixels and applies its EXIF rotation. */
    fun decode(c: Context, uri: Uri, maxSide: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val deg = c.contentResolver.openInputStream(uri)?.use { rotationOf(it) } ?: 0
            val scale = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
            if (deg != 0 || scale < 1f) {
                val m = Matrix(); if (deg != 0) m.postRotate(deg.toFloat()); if (scale < 1f) m.postScale(scale, scale)
                val t = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (t !== bmp) bmp.recycle(); bmp = t
            }
            bmp
        } catch (e: Throwable) { null }
    }

    private fun rotationOf(s: java.io.InputStream): Int = try {
        when (android.media.ExifInterface(s).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) { 0 }

    private fun drawFitted(canvas: Canvas, bmp: Bitmap, pw: Int, ph: Int) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)
        val scale = min(pw.toFloat() / bmp.width, ph.toFloat() / bmp.height)
        val w = bmp.width * scale; val h = bmp.height * scale
        val l = (pw - w) / 2f; val t = (ph - h) / 2f
        canvas.drawBitmap(bmp, null, RectF(l, t, l + w, t + h), paint)
    }

    /** One image per A4 page (portrait or landscape by shape). Returns the number of pages written, or -1 on failure. */
    fun imagesToPdf(c: Context, images: List<Uri>, out: Uri, progress: (Int) -> Unit = {}): Int {
        val doc = PdfDocument()
        var pages = 0
        try {
            for ((i, u) in images.withIndex()) {
                val bmp = decode(c, u, 2000) ?: continue
                val land = bmp.width > bmp.height
                val pw = if (land) 842 else 595; val ph = if (land) 595 else 842
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pages + 1).create())
                drawFitted(page.canvas, bmp, pw, ph)
                doc.finishPage(page); bmp.recycle(); pages++
                progress(i + 1)
            }
            if (pages == 0) return -1
            c.contentResolver.openOutputStream(out)?.use { doc.writeTo(it) } ?: return -1
            return pages
        } catch (e: Throwable) { return -1 } finally { doc.close() }
    }

    /** Merges PDFs by rendering each page to an image (text will no longer be selectable). Returns page count or -1. */
    fun mergePdfs(c: Context, pdfs: List<Uri>, out: Uri, progress: (Int) -> Unit = {}): Int {
        val doc = PdfDocument()
        var pages = 0
        try {
            for (u in pdfs) {
                val pfd: ParcelFileDescriptor = c.contentResolver.openFileDescriptor(u, "r") ?: continue
                val r = PdfRenderer(pfd)
                try {
                    for (i in 0 until r.pageCount) {
                        val p = r.openPage(i)
                        val w = p.width; val h = p.height
                        val bmp = Bitmap.createBitmap((w * 2).coerceAtMost(2400), (h * 2).coerceAtMost(3400), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        p.close()
                        val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pages + 1).create())
                        drawFitted(page.canvas, bmp, w, h)
                        doc.finishPage(page); bmp.recycle(); pages++
                        progress(pages)
                    }
                } finally { r.close(); pfd.close() }
            }
            if (pages == 0) return -1
            c.contentResolver.openOutputStream(out)?.use { doc.writeTo(it) } ?: return -1
            return pages
        } catch (e: Throwable) { return -1 } finally { doc.close() }
    }

    fun pageCount(c: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var r: PdfRenderer? = null
        return try {
            pfd = c.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            r = PdfRenderer(pfd)
            r.pageCount
        } catch (e: Throwable) { 0 } finally { try { r?.close() } catch (_: Throwable) { }; try { pfd?.close() } catch (_: Throwable) { } }
    }

    /** Re-encodes an image as JPEG. Returns the bytes so the UI can show the size before saving. */
    fun compressJpeg(c: Context, uri: Uri, maxSide: Int, quality: Int): ByteArray? {
        val bmp = decode(c, uri, maxSide) ?: return null
        return try {
            val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val cv = Canvas(flat); cv.drawColor(Color.WHITE); cv.drawBitmap(bmp, 0f, 0f, null)
            val bos = ByteArrayOutputStream()
            flat.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), bos)
            bmp.recycle(); flat.recycle()
            bos.toByteArray()
        } catch (e: Throwable) { null }
    }

    fun sizeOf(c: Context, uri: Uri): Long = try {
        c.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    } catch (e: Exception) { 0L }

    fun nameOf(c: Context, uri: Uri): String = try {
        c.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "فایل"
    } catch (e: Exception) { "فایل" }
}
