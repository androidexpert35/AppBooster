package com.tony.appbooster

import com.android.ddmlib.AndroidDebugBridge
import dadb.Dadb
import org.junit.Test

class DdmlibExploration {
    @Test
    fun explore() {
        println("=== Exploring ddmlib ===")
        try {
            val adbClass = AndroidDebugBridge::class.java
            println("\n--- AndroidDebugBridge methods ---")
            adbClass.methods.sortedBy { it.name }.forEach {
                println("  ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}): ${it.returnType.simpleName}")
            }

            // Look for AdbHelper
            try {
                val helperClass = Class.forName("com.android.ddmlib.AdbHelper")
                println("\n--- AdbHelper methods ---")
                helperClass.methods.sortedBy { it.name }.forEach {
                    println("  ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}): ${it.returnType.simpleName}")
                }
            } catch (e: Exception) {
                println("AdbHelper not found: ${e.message}")
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun exploreDadb() {
        println("=== Exploring dadb library ===")
        try {
            val dadbClass = Dadb::class.java
            println("\n--- Dadb methods ---")
            dadbClass.methods.sortedBy { it.name }.forEach {
                println("  ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}): ${it.returnType.simpleName}")
            }

            // Check for AdbKeyPair
            val keyPairClass = dadb.AdbKeyPair::class.java
            println("\n--- AdbKeyPair methods ---")
            keyPairClass.methods.sortedBy { it.name }.forEach {
                println("  ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}): ${it.returnType.simpleName}")
            }

            // Check companion object for static methods
            println("\n--- Dadb.Companion methods ---")
            Dadb::class.java.declaredClasses.forEach { innerClass ->
                println("Inner class: ${innerClass.name}")
                innerClass.methods.forEach {
                    println("  ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}): ${it.returnType.simpleName}")
                }
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun findPairingSupport() {
        println("=== Searching for pairing-related classes ===")

        // Check dadb package for pairing
        val classesToCheck = listOf(
            "dadb.Dadb",
            "dadb.AdbPairingClient",
            "dadb.AdbConnection",
            "dadb.AdbStream",
            "dadb.AdbKeyPair",
            "dadb.AdbServer",
            "dadb.AdbPairing",
            // ddmlib classes
            "com.android.ddmlib.AdbHelper",
            "com.android.ddmlib.PairingHandler",
            "com.android.ddmlib.WifiPairing"
        )

        classesToCheck.forEach { className ->
            try {
                val clazz = Class.forName(className)
                println("\n✓ Found: $className")
                clazz.declaredMethods.forEach { method ->
                    println("    ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
                }
            } catch (e: ClassNotFoundException) {
                println("✗ Not found: $className")
            }
        }
    }

    @Test
    fun exploreAllDadbClasses() {
        println("=== Deep dive into dadb library ===")

        // All known dadb classes to explore
        val dadbClasses = listOf(
            "dadb.Dadb",
            "dadb.Dadb\$Companion",
            "dadb.DadbImpl",
            "dadb.AdbKeyPair",
            "dadb.AdbKeyPair\$Companion",
            "dadb.AdbConnection",
            "dadb.AdbStream",
            "dadb.AdbMessage",
            "dadb.AdbSync",
            "dadb.AdbReader",
            "dadb.AdbWriter",
            "dadb.AdbShellResponse",
            "dadb.AdbShellPacket",
            // Pairing related
            "dadb.adbserver.AdbServer",
            "dadb.adbserver.AdbServerPairing",
            // Internal
            "dadb.internal.AdbConnectionFactory"
        )

        dadbClasses.forEach { className ->
            try {
                val clazz = Class.forName(className)
                println("\n✓ $className")
                clazz.declaredMethods.filter { !it.name.startsWith("access$") }.forEach { method ->
                    println("    ${method.name}(${method.parameterTypes.joinToString { it.simpleName }}): ${method.returnType.simpleName}")
                }
                clazz.declaredConstructors.forEach { ctor ->
                    println("    [ctor](${ctor.parameterTypes.joinToString { it.simpleName }})")
                }
            } catch (e: ClassNotFoundException) {
                // Skip silently
            }
        }
    }

    @Test
    fun checkAdbServerDiscovery() {
        println("=== Checking Dadb.discover() and AdbServer ===")

        // Check what discover() method signatures exist
        val dadbCompanion = Dadb::class.java
        println("\nDadb class methods:")
        dadbCompanion.methods
            .filter { it.name == "discover" || it.name == "create" || it.name == "pair" }
            .forEach { method ->
                println("  ${method.name}(${method.parameterTypes.map { it.simpleName }}): ${method.returnType.simpleName}")
            }

        // Check Kotlin extension functions by looking at Dadb$DefaultImpls
        try {
            val defaultImpls = Class.forName("dadb.Dadb\$DefaultImpls")
            println("\nDadb\$DefaultImpls methods:")
            defaultImpls.methods.forEach { method ->
                println("  ${method.name}(${method.parameterTypes.map { it.simpleName }})")
            }
        } catch (e: Exception) {
            println("DefaultImpls not found")
        }
    }

    @Test
    fun exploreAdbWireless() {
        println("=== Looking for ADB wireless pairing support ===")

        // Check if there's any pairing-related code
        val packagesToScan = listOf(
            "dadb",
            "com.android.ddmlib"
        )

        // Look for classes with "pair" in name
        val classLoader = Thread.currentThread().contextClassLoader

        // Try specific class names that might handle pairing
        val pairingClasses = listOf(
            // ddmlib pairing (Android Studio's implementation)
            "com.android.ddmlib.AdbDevice",
            "com.android.ddmlib.DeviceMonitor",
            "com.android.ddmlib.IDevice",
            "com.android.ddmlib.internal.DeviceImpl",
            "com.android.ddmlib.AdbInitOptions",
            "com.android.ddmlib.clientmanager.DeviceClientManager"
        )

        pairingClasses.forEach { className ->
            try {
                val clazz = Class.forName(className)
                println("\n✓ Found: $className")
                // Look for connect/pair methods
                clazz.declaredMethods
                    .filter {
                        it.name.contains("pair", ignoreCase = true) ||
                                it.name.contains("connect", ignoreCase = true) ||
                                it.name.contains("wireless", ignoreCase = true)
                    }
                    .forEach { method ->
                        println("  >> ${method.name}(${method.parameterTypes.map { it.simpleName }})")
                    }
            } catch (e: ClassNotFoundException) {
                // Skip
            }
        }

        // The key insight: Look at how apps like Wear Installer work
        println("\n=== HOW WEAR INSTALLER APPS WORK ===")
        println("""
            Apps like Wear Installer / Easy Fire Tools typically:
            
            1. **Pre-Android 11 (legacy TCP/IP mode)**:
               - Device must be on same WiFi
               - adb tcpip 5555 enables it (requires USB once)
               - Then adb connect <ip>:5555 works WITHOUT pairing
               
            2. **Android 11+ Wireless Debugging** (requires pairing):
               - User enables Wireless Debugging in Dev Options
               - User must "Pair device with pairing code" 
               - Pairing uses PAKE protocol (SPAKE2+) - complex crypto
               - After pairing, connect works
               
            3. **Workarounds these apps use**:
               a) Guide user to pair via PC's adb first
               b) Use Android's built-in ADB pairing UI (if accessible)
               c) Some root-based solutions bypass this
               d) Some use proprietary implementations of PAKE
               
            The dadb library DOES have pairing support but only for adb server mode.
        """.trimIndent())
    }
}
