package com.github.ericjgagnon.vitest.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
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
import com.intellij.javascript.testFramework.navigation.JSTestLocationProvider;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class VitestRunConfiguration extends AbstractNodeTargetRunProfile implements NodeDebugRunConfiguration, JSRunProfileWithCompileBeforeLaunchOption, SMRunnerConsolePropertiesProvider {

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

    public @NotNull RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new VitestRunProfileState(this, environment);
    }

    public String suggestedName() {
        VitestScopeKind scopeKind = settings.scope();
        if (scopeKind == VitestScopeKind.ALL && StringUtils.isNotBlank(settings.testPattern())) {
            return "Tests matching " + settings.testPattern();
        } else if (scopeKind == VitestScopeKind.TEST_FILE) {
            return PathUtil.getFileName(settings.testFilePath());
        } else if (scopeKind != VitestScopeKind.SUITE && scopeKind != VitestScopeKind.TEST) {
            return StringUtil.isNotEmpty(settings.testFilePath()) ? PathUtil.getFileName(settings.testFilePath()) : "All Tests";
        } else {
            return settings.testName();
        }
    }

    @Nullable
    public String getActionName() {
        VitestScopeKind scopeKind = settings.scope();
        if (scopeKind != VitestScopeKind.SUITE && scopeKind != VitestScopeKind.TEST) {
            return super.getActionName();
        }
        return StringUtil.notNullize(settings.testName());
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
        String vitestScope = JDOMExternalizerUtil.readCustomField(element, "test-scope");
        String suiteName = JDOMExternalizerUtil.readCustomField(element, "suite-name");
        String testFilePath = JDOMExternalizerUtil.readCustomField(element, "test-file-path");
        String testName = JDOMExternalizerUtil.readCustomField(element, "test-name");
        String testPattern = JDOMExternalizerUtil.readCustomField(element, "test-pattern");
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

        Optional.ofNullable(suiteName)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::suiteName);

        Optional.ofNullable(testFilePath)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::testFilePath);

        Optional.ofNullable(testName)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::testName);

        Optional.ofNullable(testPattern)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(settingsBuilder::testPattern);

        Optional.ofNullable(vitestScope)
                .filter(StringUtils::isNotEmpty)
                .map(VitestScopeKind::valueOf)
                .ifPresent(settingsBuilder::scope);

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

        Optional.ofNullable(settings)
                .map(VitestSettings::suiteName)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(suiteName -> {
                    JDOMExternalizerUtil.writeCustomField(element, "suite-name", suiteName);
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::testFilePath)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(testFilePath -> {
                    JDOMExternalizerUtil.writeCustomField(element, "test-file-path", FileUtil.toSystemIndependentName(testFilePath));
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::testName)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(testFileName -> {
                    JDOMExternalizerUtil.writeCustomField(element, "test-name", testFileName);
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::testPattern)
                .filter(StringUtils::isNotEmpty)
                .ifPresent(testPattern -> {
                    JDOMExternalizerUtil.writeCustomField(element, "test-pattern", testPattern);
                });

        Optional.ofNullable(settings)
                .map(VitestSettings::scope)
                .ifPresentOrElse(scope -> {
                    JDOMExternalizerUtil.writeCustomField(element, "test-scope", scope.name());
                }, () -> JDOMExternalizerUtil.writeCustomField(element, "test-scope", VitestScopeKind.ALL.name()));

        super.writeExternal(element);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends AbstractNodeTargetRunProfile> createConfigurationEditor() {
        return new VitestRunConfigurationEditor(this.getProject());
    }
}
