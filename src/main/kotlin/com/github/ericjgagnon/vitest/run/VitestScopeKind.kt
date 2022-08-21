package com.github.ericjgagnon.vitest.run

enum class VitestScopeKind(val label: String) {
    ALL("All tests"),
    TEST_FILE("Test file"),
    TEST("Test"),
    SUITE("Suite");
}