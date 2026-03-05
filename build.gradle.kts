// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

// ---------------------------------------------------------------------------
// Aggregate test tasks
// ---------------------------------------------------------------------------

/**
 * Runs all JVM unit tests across every module.
 *
 * Usage:
 *   ./gradlew runUnitTests
 *
 * Equivalent to :app:testDebugUnitTest.
 * HTML report → app/build/reports/tests/testDebugUnitTest/index.html
 */
tasks.register("runUnitTests") {
    group = "verification"
    description = "Runs all JVM unit tests in every module (debug variant)."
    dependsOn(":app:testDebugUnitTest")

    doLast {
        println("\n✅  Unit tests finished.")
        println("    Report → ${rootProject.projectDir}/app/build/reports/tests/testDebugUnitTest/index.html")
    }
}

/**
 * Runs all instrumented (on-device / emulator) tests across every module.
 *
 * Usage:
 *   ./gradlew runInstrumentedTests
 *
 * Requires a connected device or running emulator.
 * HTML report → app/build/reports/androidTests/connected/debug/index.html
 */
tasks.register("runInstrumentedTests") {
    group = "verification"
    description = "Runs all instrumented tests on a connected device or emulator."
    dependsOn(":app:connectedDebugAndroidTest")

    doLast {
        println("\n✅  Instrumented tests finished.")
        println("    Report → ${rootProject.projectDir}/app/build/reports/androidTests/connected/debug/index.html")
    }
}

/**
 * Runs the complete test suite: unit tests first, then instrumented tests.
 *
 * Usage:
 *   ./gradlew runAllTests
 *
 * For unit tests only (no device required):
 *   ./gradlew runUnitTests
 */
tasks.register("runAllTests") {
    group = "verification"
    description = "Runs all unit tests and all instrumented tests."
    dependsOn("runUnitTests", "runInstrumentedTests")

    // Unit tests always run before instrumented so fast failures surface early.
    tasks.findByName("runInstrumentedTests")
        ?.mustRunAfter("runUnitTests")

    doLast {
        println("\n\uD83C\uDF89  Full test suite finished.")
        println("    Unit test report         → ${rootProject.projectDir}/app/build/reports/tests/testDebugUnitTest/index.html")
        println("    Instrumented test report → ${rootProject.projectDir}/app/build/reports/androidTests/connected/debug/index.html")
    }
}
