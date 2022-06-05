package com.github.ericjgagnon.vitest.run;

import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

public class VitestConfigurationType extends SimpleConfigurationType implements DumbAware {
    protected VitestConfigurationType() {
        super("JavaScriptTestRunnerVitest", "Vitest", null, NotNullLazyValue.createValue(() -> {
            return AllIcons.FileTypes.JavaScript;
        }));
    }

    @Override
    public @NotNull VitestRunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new VitestRunConfiguration(project, this, "Vitest");
    }

    @NotNull
    public static VitestConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(VitestConfigurationType.class);
    }

    @Override
    public @NotNull RunConfigurationSingletonPolicy getSingletonPolicy() {
        return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY;
    }
}
