package com.github.ericjgagnon.vitest.run

import com.github.ericjgagnon.vitest.run.VitestConstants.TEST_FRAMEWORK_NAME
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.testing.JsTestConsoleProperties
import com.intellij.terminal.TerminalExecutionConsole

class VitestConsoleProperties(
    configuration: VitestRunConfiguration,
    executor: Executor,
    private val locator: SMTestLocator,
    private val withTerminalConsole: Boolean,
    targetRun: NodeTargetRun?
) : JsTestConsoleProperties(configuration, TEST_FRAMEWORK_NAME, executor, targetRun) {
    init {
        this.isUsePredefinedMessageFilter = false
        setIfUndefined(HIDE_PASSED_TESTS, false)
        setIfUndefined(HIDE_IGNORED_TEST, true)
        setIfUndefined(SCROLL_TO_SOURCE, true)
        setIfUndefined(SELECT_FIRST_DEFECT, true)
        this.isIdBasedTestTree = false
        this.isPrintTestingStartedTime = false
    }

    override fun getTestLocator(): SMTestLocator? {
        return locator
    }

    override fun createConsole(): ConsoleView {
        return if (withTerminalConsole) {
            object : TerminalExecutionConsole(this.project, null) {
                override fun attachToProcess(processHandler: ProcessHandler) {
                    super.attachToProcess(processHandler, false)
                }
            }
        } else {
            super.createConsole()
        }
    }

    override fun createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction? {
        return VitestRerunFailedTestAction((consoleView as SMTRunnerConsoleView), this)
    }
}