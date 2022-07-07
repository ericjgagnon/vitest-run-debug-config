package com.github.ericjgagnon.vitest.run

import com.github.ericjgagnon.vitest.run.utils.FormUtils.directoryField
import com.github.ericjgagnon.vitest.run.views.VitestPatternMatchedScopeView
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.util.Optional
import javax.swing.JComponent

class VitestRunConfigurationEditor(
    private val project: Project,
) : SettingsEditor<VitestRunConfiguration>() {

    private var nodeInterpreterField: NodeJsInterpreterField = NodeJsInterpreterField(project, false)
    private var nodeOptionsField: EnvironmentVariablesTextFieldWithBrowseButton =
        EnvironmentVariablesTextFieldWithBrowseButton()
    private var vitestJsPackageField: NodePackageField = NodePackageField(nodeInterpreterField, "vitest")
    private var viteConfigFilePathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
    private var workingDirectoryField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
    private var vitestScopeView = VitestPatternMatchedScopeView(project)

    private lateinit var editor: DialogPanel

    override fun resetEditorFrom(runConfiguration: VitestRunConfiguration) {
        val settings = runConfiguration.settings
        nodeInterpreterField.interpreterRef = settings.interpreter()
        Optional.ofNullable(settings.nodeOptions()).ifPresent(nodeOptionsField::setText)
        Optional.ofNullable(settings.vittestPackage()).ifPresent(vitestJsPackageField::setSelected)
        Optional.ofNullable(settings.vitestConfigFilePath()).ifPresent(viteConfigFilePathField::setText)
        Optional.ofNullable(settings.workingDirectory()).ifPresent(workingDirectoryField::setText)
        vitestScopeView.setFromSettings(settings)
    }

    override fun applyEditorTo(runConfiguration: VitestRunConfiguration) {
        val vitestSettingsBuilder: VitestSettings.Builder = runConfiguration.settings.toBuilder()
        vitestSettingsBuilder.interpreter(nodeInterpreterField.interpreterRef)
        vitestSettingsBuilder.nodeOptions(nodeOptionsField.text.ifBlank { null })
        vitestSettingsBuilder.vitestPackage(vitestJsPackageField.selected)
        vitestSettingsBuilder.vitestConfigFilePath(viteConfigFilePathField.text)
        vitestSettingsBuilder.workingDirectory(workingDirectoryField.text)
        vitestScopeView.updateSettings(vitestSettingsBuilder)

        val vitestSettings = vitestSettingsBuilder.build()
        if (vitestSettings != runConfiguration.settings) {
            runConfiguration.setRunSettings(vitestSettings)
        }
    }


    override fun createEditor(): JComponent {
        editor = panel {
            row("Node interpreter:") {
                cell(nodeInterpreterField).horizontalAlign(HorizontalAlign.FILL)
            }
            row("Node options:") {
                cell(nodeOptionsField).horizontalAlign(HorizontalAlign.FILL)
            }
            row("Vitest package:") {
                cell(vitestJsPackageField).horizontalAlign(HorizontalAlign.FILL)
            }
            row("Working directory:") { directoryField(project, workingDirectoryField, "Working Directory") }
            row("Configuration file:") {
                directoryField(project, viteConfigFilePathField, "Configuration File",
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withFileFilter { file -> file.name == "vite.config.js" })
            }
            with(vitestScopeView) {
                render()
            }
        }

        return editor
    }
}
