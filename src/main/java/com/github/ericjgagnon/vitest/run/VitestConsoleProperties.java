package com.github.ericjgagnon.vitest.run;

import com.intellij.execution.Executor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.testing.JsTestConsoleProperties;
import com.intellij.terminal.TerminalExecutionConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VitestConsoleProperties extends JsTestConsoleProperties {
    public static final String TEST_FRAMEWORK_NAME = "VitestJavaScriptTestRunner";
    private final SMTestLocator locator;
    private final boolean withTerminalConsole;


    public VitestConsoleProperties(@NotNull VitestRunConfiguration configuration, @NotNull Executor executor, @NotNull SMTestLocator locator, boolean withTerminalConsole, @Nullable NodeTargetRun targetRun) {
        super(configuration, TEST_FRAMEWORK_NAME, executor, targetRun);
        this.locator = locator;
        this.withTerminalConsole = withTerminalConsole;
        this.setUsePredefinedMessageFilter(false);
        this.setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false);
        this.setIfUndefined(TestConsoleProperties.HIDE_IGNORED_TEST, true);
        this.setIfUndefined(TestConsoleProperties.SCROLL_TO_SOURCE, true);
        this.setIfUndefined(TestConsoleProperties.SELECT_FIRST_DEFECT, true);
        this.setIdBasedTestTree(false);
        this.setPrintTestingStartedTime(false);
    }

    @Override
    public SMTestLocator getTestLocator() {
        return locator;
    }

    @NotNull
    public ConsoleView createConsole() {
        if (this.withTerminalConsole) {
            return new TerminalExecutionConsole(this.getProject(), null) {
                @Override
                public void attachToProcess(@NotNull ProcessHandler processHandler) {
                    super.attachToProcess(processHandler, false);
                }
            };
        } else {
            return super.createConsole();
        }
    }

    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties consoleProperties) {
        return super.createTestEventsConverter(testFrameworkName, consoleProperties);
    }

    @Override
    public @Nullable AbstractRerunFailedTestsAction createRerunFailedTestsAction(ConsoleView consoleView) {
        return new VitestRerunFailedTestAction((SMTRunnerConsoleView) consoleView, this);
    }
}
