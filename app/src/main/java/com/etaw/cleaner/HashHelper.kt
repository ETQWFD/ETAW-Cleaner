package com.etaw.cleaner

import java.security.MessageDigest

object HashHelper {

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun appFingerprint(app: AppInfo): String {
        val raw = "${app.pkg}|${app.name}|${app.website ?: ""}|${app.installTime}"
        return sha256(raw)
    }
}
