package com.github.ericjgagnon.vitest.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
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
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.ConsoleCommandLineFolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files

private const val reporterPackage = "cypress-vitest-reporter"

@Suppress("JVM_DEFAULT_THROUGH_INHERITANCE")
class VitestRunProfileState(
    private val vitestRunConfiguration: VitestRunConfiguration,
    private val environment: ExecutionEnvironment,
) : NodeBaseRunProfileState, NodeCommandLineOwner {

    private val reporter by lazy {
        Files.createTempFile("intellij-vitest-reporter", ".js").toFile().apply {
            writeBytes(VitestRunProfileState::class.java.getResourceAsStream("/vitest-intellij-plugin/dist/reporter.js")!!.readBytes())
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
        val consoleView = SMTestRunnerConnectionUtil.createConsole("VitestJavaScriptTestRunner", consoleProperties)
        val workingDirectory = settings.workingDirectory()
        consoleProperties.addStackTraceFilter(NodeStackTraceFilter(project, workingDirectory, consoleProperties.targetRun))
        val stackTraceFilters: Iterator<Filter> = consoleProperties.stackTrackFilters.iterator()

        while (stackTraceFilters.hasNext()) {
            val filter = stackTraceFilters.next()
            consoleView.addMessageFilter(filter)
        }

        consoleView.addMessageFilter(NodeConsoleAdditionalFilter(project, workingDirectory))
        val testConsole = consoleView as SMTRunnerConsoleView
        val consoleImpl = testConsole.console as ConsoleViewImpl
        val editor = consoleImpl.editor
        val keyCodeMapping: Map<Int, Int> =
            ContainerUtil.newHashMap(Pair.create(40, 1792834),
                Pair.create(37, 1792836),
                Pair.create(39, 1792835),
                Pair.create(38, 1792833),
                Pair.create(8, if (SystemInfo.isWindows) 8 else 127),
                Pair.create(10, 13),
                Pair.create(27, 27))

        editor?.contentComponent?.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(event: KeyEvent) {
                if (event.keyChar.code != 0) {
                    sendCode(event.keyChar.code)
                }
            }

            override fun keyReleased(event: KeyEvent) {
                val newKeyCode = keyCodeMapping[event.keyCode]
                if (newKeyCode != null) {
                    sendCode(newKeyCode)
                }
            }

            private fun sendCode(keyCode: Int) {
                if (!processHandler.isProcessTerminated) {
                    val input: OutputStream? = processHandler.processInput
                    if (input != null) {
                        try {
                            input.write(keyCode)
                            input.flush()
                        } catch (exception: IOException) {
                            throw RuntimeException("Failed to handle input with keyCode $keyCode", exception)
                        }
                    }
                }
            }
        })

        ProcessTerminatedListener.attach(processHandler)
        consoleView.attachToProcess(processHandler)
        folder.foldCommandLine(consoleView, processHandler)
        return DefaultExecutionResult(consoleView, processHandler)
    }

    override fun startProcess(configurator: CommandLineDebugConfigurator?): ProcessHandler {
        val nodeJsInterpreterRef = settings.interpreter()
        val nodeInterpreter: NodeJsInterpreter = nodeJsInterpreterRef.resolveNotNull(project)
        val nodeTargetRun = NodeTargetRun(nodeInterpreter, project, configurator, NodeTargetRun.createOptionsForTestConsole(
            listOf(), true, vitestRunConfiguration
        ))

        val commandLine = nodeTargetRun.commandLineBuilder

        val workingDir = settings.workingDirectory()
        if (workingDir != null) {
            commandLine.setWorkingDirectory(workingDir)
        }

        val vitestPackage = settings.vittestPackage()
        if (vitestPackage != null) {
            val bin = File(vitestPackage.systemDependentPath, "dist/cli")
            commandLine.addParameter(nodeTargetRun.path(bin.absolutePath))
            commandLine.addParameter("run")
            folder.addPlaceholderText(vitestPackage.name)
            folder.addPlaceholderText("run")
        }

        NodeCommandLineUtil.prependNodeDirToPATH(nodeTargetRun)
        commandLine.addParameter("--config")
        val vitestConfigFilePath = settings.vitestConfigFilePath()
        if (vitestConfigFilePath != null) {
            commandLine.addParameter(nodeTargetRun.path(FileUtil.toSystemDependentName(vitestConfigFilePath)))
            folder.addPlaceholderTexts(listOf("--config=" + PathUtil.getFileName(vitestConfigFilePath)))
        }

        val reporterFile = vitestRunConfiguration.getVitestReporterFile()
        reporterFile.let {
            commandLine.addParameter("--reporter")
            commandLine.addParameter(it)
            folder.addPlaceholderText("--reporter")
            folder.addPlaceholderText(it)
        }


        val testFilePath = settings.testFilePath()
        if (testFilePath != null) {
            commandLine.addParameter(testFilePath)
            folder.addPlaceholderText(testFilePath)
        }

        val testPattern = settings.testPattern()
        if (testPattern != null) {
            commandLine.addParameters("-t", testPattern)
            folder.addPlaceholderTexts("-t", testPattern)
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
                reporterPackage,
                NodeModuleDirectorySearchProcessor.PROCESSOR
            )
            if (info != null && info.moduleSourceRoot.isDirectory) {
                return NodePackage(info.moduleSourceRoot.path).systemIndependentPath
            }
        }

        return reporter.absolutePath
    }
}
