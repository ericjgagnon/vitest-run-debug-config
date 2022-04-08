package com.github.ericjgagnon.vitest.run

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.SwingHelper
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField

class VitestRunConfigurationEditor(private val project: Project) : SettingsEditor<VitestRunConfiguration>() {

    private var configurationFileField: TextFieldWithBrowseButton
    private var nodeInterpreterField: NodeJsInterpreterField
    private var nodeOptionsField: EnvironmentVariablesTextFieldWithBrowseButton
    private var vitestJsPackageField: NodePackageField
    private var viteConfigFilePathField: TextFieldWithBrowseButton
    private var workingDirectoryField: TextFieldWithBrowseButton

    init {
        val textField = JTextField();
        configurationFileField = TextFieldWithBrowseButton(textField)
        nodeInterpreterField = NodeJsInterpreterField(project, false)
        nodeOptionsField = EnvironmentVariablesTextFieldWithBrowseButton()
        vitestJsPackageField = NodePackageField(nodeInterpreterField, "vitest")
        viteConfigFilePathField = TextFieldWithBrowseButton()
        workingDirectoryField = TextFieldWithBrowseButton()
    }


    override fun resetEditorFrom(runConfiguration: VitestRunConfiguration) {
        val settings = runConfiguration.settings
        nodeInterpreterField.interpreterRef = settings.interpreter()
        Optional.ofNullable(settings.nodeOptions()).ifPresent(nodeOptionsField::setText)
        Optional.ofNullable(settings.vittestPackage()).ifPresent(vitestJsPackageField::setSelected)
        Optional.ofNullable(settings.vitestConfigFilePath()).ifPresent(viteConfigFilePathField::setText)
        Optional.ofNullable(settings.workingDirectory()).ifPresent(workingDirectoryField::setText)
    }

    override fun applyEditorTo(runConfiguration: VitestRunConfiguration) {
        val vitestSettings: VitestSettings.Builder = runConfiguration.settings.toBuilder()
        vitestSettings.interpreter(interpreter = nodeInterpreterField.interpreterRef)
        vitestSettings.nodeOptions(nodeOptions = nodeOptionsField.text)
        vitestSettings.vitestPackage(vitestPackage = vitestJsPackageField.selected)
        vitestSettings.vitestConfigFilePath(vitestConfigFilePath = viteConfigFilePathField.text)
        vitestSettings.workingDirectory(workingDirectory = workingDirectoryField.text)
        runConfiguration.setRunSettings(vitestSettings)
    }

    override fun createEditor(): JComponent {
        fun Row.nodeInterpreterField(): Cell<NodeJsInterpreterField> {
            return cell(nodeInterpreterField).horizontalAlign(HorizontalAlign.FILL)
        }

        fun Row.nodeOptionsField(): Cell<EnvironmentVariablesTextFieldWithBrowseButton> {
            return cell(nodeOptionsField).horizontalAlign(HorizontalAlign.FILL)
        }

        fun Row.viteConfigurationFileField(): Cell<TextFieldWithBrowseButton> {
            SwingHelper.installFileCompletionAndBrowseDialog(
                project,
                viteConfigFilePathField,
                "Configuration File:",
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                    .withFileFilter { file -> file.name == "vite.config.js" })
            return cell(viteConfigFilePathField).horizontalAlign(HorizontalAlign.FILL)
        }

        fun Row.workingDirectoryField(): Cell<TextFieldWithBrowseButton> {
            SwingHelper.installFileCompletionAndBrowseDialog(
                project,
                workingDirectoryField,
                "Working directory:",
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
            )
            return cell(workingDirectoryField).horizontalAlign(HorizontalAlign.FILL)
        }

        return panel {
            row("Node interpreter:") {
                nodeInterpreterField()
            }
            row("Node options:") { nodeOptionsField() }
            row("Vitest package:") {
                cell(vitestJsPackageField).horizontalAlign(HorizontalAlign.FILL)
            }
            row("Working directory: ") { workingDirectoryField() }
            row("Configuration file:") {
                viteConfigurationFileField()
            }
        }
    }
}