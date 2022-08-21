package com.github.ericjgagnon.vitest.run

import com.github.ericjgagnon.vitest.run.VitestConstants.CONFIG_TYPE_NAME
import com.github.ericjgagnon.vitest.run.VitestConstants.TEST_FRAMEWORK_NAME
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.Icons

class VitestConfigurationType: SimpleConfigurationType(TEST_FRAMEWORK_NAME,
    CONFIG_TYPE_NAME,
    null,
    NotNullLazyValue.createValue { Icons.VITEST_ICON }), DumbAware {
    override fun createTemplateConfiguration(project: Project): VitestRunConfiguration {
        return VitestRunConfiguration(project, this, CONFIG_TYPE_NAME)
    }

    override fun getSingletonPolicy(): RunConfigurationSingletonPolicy {
        return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY
    }

    companion object {
        @JvmStatic
        val instance: VitestConfigurationType
            get() = findConfigurationType(VitestConfigurationType::class.java)
    }
}