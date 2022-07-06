package com.github.ericjgagnon.vitest.run.views

import com.github.ericjgagnon.vitest.run.VitestSettings

interface VitestScopeView {

    fun setFromSettings(settings: VitestSettings)
    fun updateSettings(settingsBuilder: VitestSettings.Builder)
    fun com.intellij.ui.dsl.builder.Panel.render()
}