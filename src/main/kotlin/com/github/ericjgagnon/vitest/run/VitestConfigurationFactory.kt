package com.github.ericjgagnon.vitest.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class VitestConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return VitestRunConfiguration(project, this, "Vitest")
    }

    override fun getId(): String {
        return "VitestTestRunner"
    }
}