package com.github.ericjgagnon.vitest.run;

import com.intellij.execution.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VitestFilter implements Filter {
    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
        return null;
    }
}
