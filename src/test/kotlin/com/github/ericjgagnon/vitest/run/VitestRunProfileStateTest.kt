package com.github.ericjgagnon.vitest.run

import com.google.common.collect.Lists
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.Key
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

const val TEST_WORKING_DIR = "vitest-intellij-plugin"
const val TEST_FILENAME = "single-testcase.test.js"
const val TEST_PATH = "$TEST_WORKING_DIR/src/Simple/__test__/$TEST_FILENAME"
const val VITE_PACKAGE = "$TEST_WORKING_DIR/node_modules/vitest"

class VitestRunProfileStateTest: BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    fun testCreateExecutionResult() {
        val expectation = "##teamcity[testingStarted]\n" +
                "##teamcity[testStarted id='1766321171_0' name='2 === 2' nodeId='1766321171_0']\n" +
                "##teamcity[testFinished id='1766321171_0' name='2 === 2' nodeId='1766321171_0' duration='1']\n" +
                "##teamcity[testingFinished]\n"

        val config = VitestConfigurationType.getInstance().createTemplateConfiguration(project)
        val settings = VitestSettings.Builder(
            NodeJsInterpreterRef.createProjectRef(),
            null,
            NodePackage("${myFixture.testDataPath}/$VITE_PACKAGE"),
            "vite.config.js",
            "${myFixture.testDataPath}/$TEST_WORKING_DIR",
            Lists.newArrayList("single-testcase.test.js"),
            "${myFixture.testDataPath}/$TEST_PATH",
        )
        config.setRunSettings(settings.build())
        val actual = StringBuilder()
        PlatformTestUtil.executeConfiguration(config, DefaultRunExecutor.EXECUTOR_ID) { runContentDescriptor ->
            runContentDescriptor.processHandler?.addProcessListener(object: ProcessListener {
                override fun startNotified(event: ProcessEvent) {
                }

                override fun processTerminated(event: ProcessEvent) {
                    assertTrue(actual.contains(expectation))
                }

                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    if (event.text.isNotEmpty()) {
                        actual.append(event.text)
                    }
                }
            })
        }

        // Release all editors to prevent failure
        EditorFactory.getInstance().allEditors.forEach { editor -> EditorFactory.getInstance().releaseEditor(editor) }
    }
}