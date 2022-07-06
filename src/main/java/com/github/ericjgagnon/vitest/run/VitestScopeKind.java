package com.github.ericjgagnon.vitest.run;

public enum VitestScopeKind {
    ALL("All tests"),
    TEST_FILE("Test file"),
    TEST("Test"),
    SUITE("Suite");

    private final String label;

    VitestScopeKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
