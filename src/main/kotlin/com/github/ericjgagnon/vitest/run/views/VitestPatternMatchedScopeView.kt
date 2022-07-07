package com.github.ericjgagnon.vitest.run.views

import com.github.ericjgagnon.vitest.run.VitestSettings
import com.github.ericjgagnon.vitest.run.utils.FormUtils.directoryField
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign

class VitestPatternMatchedScopeView(private val project: Project): VitestScopeView {

    private var testFileField = TextFieldWithBrowseButton()
    private var testPatternField: EnvironmentVariablesTextFieldWithBrowseButton =
        EnvironmentVariablesTextFieldWithBrowseButton()

    override fun setFromSettings(settings: VitestSettings) {
        settings.testPattern()?.let {
            testPatternField.text = it
        }

        settings.testFilePath()?.let {
            testFileField.text = it
        }
    }

    override fun updateSettings(settingsBuilder: VitestSettings.Builder) {
        settingsBuilder.testPattern(testPatternField.text)
        settingsBuilder.testFilePath(testFileField.text)
    }

    override fun Panel.render() {
        row("Test file:") {
            directoryField(project, testFileField, "Test file",
                FileChooserDescriptorFactory.createSingleFileDescriptor()
            )
        }
        row("Test pattern:") {
            cell(testPatternField).horizontalAlign(HorizontalAlign.FILL)
        }
    }
}