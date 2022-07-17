package com.github.ericjgagnon.vitest.run

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.collections.CollectionUtils

class VitestSettings private constructor(
    private val interpreter: NodeJsInterpreterRef?,
    private val nodeOptions: String?,
    private val vitestPackage: NodePackage?,
    private val vitestConfigFilePath: String?,
    private val workingDirectory: String?,
    private val testNames: List<String>?,
    private val testFilePath: String?,
    private val suiteName: String?,
    private val scope: VitestScopeKind,
    ) {

    fun interpreter(): NodeJsInterpreterRef {
        return interpreter!!
    }

    fun nodeOptions(): String? {
        return nodeOptions
    }

    fun vittestPackage(): NodePackage? {
        return vitestPackage!!
    }

    fun vitestConfigFilePath(): String? {
        return vitestConfigFilePath
    }

    fun workingDirectory(): String? {
        return workingDirectory
    }

    fun testNames(): List<String>? {
        return testNames
    }

    fun testFilePath(): String? {
        return testFilePath
    }

    fun suiteName(): String? {
        return suiteName
    }

    fun scope(): VitestScopeKind {
        return scope
    }

    fun toBuilder(): Builder {
        return Builder(interpreter, nodeOptions, vitestPackage, vitestConfigFilePath, workingDirectory, testNames, testFilePath, suiteName, scope)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VitestSettings

        if (interpreter != other.interpreter) return false
        if (nodeOptions != other.nodeOptions) return false
        if (vitestPackage != other.vitestPackage) return false
        if (vitestConfigFilePath != other.vitestConfigFilePath) return false
        if (workingDirectory != other.workingDirectory) return false
        if (!CollectionUtils.isEqualCollection(testNames, other.testNames)) return false
        if (testFilePath != other.testFilePath) return false
        if (suiteName != other.suiteName) return false
        if (scope != other.scope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interpreter?.hashCode() ?: 0
        result = 31 * result + (nodeOptions?.hashCode() ?: 0)
        result = 31 * result + (vitestPackage?.hashCode() ?: 0)
        result = 31 * result + (vitestConfigFilePath?.hashCode() ?: 0)
        result = 31 * result + (workingDirectory?.hashCode() ?: 0)
        result = 31 * result + (testNames?.hashCode() ?: 0)
        result = 31 * result + (testFilePath?.hashCode() ?: 0)
        result = 31 * result + (suiteName?.hashCode() ?: 0)
        result = 31 * result + scope.hashCode()
        return result
    }


    data class Builder(
        var interpreter: NodeJsInterpreterRef? = null,
        var nodeOptions: String? = null,
        var vitestPackage: NodePackage? = null,
        var vitestConfigFilePath: String? = null,
        var workingDirectory: String? = null,
        var testNames: List<String>? = null,
        var testFilePath: String? = null,
        var suiteName: String?  = null,
        var scope: VitestScopeKind = VitestScopeKind.ALL,
    ) {

        fun interpreter(interpreter: NodeJsInterpreterRef) = apply { this.interpreter = interpreter }
        fun nodeOptions(nodeOptions: String?) = apply { this.nodeOptions = nodeOptions }
        fun vitestPackage(vitestPackage: NodePackage) = apply { this.vitestPackage = vitestPackage }
        fun vitestConfigFilePath(vitestConfigFilePath: String) = apply { this.vitestConfigFilePath = vitestConfigFilePath }
        fun workingDirectory(workingDirectory: String) = apply { this.workingDirectory = workingDirectory }
        fun scope(scope: VitestScopeKind) = apply { this.scope = scope }
        fun testNames(testNames: List<String>) = apply { this.testNames = testNames }
        fun testFilePath(testFilePath: String) = apply { this.testFilePath = FileUtil.toSystemDependentName(testFilePath) }
        fun suiteName(suiteName: String) = apply { this.suiteName = suiteName }
        fun build(): VitestSettings {
            return VitestSettings(
                interpreter,
                nodeOptions,
                vitestPackage,
                vitestConfigFilePath,
                workingDirectory,
                testNames,
                testFilePath,
                suiteName,
                scope
            )
        }
    }
}
