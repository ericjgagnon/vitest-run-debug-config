package com.github.ericjgagnon.vitest.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.javascript.jest.JestUtil;
import com.intellij.javascript.nodejs.util.NodePackageDescriptor;
import com.intellij.javascript.testFramework.JsTestElementPath;
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructure;
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder;
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructure;
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder;
import com.intellij.javascript.testing.JsTestRunConfigurationProducer;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSTestFileType;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class VitestRunConfigurationProducer extends JsTestRunConfigurationProducer<VitestRunConfiguration> {
    protected VitestRunConfigurationProducer() {
        super(new NodePackageDescriptor("vitest"), Collections.emptyList());
    }

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return VitestConfigurationType.getInstance();
    }

    @Override
    protected boolean setupConfigurationFromCompatibleContext(@NotNull VitestRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        PsiElement element = context.getPsiLocation();
        if (element != null && this.isTestRunnerPackageAvailableFor(element, context)) {
            VitestRunConfigurationProducer.TestElementInfo elementRunInfo = this.createTestElementRunInfo(element, configuration.getSettings());
            if (elementRunInfo == null) {
                return false;
            } else {
                VitestSettings runSettings = elementRunInfo.getSettings();
                configuration.setRunSettings(runSettings);
                sourceElement.set(elementRunInfo.getEnclosingTestElement());
                configuration.setGeneratedName();
                return true;
            }
        } else {
            return false;
        }
    }

    private TestElementInfo createTestElementRunInfo(PsiElement element, VitestSettings settings) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (virtualFile == null) {
            return null;
        } else {
            JsTestElementPath testElementPath = createSuiteOrTestData(element);
            if (testElementPath == null) {
                return this.createFileInfo(element, virtualFile, settings);
            } else {
                VitestSettings.Builder builder = settings.toBuilder();
                builder.setTestFilePath(virtualFile.getPath());
                String testName = testElementPath.getTestName();
                String suiteName = testElementPath.getSuiteNames().stream().findFirst().orElse(null);
                if (suiteName != null) {
                    builder.scope(VitestScopeKind.SUITE);
                    builder.setSuiteName(suiteName);
                }

                if (testName != null){
                    builder.scope(VitestScopeKind.TEST);
                    builder.setTestName(testName);
                }

                return new VitestRunConfigurationProducer.TestElementInfo(this, builder.build(), testElementPath.getTestElement());
            }
        }
    }

    private TestElementInfo createFileInfo(PsiElement element, VirtualFile virtualFile, VitestSettings settings) {
        JSFile psiFile = ObjectUtils.tryCast(element.getContainingFile(), JSFile.class);
        JSTestFileType testFileType = psiFile == null ? null : psiFile.getTestFileType();
        if (psiFile != null && testFileType == JSTestFileType.JASMINE) {
            VitestSettings.Builder builder = settings.toBuilder();
            builder.scope(VitestScopeKind.TEST_FILE);
            builder.setTestFilePath(virtualFile.getPath());
            return new VitestRunConfigurationProducer.TestElementInfo(this, builder.build(), psiFile);
        } else {
            JsonFile jsonFile = ObjectUtils.tryCast(element.getContainingFile(), JsonFile.class);
            if (jsonFile != null) {
                if (JestUtil.isJestConfigFile(jsonFile.getName())) {
                    VitestSettings.Builder builder = settings.toBuilder();
                    builder.vitestConfigFilePath(virtualFile.getPath());
                    builder.scope(VitestScopeKind.ALL);
                    return new VitestRunConfigurationProducer.TestElementInfo(this, builder.build(), jsonFile);
                }

                if (PackageJsonUtil.isPackageJsonFile(jsonFile)) {
                    JsonProperty testProp = PackageJsonUtil.findContainingTopLevelProperty(element);
                    if (testProp != null && "vitest".equals(testProp.getName())) {
                        return new VitestRunConfigurationProducer.TestElementInfo(this, settings.toBuilder().scope(VitestScopeKind.ALL).build(), testProp);
                    }
                }
            }

            return null;
        }
    }

    @Override
    protected boolean isConfigurationFromCompatibleContext(@NotNull VitestRunConfiguration configuration, @NotNull ConfigurationContext context) {
        PsiElement element = context.getPsiLocation();
        if (element == null) {
            return false;
        } else {
            VitestRunConfigurationProducer.TestElementInfo elementRunInfo = this.createTestElementRunInfo(element, configuration.getSettings());
            if (elementRunInfo == null) {
                return false;
            } else {
                VitestSettings thisRunSettings = elementRunInfo.getSettings();
                VitestSettings thatRunSettings = configuration.getSettings();
                if (!Objects.equals(thisRunSettings.vitestConfigFilePath(), thatRunSettings.vitestConfigFilePath())) {
                    return false;
                } else if (thisRunSettings.scope() != thatRunSettings.scope()) {
                    return false;
                } else {
                    VitestScopeKind scopeKind = thisRunSettings.scope();
                    if (scopeKind == VitestScopeKind.ALL) {
                        return true;
                    } else if (scopeKind == VitestScopeKind.TEST_FILE) {
                        return Objects.equals(thisRunSettings.testFilePath(), thatRunSettings.testFilePath());
                    } else if (scopeKind != VitestScopeKind.SUITE && scopeKind != VitestScopeKind.TEST) {
                        return false;
                    } else {
                        return Objects.equals(thisRunSettings.testFilePath(), thatRunSettings.testFilePath()) && Objects.equals(thisRunSettings.testName(), thatRunSettings.testName());
                    }
                }
            }
        }
    }

    @Nullable
    private static JsTestElementPath createSuiteOrTestData(@NotNull PsiElement element) {

        if (element instanceof PsiFileSystemItem) {
            return null;
        } else {
            JSFile jsFile = ObjectUtils.tryCast(element.getContainingFile(), JSFile.class);
            TextRange textRange = element.getTextRange();
            if (jsFile == null || textRange == null) {
                return null;
            } else {
                JasmineFileStructure jasmineStructure = JasmineFileStructureBuilder.getInstance().fetchCachedTestFileStructure(jsFile);
                JsTestElementPath path = jasmineStructure.findTestElementPath(textRange);
                if (path == null) {
                    MochaTddFileStructure mochaStructure = MochaTddFileStructureBuilder.getInstance().fetchCachedTestFileStructure(jsFile);
                    return mochaStructure.findTestElementPath(textRange);
                } else {
                    return path;
                }
            }
        }
    }

    public static class TestElementInfo {
        private final VitestRunConfigurationProducer vitestRunConfigurationProducer;
        private final VitestSettings settings;
        private final PsiElement element;

        public TestElementInfo(VitestRunConfigurationProducer vitestRunConfigurationProducer, VitestSettings settings, PsiElement element) {
            this.vitestRunConfigurationProducer = vitestRunConfigurationProducer;
            this.settings = settings;
            this.element = element;
        }

        public VitestSettings getSettings() {
            return settings;
        }

        public PsiElement getEnclosingTestElement() {
            return element;
        }
    }

}
