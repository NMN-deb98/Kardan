package ir.techima.kardan

import android.content.Context
import android.provider.ContactsContract

data class PhoneContact(val id: Long, val name: String, val number: String)

/** Reads the phone's contacts. Only called after the user allowed contacts access; nothing is uploaded or stored. */
object PhoneContacts {
    fun digits(n: String): String = Persian.latinDigits(n).filter { it.isDigit() }

    /** Same person written with/without country code → same key. */
    fun key(n: String): String = digits(n).takeLast(10)

    fun load(ctx: Context): List<PhoneContact> {
        val out = LinkedHashMap<String, PhoneContact>()
        try {
            val cols = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                val iId = c.getColumnIndex(cols[0]); val iName = c.getColumnIndex(cols[1]); val iNum = c.getColumnIndex(cols[2])
                while (c.moveToNext()) {
                    val name = c.getString(iName)?.trim().orEmpty()
                    val num = c.getString(iNum)?.trim().orEmpty()
                    if (num.isEmpty()) continue
                    val k = key(num)
                    if (k.length < 3 || out.containsKey(name + "|" + k)) continue
                    out[name + "|" + k] = PhoneContact(c.getLong(iId), name.ifBlank { num }, num)
                }
            }
        } catch (_: SecurityException) { return emptyList() } catch (_: Exception) { }
        return out.values.toList()
    }

    /** Name search that tolerates typos; digits search the number. */
    fun filter(all: List<PhoneContact>, query: String): List<PhoneContact> {
        val q = query.trim()
        if (q.isEmpty()) return all
        val qd = digits(q)
        return all.filter { Fuzzy.matches(it.name, q) || (qd.length >= 3 && digits(it.number).contains(qd)) }
    }
}
