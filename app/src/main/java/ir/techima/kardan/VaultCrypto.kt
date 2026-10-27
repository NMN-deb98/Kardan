package ir.techima.kardan

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultItem(val id: Long, val title: String, val user: String, val pass: String, val note: String)

/** AES-256-GCM with a non-exportable Android Keystore key. Nothing leaves the device. */
object VaultCrypto {
    private const val ALIAS = "kardan_vault_key_v1"
    private const val PREFS = "kardan_vault"
    private const val KEY_ITEMS = "items"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val iv = c.iv
        return iv + c.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return c.doFinal(blob, 12, blob.size - 12)
    }

    /** Returns null when the stored data cannot be decrypted (e.g. key lost); empty list when nothing stored. */
    fun load(ctx: Context): List<VaultItem>? {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(String(decrypt(Base64.decode(raw, Base64.NO_WRAP)), Charsets.UTF_8))
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                VaultItem(o.getLong("id"), o.getString("t"), o.optString("u", ""), o.optString("p", ""), o.optString("n", ""))
            }
        } catch (e: Exception) { null }
    }

    fun save(ctx: Context, items: List<VaultItem>): Boolean = try {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("t", it.title).put("u", it.user).put("p", it.pass).put("n", it.note)) }
        val blob = Base64.encodeToString(encrypt(arr.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, blob).apply()
        true
    } catch (e: Exception) { false }

    fun wipe(ctx: Context) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply() }
}
