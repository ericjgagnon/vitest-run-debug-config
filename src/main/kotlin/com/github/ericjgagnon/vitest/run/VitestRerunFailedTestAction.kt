package com.github.ericjgagnon.vitest.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView

class VitestRerunFailedTestAction(consoleView: SMTRunnerConsoleView, consoleProperties: VitestConsoleProperties): AbstractRerunFailedTestsAction(consoleView) {

    init {
        this.myConsoleProperties = consoleProperties
        this.model = consoleView.resultsViewer
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
        val runConfiguration = myConsoleProperties.configuration as VitestRunConfiguration
        val failedTests = getFailedTests(runConfiguration.project)
        val failedTestNames = failedTests.filter { it.isLeaf && it.name != null }.map { it.name }

        val testSettingsBuilder = runConfiguration.settings.toBuilder()
        testSettingsBuilder.testNames(failedTestNames)
        runConfiguration.setRunSettings(testSettingsBuilder.build())

        val rerunState = VitestRunProfileState(runConfiguration, environment)

        return object: MyRunProfile(runConfiguration) {
            override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
                return rerunState
            }
        };
    }
}