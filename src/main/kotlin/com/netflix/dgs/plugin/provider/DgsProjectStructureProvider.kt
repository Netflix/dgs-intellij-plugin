/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.dgs.plugin.provider

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.SimpleTextAttributes
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.services.DgsService

class DgsProjectStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        if (parent.parent == null) {
            children.add(DgsComponentsRootNode(parent.project, settings))
        }

        return children
    }
}

class DgsComponentsRootNode(
    project: Project?,
    settings: ViewSettings?,
) : ProjectViewNode<String>(project, "DGS", settings) {

    override fun update(presentation: PresentationData) {
        presentation.apply {
            val text = "DGS Components"
            val toolTip = children.mapNotNull { it.name }.joinToString(", ")
            val textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
            addText(ColoredFragment(text, toolTip, textAttributes))
            setIcon(DgsConstants.dgsIcon)
        }
    }

    override fun getChildren(): Collection<out AbstractTreeNode<*>> {

        return listOf(DataFetcherRootNode(project, settings))

    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }
}

class DataFetcherRootNode(
    project: Project?,
    settings: ViewSettings?,
) : ProjectViewNode<String>(project, "DGS", settings) {
    override fun update(presentation: PresentationData) {
        presentation.apply {
            val text = "Data fetchers"
            val toolTip = children.mapNotNull { it.name }.joinToString(", ")
            val textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
            addText(ColoredFragment(text, toolTip, textAttributes))
            setIcon(DgsConstants.dgsIcon)
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {

        if(project != null) {
            val dgsService = project!!.getService(DgsService::class.java)
            return dgsService.dgsComponentIndex.dataFetchers.map { dataFetcher ->
                DataFetcherNode(project, settings, dataFetcher.parentType, dataFetcher.field, dataFetcher.psiMethod)
            }.toMutableList()
        }

        return mutableListOf()
    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }
}

class DataFetcherNode(
    project: Project?,
    settings: ViewSettings?,
    private val parentType: String,
    private val field: String,
    private val psiElement: PsiElement?
) : ProjectViewNode<String>(project, "DGS", settings) {
    override fun update(presentation: PresentationData) {
        presentation.apply {
            val text = "${parentType}.${field}"
            val toolTip = children.mapNotNull { it.name }.joinToString(", ")
            val textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
            addText(ColoredFragment(text, toolTip, textAttributes))
            setIcon(AllIcons.Nodes.Method)
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        return mutableListOf()
    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }

    override fun navigate(requestFocus: Boolean) {
        if(project != null && psiElement != null) {

            val fileEditorManager = FileEditorManager.getInstance(project!!)
            fileEditorManager.openEditor(OpenFileDescriptor(project!!, psiElement.containingFile.virtualFile, psiElement.textOffset), true)
        }

    }

    override fun canNavigate(): Boolean {
        return psiElement != null
    }
}

