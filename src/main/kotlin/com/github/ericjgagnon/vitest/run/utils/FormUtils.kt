package com.github.ericjgagnon.vitest.run.utils

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.ui.SwingHelper
import javax.swing.ButtonGroup
import javax.swing.ButtonModel

object FormUtils {

    @JvmStatic
    fun Row.directoryField(
        project: Project,
        field: TextFieldWithBrowseButton,
        dialogTitle: String,
        fileChooser: FileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
    ): Cell<TextFieldWithBrowseButton> {
        SwingHelper.installFileCompletionAndBrowseDialog(
            project,
            field,
            dialogTitle,
            fileChooser
        )
        return cell(field).horizontalAlign(HorizontalAlign.FILL)
    }

    fun ButtonGroup.selectedValueMatches(predicate: (ButtonModel?) -> Boolean): ComponentPredicate {
        return ButtonGroupPredicate(this, predicate)
    }

    class ButtonGroupPredicate(private val buttonGroup: ButtonGroup, private val predicate: (ButtonModel?) -> Boolean) : ComponentPredicate() {
        override fun invoke(): Boolean = predicate(buttonGroup.selection)

        override fun addListener(listener: (Boolean) -> Unit) {
            for (button in buttonGroup.elements) {
                button.addChangeListener {
                    listener(predicate(buttonGroup.selection))
                }
            }
        }
    }
}