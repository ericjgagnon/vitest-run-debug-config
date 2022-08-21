package com.github.ericjgagnon.vitest.run

import com.github.ericjgagnon.vitest.run.VitestConstants.REPORTER_JS_FILE_NAME
import com.github.ericjgagnon.vitest.run.VitestConstants.TEST_FRAMEWORK_NAME
import com.github.ericjgagnon.vitest.run.utils.withNotNullNorEmpty
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeModuleDirectorySearchProcessor
import com.intellij.javascript.nodejs.NodeModuleSearchUtil
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeCommandLineOwner
import com.intellij.javascript.nodejs.execution.NodeBaseRunProfileState
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.ConsoleCommandLineFolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import java.io.File
import java.nio.file.Files

class VitestRunProfileState(
    private val vitestRunConfiguration: VitestRunConfiguration,
    private val environment: ExecutionEnvironment,
) : NodeBaseRunProfileState, NodeCommandLineOwner {

    private val reporter by lazy {
        Files.createTempFile(REPORTER_JS_FILE_NAME, ".js").toFile().apply {
            val os = if (SystemInfo.isWindows) "windows" else "nix"
            writeBytes(VitestRunProfileState::class.java.getResourceAsStream("/vitest-intellij-plugin/dist/reporter.$os.js")!!.readBytes())
            deleteOnExit()
        }
    }

    private val folder: ConsoleCommandLineFolder = ConsoleCommandLineFolder("")

    private val project: Project = vitestRunConfiguration.project

    private val settings: VitestSettings = vitestRunConfiguration.settings

    override fun createExecutionResult(processHandler: ProcessHandler): ExecutionResult {
        val consoleProperties = vitestRunConfiguration.createTestConsoleProperties(environment.executor,
            NodeCommandLineUtil.shouldUseTerminalConsole(processHandler),
            NodeTargetRun.getTargetRun(processHandler))
        val consoleView = SMTestRunnerConnectionUtil.createConsole(TEST_FRAMEWORK_NAME, consoleProperties)
        val workingDirectory = settings.workingDirectory()
        consoleProperties.addStackTraceFilter(NodeStackTraceFilter(project, workingDirectory, consoleProperties.targetRun))
        val stackTraceFilters: Iterator<Filter> = consoleProperties.stackTrackFilters.iterator()

        while (stackTraceFilters.hasNext()) {
            val filter = stackTraceFilters.next()
            consoleView.addMessageFilter(filter)
        }

        consoleView.addMessageFilter(NodeConsoleAdditionalFilter(project, workingDirectory))

        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)
        folder.foldCommandLine(consoleView, processHandler)
        val executionResult = DefaultExecutionResult(consoleView, processHandler)
        executionResult.setRestartActions(consoleProperties.createRerunFailedTestsAction(consoleView))
        return executionResult
    }

    override fun startProcess(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val nodeJsInterpreterRef = settings.interpreter() ?: NodeJsInterpreterRef.createProjectRef()
        val nodeInterpreter: NodeJsInterpreter = nodeJsInterpreterRef.resolveNotNull(project)
        val nodeTargetRun = NodeTargetRun(nodeInterpreter, project, configurator, NodeTargetRun.createOptionsForTestConsole(
            listOf(), true, vitestRunConfiguration
        ))

        val commandLine = nodeTargetRun.commandLineBuilder

        val workingDir = settings.workingDirectory()
        workingDir?.let{
            commandLine.setWorkingDirectory(it)
        }

        val vitestPackage = settings.vittestPackage()
        vitestPackage?.let {
            val bin = File(it.systemDependentPath, "dist/cli.mjs")
            commandLine.addParameter(nodeTargetRun.path(bin.absolutePath))
            commandLine.addParameter("run")
            folder.addPlaceholderText(it.name)
            folder.addPlaceholderText("run")
        }

        NodeCommandLineUtil.prependNodeDirToPATH(nodeTargetRun)
        val vitestConfigFilePath = settings.vitestConfigFilePath()
        vitestConfigFilePath?.let {
            commandLine.addParameter("--config")
            commandLine.addParameter(nodeTargetRun.path(FileUtil.toSystemDependentName(it)))
            folder.addPlaceholderTexts(listOf("--config=" + PathUtil.getFileName(it)))
        }

        val reporterFile = vitestRunConfiguration.getVitestReporterFile()
        reporterFile.let {
            commandLine.addParameter("--reporter")
            commandLine.addParameter(it)
            folder.addPlaceholderText("--reporter")
            folder.addPlaceholderText(it)
        }

        val testFilePath = settings.testFilePath()
        testFilePath?.let {
            commandLine.addParameter(it)
            folder.addPlaceholderText(it)
        }

        val testNames = settings.testNames()
        testNames.withNotNullNorEmpty {
            val testNamesPiped = this.joinToString("|");
            commandLine.addParameter("--testNamePattern=$testNamesPiped")
            folder.addPlaceholderText("--testNamePattern=$testNamesPiped")
        }

        commandLine.addParameter(" --passWithNoTests")
        folder.addPlaceholderText(" --passWithNoTests")

        return nodeTargetRun.startProcess()
    }

    private fun VitestRunConfiguration.getVitestReporterFile(): String {
        val file = LocalFileSystem.getInstance().findFileByPath(settings.workingDirectory()!!)
        file?.let {
            val info = NodeModuleSearchUtil.resolveModuleFromNodeModulesDir(
                it,
                REPORTER_JS_FILE_NAME,
                NodeModuleDirectorySearchProcessor.PROCESSOR
            )
            if (info != null && info.moduleSourceRoot.isDirectory) {
                return NodePackage(info.moduleSourceRoot.path).systemIndependentPath
            }
        }

        return reporter.absolutePath
    }
}
