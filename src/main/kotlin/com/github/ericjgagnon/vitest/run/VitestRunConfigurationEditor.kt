package com.github.ericjgagnon.vitest.run

import com.github.ericjgagnon.vitest.run.VitestConstants.CONFIG_FILE_NAMES
import com.github.ericjgagnon.vitest.run.utils.FormUtils.fileSystemCell
import com.github.ericjgagnon.vitest.run.views.VitestStructuredScopeView
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
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
    private var vitestScopeView = VitestStructuredScopeView(project)

    private lateinit var editor: DialogPanel

    override fun resetEditorFrom(runConfiguration: VitestRunConfiguration) {
        val settings = runConfiguration.settings
        nodeInterpreterField.interpreterRef = settings.interpreter() ?: NodeJsInterpreterRef.createProjectRef()
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
            row("Working directory:") { fileSystemCell(project, workingDirectoryField, "Working Directory") }
            row("Configuration file:") {
                fileSystemCell(project, viteConfigFilePathField, "Configuration File",
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withFileFilter { file ->
                        CONFIG_FILE_NAMES.contains(file.name)
                    }
                )
            }
            with(vitestScopeView) {
                render()
            }
        }

        return editor
    }
}
