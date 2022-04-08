package com.github.ericjgagnon.vitest.run

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.annotations.NonNls

class VitestSettings private constructor(
    private val interpreter: NodeJsInterpreterRef?,
    private val nodeOptions: String?,
    private val vitestPackage: NodePackage?,
    private val vitestConfigFilePath: String?,
    private val workingDirectory: String?,
    private val testNames: MutableList<String>,
    private val testFilePath: @NonNls String,
    private val scope: VitestScopeKind?,
    ) {

    fun interpreter(): NodeJsInterpreterRef {
        return interpreter!!
    }

    fun nodeOptions(): String? {
        return nodeOptions
    }

    fun vittestPackage(): NodePackage? {
        return vitestPackage
    }

    fun vitestConfigFilePath(): String? {
        return vitestConfigFilePath
    }

    fun workingDirectory(): String? {
        return workingDirectory
    }

    fun testNames(): List<String> {
        return testNames
    }

    fun testFilePath(): String {
        return testFilePath
    }

    fun scope(): VitestScopeKind? {
        return scope
    }

    fun toBuilder(): Builder {
        return Builder(interpreter, nodeOptions, vitestPackage, vitestConfigFilePath, workingDirectory, testNames, testFilePath, scope)
    }

    data class Builder(
        var interpreter: NodeJsInterpreterRef? = null,
        var nodeOptions: String? = null,
        var vitestPackage: NodePackage? = null,
        var vitestConfigFilePath: String? = null,
        var workingDirectory: String? = null,
        var testNames: MutableList<String> = ArrayList(),
        var testFilePath: @NonNls String = "",
        var scope: VitestScopeKind? = null,
    ) {

        fun interpreter(interpreter: NodeJsInterpreterRef) = apply { this.interpreter = interpreter }
        fun nodeOptions(nodeOptions: String?) = apply { this.nodeOptions = nodeOptions }
        fun vitestPackage(vitestPackage: NodePackage) = apply { this.vitestPackage = vitestPackage }
        fun vitestConfigFilePath(vitestConfigFilePath: String) = apply { this.vitestConfigFilePath = vitestConfigFilePath }
        fun workingDirectory(workingDirectory: String) = apply { this.workingDirectory = workingDirectory }
        fun setScopeKind(scope: VitestScopeKind) = apply { this.scope = scope }
        fun build() = VitestSettings(
            interpreter,
            nodeOptions,
            vitestPackage,
            vitestConfigFilePath,
            workingDirectory,
            testNames,
            FileUtil.toSystemDependentName(testFilePath),
            scope
        )
    }
}
