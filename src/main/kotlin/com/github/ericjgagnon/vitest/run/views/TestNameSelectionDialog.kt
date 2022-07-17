package com.github.ericjgagnon.vitest.run.views

import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.TreeTraversal
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Dimension
import java.util.StringJoiner
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class TestNameSelectionDialog(val project: Project, private val filePath: String?): DialogWrapper(project) {

    private val testNamesField = TextFieldWithBrowseButton()
    private val testTree: CheckboxTree
    private val rootNode: CheckedTreeNode = CheckedTreeNode("")
    private val selectedTests = StringJoiner("|", "'", "'")

    init {
        testTree = CheckboxTree(MyCheckboxTreeCellRenderer(), rootNode)
        rootNode.removeAllChildren()
        testNamesInSelectedFile(rootNode)
        testTree.isRootVisible = false
        testTree.showsRootHandles = false
        TreeUtil.installActions(testTree)
        TreeUtil.expandAll(testTree)
        TreeUtil.promiseSelectFirst(testTree)
        testTree.minimumSize = Dimension(25, -1);
        title = "Select Tests to Run"

        super.init()

        testNamesField.addActionListener {
            show()
        }
    }

    fun selections(): String {
        return selectedTests.toString()
    }

    //    val dialogPanel = DialogPanel("Select Tests To Run")
    override fun createCenterPanel(): JComponent {
        return ScrollPaneFactory.createScrollPane(testTree)
    }

    override fun doOKAction() {
        TreeUtil.treeNodeTraverser(rootNode).traverse(TreeTraversal.LEAVES_DFS).processEach { node ->
            if (node is CheckedTreeNode && node.isChecked) {
                selectedTests.add(node.userObject as String)
            }
            true
        }
        testNamesField.text = selectedTests.toString()
        super.doOKAction()
    }

    private fun testNamesInSelectedFile(rootNode: DefaultMutableTreeNode) {
        VirtualFileManager.getInstance().findFileByUrl("file://$filePath")?.let { virtualFile ->
            PsiManager.getInstance(project).findFile(virtualFile)?.let { psiFile ->
                ObjectUtils.tryCast(psiFile, JSFile::class.java)?.let { jsFile ->
                    var latestSuiteNode: DefaultMutableTreeNode? = null
                    val fetchCachedTestFileStructure =
                        JasmineFileStructureBuilder.getInstance().fetchCachedTestFileStructure(jsFile)
                    fetchCachedTestFileStructure.forEach({
                            suite ->
                        val suiteNode = DefaultMutableTreeNode(suite.name)
                        suite.specs.forEach {
                            val testNode = CheckedTreeNode(it.name)
                            testNode.isChecked = false
                            suiteNode.add(testNode)
                        }
                        latestSuiteNode?.let {
                            suiteNode.add(latestSuiteNode)
                        }
                        latestSuiteNode = suiteNode
                    }, null)
                    latestSuiteNode?.also {
                        rootNode.add(latestSuiteNode)
                    }
                }

            }
        }
    }

    class MyCheckboxTreeCellRenderer: CheckboxTree.CheckboxTreeCellRenderer(false) {
        override fun customizeRenderer(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            if (value is DefaultMutableTreeNode) {
                val userObject = value.userObject
                if (userObject is String) {
                    textRenderer.append(userObject)
                }
            }
        }
    }
}