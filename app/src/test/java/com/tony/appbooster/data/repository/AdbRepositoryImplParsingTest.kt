package com.tony.appbooster.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for parsing utilities inside [AdbRepositoryImpl].
 *
 * Note: This uses reflection to access private parsing helpers. We keep it lightweight
 * because production correctness here is critical across many Android versions.
 */
class AdbRepositoryImplParsingTest {

    @Test
    fun `given true output when parseCompileCheckNeedsOptimization then returns true`() {
        assertEquals(true, DexoptStatusParser.parseCompileCheckNeedsOptimization("true"))
    }

    @Test
    fun `given false output when parseCompileCheckNeedsOptimization then returns false`() {
        assertEquals(false, DexoptStatusParser.parseCompileCheckNeedsOptimization("false"))
    }

    @Test
    fun `given compilation not needed text when parseCompileCheckNeedsOptimization then returns false`() {
        assertEquals(
            false,
            DexoptStatusParser.parseCompileCheckNeedsOptimization("Compilation not needed")
        )
    }

    @Test
    fun `given compilation needed text when parseCompileCheckNeedsOptimization then returns true`() {
        assertEquals(
            true,
            DexoptStatusParser.parseCompileCheckNeedsOptimization("Compilation needed")
        )
    }

    @Test
    fun `given unknown output when parseCompileCheckNeedsOptimization then returns null`() {
        assertEquals(null, DexoptStatusParser.parseCompileCheckNeedsOptimization("cmd: unknown option"))
    }

    @Test
    fun `given dexopt dump containing speed-profile when parseCompilerFilterFromDexoptDump then detects speed-profile`() {
        val dump = """
            Dexopt state:
              Package [com.example.app]
                compiler-filter=speed-profile
        """.trimIndent()

        assertEquals(
            "speed-profile",
            DexoptStatusParser.parseCompilerFilterFromDexoptDump("com.example.app", dump)
        )
    }

    @Test
    fun `given dexopt dump containing status speed when parseCompilerFilterFromDexoptDump then detects speed`() {
        val dump = """
            something
            com.example.app
              [status=speed]
        """.trimIndent()

        assertEquals(
            "speed",
            DexoptStatusParser.parseCompilerFilterFromDexoptDump("com.example.app", dump)
        )
    }

    @Test
    fun `given dexopt dump with only bracketed package when parseCompilerFilterFromDexoptDump then returns unknown-present`() {
        val dump = """
            Dexopt state:
              [com.android.systemui.auto_generated_rro_product__]
        """.trimIndent()

        assertEquals(
            "unknown-present",
            DexoptStatusParser.parseCompilerFilterFromDexoptDump(
                packageName = "com.android.systemui.auto_generated_rro_product__",
                dump = dump
            )
        )
        assertTrue(
            DexoptStatusParser.isPackagePresentInDexoptDump(
                packageName = "com.android.systemui.auto_generated_rro_product__",
                dump = dump
            )
        )
    }
}
