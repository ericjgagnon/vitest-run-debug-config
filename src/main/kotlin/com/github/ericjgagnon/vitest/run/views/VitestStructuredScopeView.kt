package com.github.ericjgagnon.vitest.run.views

import com.github.ericjgagnon.vitest.run.VitestScopeKind
import com.github.ericjgagnon.vitest.run.VitestSettings
import com.github.ericjgagnon.vitest.run.utils.FormUtils.fileSystemCell
import com.github.ericjgagnon.vitest.run.utils.FormUtils.selectedValueMatches
import com.google.common.base.Splitter
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.util.containers.reverse
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.util.Optional
import javax.swing.ButtonGroup
import javax.swing.ButtonModel
import javax.swing.JRadioButton

private var TEST_FILE_SCOPES = setOf(VitestScopeKind.TEST_FILE, VitestScopeKind.SUITE, VitestScopeKind.TEST)
private var SUITE_SCOPES = setOf(VitestScopeKind.SUITE)
private var TEST_NAME_SCOPES = setOf(VitestScopeKind.TEST)

class VitestStructuredScopeView(private val project: Project): VitestScopeView {

    private var testFileField = TextFieldWithBrowseButton()
    private var suiteNameField = JBTextField()
    private var testNameField = JBTextField()
    private var vitestScopeKindField = ButtonGroup()

    private var scopedButtonModels = mutableMapOf<VitestScopeKind, ButtonModel>()
    private var buttonModelScopes: Map<ButtonModel, VitestScopeKind>
    private val testFileScopesPredicate: ComponentPredicate
    private val testSuiteScopesPredicate: ComponentPredicate
    private val testNameScopesPredicate: ComponentPredicate


    init {
//        testNameField.addActionListener {
//            val currentSelections = testNameField.text
//            val testNameSelectionDialog = TestNameSelectionDialog(project, testFileField.text)
//            if (testNameSelectionDialog.showAndGet()) {
//                testNameField.text = testNameSelectionDialog.selections()
//            }
//        }
        for (scope in VitestScopeKind.values()) {
            val testScopeButton = JRadioButton(scope.label)
            vitestScopeKindField.add(testScopeButton)
            scopedButtonModels[scope] = testScopeButton.model
        }
        buttonModelScopes = scopedButtonModels.reverse()
        testFileScopesPredicate = vitestScopeKindField.selectedValueMatches {
            TEST_FILE_SCOPES.contains(buttonModelScopes[it])
        }
        testSuiteScopesPredicate = vitestScopeKindField.selectedValueMatches {
            SUITE_SCOPES.contains(buttonModelScopes[it])
        }
        testNameScopesPredicate = vitestScopeKindField.selectedValueMatches {
            TEST_NAME_SCOPES.contains(buttonModelScopes[it])
        }
    }

    override fun setFromSettings(settings: VitestSettings) {
        Optional.ofNullable(settings.testFilePath()).ifPresent {
            testFileField.text = it
            Optional.ofNullable(settings.testNames())
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent { testNames ->
                    testNameField.text = testNames.joinToString("|")
                }
        }
        Optional.ofNullable(settings.suiteName()).ifPresent(suiteNameField::setText)

        vitestScopeKindField.clearSelection()
        Optional.ofNullable(settings.scope()).ifPresentOrElse(
            {
                vitestScopeKindField.setSelected(scopedButtonModels[it], true)
            },
            {
                vitestScopeKindField.setSelected(scopedButtonModels[VitestScopeKind.ALL], true)
            }
        )
    }

    override fun updateSettings(settingsBuilder: VitestSettings.Builder) {
        val testScope = buttonModelScopes.getOrDefault(vitestScopeKindField.selection, VitestScopeKind.ALL)

        val suiteName = suiteNameField.text
        val testFilePath = testFileField.text
        val testNames = Splitter.on("|").omitEmptyStrings().splitToList(testNameField.text)

        settingsBuilder.suiteName(suiteName)
        settingsBuilder.testFilePath(testFilePath)
        settingsBuilder.testNames(testNames)

        settingsBuilder.scope(testScope)
    }

    override fun com.intellij.ui.dsl.builder.Panel.render() {
        group {
            row {
                for (button in vitestScopeKindField.elements) {
                    panel {
                        row {
                            cell(button)
                        }
                    }.gap(RightGap.COLUMNS)
                }
            }

            row("Test file:") {
                fileSystemCell(project, testFileField, "Test file", FileChooserDescriptorFactory.createSingleFileDescriptor())
            }.visibleIf(testFileScopesPredicate)
            row("Suite name:") {
                cell(suiteNameField).horizontalAlign(HorizontalAlign.FILL)
            }.visibleIf(testSuiteScopesPredicate)
            row("Test names:") {
                cell(testNameField).horizontalAlign(HorizontalAlign.FILL)
            }.visibleIf(testNameScopesPredicate.and(testFileField.childComponent.enteredTextSatisfies { filePath ->
                StringUtils.isNotBlank(filePath) && File(filePath).exists()
            }))
        }
    }
}