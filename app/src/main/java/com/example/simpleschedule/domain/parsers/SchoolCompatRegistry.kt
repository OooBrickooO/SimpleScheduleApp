package com.example.simpleschedule.domain.parsers

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Host-scoped WebView compatibility rules so school-specific workarounds never affect unrelated portals.
 */
object SchoolCompatRegistry {

    data class HostRule(
        val host: String,
        val forceDesktopLayoutOnLoginPath: String? = null,
        val transparentFallbackPaths: Set<String> = emptySet()
    )

    private val rules = listOf(
        HostRule(
            host = "jw.gxstnu.edu.cn",
            forceDesktopLayoutOnLoginPath = "/jsxsd/",
            transparentFallbackPaths = setOf("/assets_newL/images/index/nav3.png")
        )
    )

    private val transparent1x1Png = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(),
        0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
        0x54, 0x78, 0x9C.toByte(), 0x63, 0x60, 0x00, 0x00,
        0x00, 0x02, 0x00, 0x01, 0xE5.toByte(), 0x27, 0xD4.toByte(),
        0xA2.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
        0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
    )

    fun ruleForUrl(url: String?): HostRule? {
        if (url.isNullOrBlank()) return null
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return null
        return rules.firstOrNull { it.host.equals(host, ignoreCase = true) }
    }

    fun shouldApplyLegacyDesktopWidth(url: String?): Boolean {
        val rule = ruleForUrl(url) ?: return false
        val pathSuffix = rule.forceDesktopLayoutOnLoginPath ?: return false
        return url?.endsWith(pathSuffix, ignoreCase = true) == true
    }

    fun interceptResourceIfNeeded(uri: Uri?): WebResourceResponse? {
        if (uri == null) return null
        val host = uri.host ?: return null
        val path = uri.path ?: return null
        val rule = rules.firstOrNull { it.host.equals(host, ignoreCase = true) } ?: return null
        if (rule.transparentFallbackPaths.any { it.equals(path, ignoreCase = true) }) {
            return WebResourceResponse("image/png", null, ByteArrayInputStream(transparent1x1Png))
        }
        return null
    }
}
