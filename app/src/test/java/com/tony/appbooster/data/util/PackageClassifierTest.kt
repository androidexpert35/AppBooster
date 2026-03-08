package com.tony.appbooster.data.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for [PackageClassifier].
 *
 * Verifies overlay detection logic across all supported signal types:
 * name patterns, overlay path signals, and overlayTarget= markers.
 */
class PackageClassifierTest {

    // ── overlayTarget marker (definitive) ─────────────────────────────────────

    @Test
    fun `given dump with overlayTarget= when isOverlayLike then returns true`() {
        val dump = """
            Package [com.example.overlay] (abcd):
                codePath=/data/app/com.example.overlay
                overlayTarget=com.android.systemui
        """.trimIndent()
        assertTrue(PackageClassifier.isOverlayLike("com.example.overlay", dump))
    }

    // ── overlay path signals ──────────────────────────────────────────────────

    @Test
    fun `given codePath in product overlay when isOverlayLike then returns true`() {
        val dump = "codePath=/product/overlay/SomeOverlay.apk"
        assertTrue(PackageClassifier.isOverlayLike("com.example.app", dump))
    }

    @Test
    fun `given codePath in system overlay when isOverlayLike then returns true`() {
        val dump = "codePath=/system/overlay/SomeOverlay.apk"
        assertTrue(PackageClassifier.isOverlayLike("com.example.app", dump))
    }

    @Test
    fun `given codePath in vendor overlay when isOverlayLike then returns true`() {
        val dump = "codePath=/vendor/overlay/SomeOverlay.apk"
        assertTrue(PackageClassifier.isOverlayLike("com.example.app", dump))
    }

    @Test
    fun `given codePath in odm overlay when isOverlayLike then returns true`() {
        val dump = "codePath=/odm/overlay/SomeOverlay.apk"
        assertTrue(PackageClassifier.isOverlayLike("com.example.app", dump))
    }

    @Test
    fun `given resourcePath in product overlay when isOverlayLike then returns true`() {
        val dump = "resourcePath=/product/overlay/SomeOverlay.apk"
        assertTrue(PackageClassifier.isOverlayLike("com.example.app", dump))
    }

    // ── name-based signals ────────────────────────────────────────────────────

    @Test
    fun `given package name containing auto_generated_rro when isOverlayLike then returns true`() {
        assertTrue(PackageClassifier.isOverlayLike("com.android.systemui.auto_generated_rro_product__", null))
    }

    @Test
    fun `given package name containing rro_ prefix when isOverlayLike then returns true`() {
        assertTrue(PackageClassifier.isOverlayLike("com.example.rro_myoverlay", null))
    }

    @Test
    fun `given package name containing dot rro when isOverlayLike then returns true`() {
        assertTrue(PackageClassifier.isOverlayLike("com.example.app.rro", null))
    }

    @Test
    fun `given package name containing overlay keyword when isOverlayLike then returns true`() {
        assertTrue(PackageClassifier.isOverlayLike("com.android.theme.overlay.dark", null))
    }

    // ── normal apps ────────────────────────────────────────────────────────────

    @Test
    fun `given regular app codePath when isOverlayLike then returns false`() {
        val dump = """
            Package [com.example.normal] (aaaa):
                codePath=/data/app/~~hash==/com.example.normal-abc/base.apk
                resourcePath=/data/app/~~hash==/com.example.normal-abc/base.apk
        """.trimIndent()
        assertFalse(PackageClassifier.isOverlayLike("com.example.normal", dump))
    }

    @Test
    fun `given null dump and normal package name when isOverlayLike then returns false`() {
        assertFalse(PackageClassifier.isOverlayLike("com.example.myapp", null))
    }

    @Test
    fun `given empty dump and normal package name when isOverlayLike then returns false`() {
        assertFalse(PackageClassifier.isOverlayLike("com.example.myapp", ""))
    }

    @Test
    fun `given system app with normal paths and no overlay signals when isOverlayLike then returns false`() {
        val dump = """
            Package [com.android.settings] (1234):
                codePath=/system/app/Settings/Settings.apk
                resourcePath=/system/app/Settings/Settings.apk
                flags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ]
        """.trimIndent()
        assertFalse(PackageClassifier.isOverlayLike("com.android.settings", dump))
    }
}

