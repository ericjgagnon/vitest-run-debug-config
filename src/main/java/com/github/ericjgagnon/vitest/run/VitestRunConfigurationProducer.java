package com.github.ericjgagnon.vitest.run;

import com.google.common.collect.Lists;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.javascript.nodejs.PackageJsonData;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.javascript.nodejs.util.NodePackageDescriptor;
import com.intellij.javascript.testFramework.JsTestElementPath;
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructure;
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder;
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructure;
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder;
import com.intellij.javascript.testing.JSTestRunnerManager;
import com.intellij.javascript.testing.JsTestRunConfigurationProducer;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.javascript.buildTools.base.JsbtTaskFetchException;
import com.intellij.lang.javascript.buildTools.npm.NpmScriptsService;
import com.intellij.lang.javascript.buildTools.npm.NpmScriptsStructure;
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSTestFileType;
import com.intellij.lang.javascript.psi.util.JSProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.ericjgagnon.vitest.run.VitestConstants.NODE_PACKAGE_NAME;

public class VitestRunConfigurationProducer extends JsTestRunConfigurationProducer<VitestRunConfiguration> {

    private static final NodePackageDescriptor testRunnerPackageDescriptor = new NodePackageDescriptor(NODE_PACKAGE_NAME);

    protected VitestRunConfigurationProducer() {
        super(testRunnerPackageDescriptor, Collections.emptyList());
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
                        return Objects.equals(thisRunSettings.testFilePath(), thatRunSettings.testFilePath()) && Objects.equals(thisRunSettings.testNames(), thatRunSettings.testNames());
                    }
                }
            }
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
                    builder.setTestNames(Lists.newArrayList(testName));
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
                if (PackageJsonUtil.isPackageJsonFile(jsonFile)) {
                    JsonProperty testProp = PackageJsonUtil.findContainingTopLevelProperty(element);
                    if (testProp != null && NODE_PACKAGE_NAME.equals(testProp.getName())) {
                        return new VitestRunConfigurationProducer.TestElementInfo(this, settings.toBuilder().scope(VitestScopeKind.ALL).build(), testProp);
                    }
                }
            }

            return null;
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

    @Nullable
    private VirtualFile guessWorkingDir(@NotNull Project project, @NotNull VirtualFile contextFile) {
        Ref<VirtualFile> firstPackageJsonDir = Ref.create();
        AtomicBoolean basedOnJestDependencyDeclared = new AtomicBoolean(false);
        VirtualFile workingDir = JSProjectUtil.processDirectoriesUpToContentRootAndFindFirst(project, contextFile, (dir) -> {
            VirtualFile config = dir.findChild("jest.config.js");
            if (config != null && !config.isDirectory()) {
                return dir;
            } else {
                VirtualFile packageJson = PackageJsonUtil.findChildPackageJsonFile(dir);
                if (packageJson != null) {
                    firstPackageJsonDir.setIfNull(dir);
                    PackageJsonData data = PackageJsonData.getOrCreate(packageJson);

                    if (JSTestRunnerManager.isDeclaredInPackageJson(data, this)) {
                        basedOnJestDependencyDeclared.set(true);
                        return dir;
                    }

                    if (hasTestScript(project, packageJson, data)) {
                        return dir;
                    }
                }

                return null;
            }
        });
        return workingDir != null && !workingDir.equals(firstPackageJsonDir.get()) && basedOnJestDependencyDeclared.get() && workingDir.findChild("lerna.json") != null ? firstPackageJsonDir.get() : workingDir;
    }

    private static boolean hasTestScript(@NotNull Project project, @NotNull VirtualFile packageJson, @NotNull PackageJsonData data) {
        if (data.getTopLevelProperties().contains("scripts")) {
            try {
                NpmScriptsStructure structure = NpmScriptsService.getInstance(project).fetchBuildfileStructure(packageJson);
                return structure.getTaskNames().contains("test");
            } catch (JsbtTaskFetchException jsbtTaskFetchException) {
                return false;
            }
        }
        return false;
    }

    public static class TestElementInfo {
        private final VitestSettings settings;
        private final PsiElement element;

        public TestElementInfo(VitestRunConfigurationProducer configurationProducer, VitestSettings settings, PsiElement element) {
            this.element = element;

            VitestSettings.Builder settingsBuilder = settings.toBuilder();
            NodePackage vitestPackage = settings.vittestPackage();
            if (settings.vittestPackage() == null) {
                Project project = element.getProject();
                VirtualFile contextFile = Objects.requireNonNull(PsiUtilCore.getVirtualFile(element));
                NodeJsInterpreterRef interpreter = Optional.ofNullable(settings.interpreter()).orElseGet(NodeJsInterpreterRef::createProjectRef);
                vitestPackage = VitestRunConfigurationProducer.testRunnerPackageDescriptor.findFirstDirectDependencyPackage(project, interpreter.resolve(project), contextFile);
            }

            if (vitestPackage != null) {
                settingsBuilder.vitestPackage(vitestPackage);
            }

            String workingDirectoryPath = settings.workingDirectory();
            if (StringUtils.isEmpty(settings.workingDirectory())) {
                VirtualFile contextFile = Objects.requireNonNull(PsiUtilCore.getVirtualFile(element));
                VirtualFile workingDirectory = configurationProducer.guessWorkingDir(element.getProject(), contextFile);
                if (workingDirectory != null) {
                    workingDirectoryPath = FileUtil.toSystemDependentName(workingDirectory.getPath());
                }
            }

            if (workingDirectoryPath != null) {
                settingsBuilder.workingDirectory(workingDirectoryPath);
            }

            this.settings = settingsBuilder.build();
        }

        public VitestSettings getSettings() {
            return settings;
        }

        public PsiElement getEnclosingTestElement() {
            return element;
        }
    }

}
