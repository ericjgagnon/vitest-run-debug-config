package com.github.ericjgagnon.vitest.run

object VitestConstants {
    const val CONFIG_TYPE_NAME = "Vitest"
    const val TEST_FRAMEWORK_NAME = "VitestJavaScriptTestRunner"
    const val NODE_PACKAGE_NAME = "vitest"
    val CONFIG_FILE_NAMES = setOf(
        "vite.config.ts",
        "vite.config.mjs",
        "vite.config.js",
        "vite.config.cjs",
        "vite.config.mts",
        "vite.config.cts",
        "vitest.config.ts",
        "vitest.config.mjs",
        "vitest.config.js",
        "vitest.config.cjs",
        "vitest.config.mts",
        "vitest.config.cts"
    )
    const val REPORTER_JS_FILE_NAME = "intellij-vitest-reporter"
}