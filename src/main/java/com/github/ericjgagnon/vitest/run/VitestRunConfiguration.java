package com.github.ericjgagnon.vitest.run;

import com.github.ericjgagnon.vitest.run.VitestSettings.Builder;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption;
import com.intellij.javascript.nodejs.NodeCommandLineUtil;
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.javascript.testFramework.PreferableRunConfiguration;
import com.intellij.javascript.testFramework.navigation.JSTestLocationProvider;
import com.intellij.javascript.testFramework.util.JsTestFqn;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class VitestRunConfiguration extends AbstractNodeTargetRunProfile implements NodeDebugRunConfiguration, PreferableRunConfiguration, JSRunProfileWithCompileBeforeLaunchOption, SMRunnerConsolePropertiesProvider {

    private VitestSettings settings;

    public VitestRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
    }

    @NotNull
    public VitestSettings getSettings() {
        return this.settings;
    }

    @Nullable
    public NodeJsInterpreter getInterpreter() {
        return Optional.ofNullable(this.settings).map(VitestSettings::interpreter).map(nodeJsInterpreterRef -> nodeJsInterpreterRef.resolve(this.getProject())).orElse(null);
    }

    @NotNull
    public VitestRunConfigurationEditor getConfigurationEditor() {
        return new VitestRunConfigurationEditor(this.getProject());
    }

    @Nullable
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new VitestRunProfileState(this, environment);
    }

    public boolean isPreferredOver(@NotNull RunConfiguration runConfiguration, @NotNull PsiElement psiElement) {
        CharSequence contents = psiElement.getContainingFile().getViewProvider().getContents();
        if (!StringUtil.contains(contents, "vitest")) {
            return false;
        } else {
            return StringUtil.contains(contents, "'vitest'") || StringUtil.contains(contents, "\"vitest\"");
        }
    }

    @Nullable
    public String suggestedName() {
        VitestScopeKind scopeKind = settings.scope();
        if (scopeKind == VitestScopeKind.TEST_FILE) {
            return PathUtil.getFileName(settings.testFilePath());
        } else if (scopeKind != VitestScopeKind.SUITE && scopeKind != VitestScopeKind.TEST) {
            return StringUtil.isNotEmpty(settings.vitestConfigFilePath()) ? PathUtil.getFileName(settings.vitestConfigFilePath()) : "All Tests";
        } else {
            return JsTestFqn.getPresentableName(settings.testNames());
        }
    }

    @Nullable
    public String getActionName() {
        VitestScopeKind scopeKind = settings.scope();
        return scopeKind != VitestScopeKind.SUITE && scopeKind != VitestScopeKind.TEST ? super.getActionName() : StringUtil.notNullize(ContainerUtil.getLastItem(settings.testNames()));
    }

    public void setRunSettings(@NotNull Builder vitestSettings) {
        this.settings = vitestSettings.build();
    }

    public void setRunSettings(@NotNull VitestSettings vitestSettings) {
        this.settings = vitestSettings;
    }

    @Override
    public @NotNull SMTRunnerConsoleProperties createTestConsoleProperties(@NotNull Executor executor) {
        return createTestConsoleProperties(executor, false, null);
    }

    public @NotNull VitestConsoleProperties createTestConsoleProperties(@NotNull Executor executor, boolean withTerminalConsole, @Nullable NodeTargetRun nodeTargetRun) {
        return new VitestConsoleProperties(this, executor, new JSTestLocationProvider(true), withTerminalConsole, nodeTargetRun);
    }

    @Override
    public boolean hasConfiguredDebugAddress() {
        return NodeCommandLineUtil.findDebugPort(this.settings.nodeOptions()) != -1;
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        String interpreterRef = JDOMExternalizerUtil.readCustomField(element, "node-interpreter");
        String nodeOptions = JDOMExternalizerUtil.readCustomField(element, "node-options");
        String vitestPackage = JDOMExternalizerUtil.readCustomField(element, "vitest-package");
        String vitestConfig = JDOMExternalizerUtil.readCustomField(element, "vitest-config");
        String workingDir = JDOMExternalizerUtil.readCustomField(element, "working-dir");
        VitestSettings.Builder settingsBuilder = new VitestSettings.Builder();

        Optional.ofNullable(interpreterRef)
                .filter(StringUtils::isNotEmpty)
                .map(NodeJsInterpreterRef::create)
                .ifPresentOrElse(settingsBuilder::interpreter, () -> {
                    settingsBuilder.interpreter(NodeJsInterpreterRef.createProjectRef());
                });

        Optional.ofNullable(nodeOptions)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::setNodeOptions);

        Optional.ofNullable(vitestPackage)
                .filter(StringUtils::isNotEmpty)
                .map(NodePackage::new)
                .ifPresent(settingsBuilder::vitestPackage);

        Optional.ofNullable(vitestConfig)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::vitestConfigFilePath);

        Optional.ofNullable(workingDir)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::workingDirectory);

        settings = settingsBuilder.build();
    }

    @Override
    public void writeExternal(@NotNull Element element) {

        Optional.ofNullable(settings)
                .map(VitestSettings::interpreter)
                .map(NodeJsInterpreterRef::getReferenceName)
                .ifPresent(ref -> {
                    JDOMExternalizerUtil.writeCustomField(element, "node-interpreter", ref);
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::nodeOptions)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(options -> {
                    JDOMExternalizerUtil.writeCustomField(element, "node-options", options);
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::vittestPackage)
                .ifPresent(testPackage -> {
                    JDOMExternalizerUtil.writeCustomField(element, "vitest-package", testPackage.getSystemIndependentPath());
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::vitestConfigFilePath)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(configPath -> {
                    JDOMExternalizerUtil.writeCustomField(element, "vitest-config", FileUtil.toSystemIndependentName(configPath));
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::workingDirectory)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(workingDir -> {
                    JDOMExternalizerUtil.writeCustomField(element, "working-dir", FileUtil.toSystemIndependentName(workingDir));
                });
        super.writeExternal(element);
    }
}
