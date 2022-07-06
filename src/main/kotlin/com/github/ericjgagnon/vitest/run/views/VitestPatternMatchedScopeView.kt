package com.github.ericjgagnon.vitest.run.views

import com.github.ericjgagnon.vitest.run.VitestScopeKind
import com.github.ericjgagnon.vitest.run.VitestSettings
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.awt.TextField

class VitestPatternMatchedScopeView: VitestScopeView {

    private var testPatternField: EnvironmentVariablesTextFieldWithBrowseButton =
        EnvironmentVariablesTextFieldWithBrowseButton()
    private var testScopeField = TextField()

    override fun setFromSettings(settings: VitestSettings) {
        testPatternField.text = settings.testPattern()!!
        testScopeField.text = settings.scope().name
        testScopeField.isVisible = false
    }

    override fun updateSettings(settingsBuilder: VitestSettings.Builder) {
        settingsBuilder.testPattern(testPatternField.text)
        settingsBuilder.scope(VitestScopeKind.ALL)
    }

    override fun Panel.render() {
        row("Test pattern:") {
            cell(testPatternField).horizontalAlign(HorizontalAlign.FILL)
        }
    }


}