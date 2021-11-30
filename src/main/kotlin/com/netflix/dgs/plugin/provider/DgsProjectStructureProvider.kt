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
import com.netflix.dgs.plugin.NamedNavigationComponent
import com.netflix.dgs.plugin.services.DgsService
import javax.swing.Icon

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

    override fun getChildren(): Collection<AbstractTreeNode<*>> {

        val service = project?.getService(DgsService::class.java)
        return if (service != null) {

            listOf(
                DgsNamedRootNode(project, settings, "Data fetchers", service.dgsComponentIndex.dataFetchers),
                DgsNamedRootNode(project, settings, "Entity fetchers", service.dgsComponentIndex.entityFetchers),
                DgsNamedRootNode(project, settings, "Data loaders", service.dgsComponentIndex.dataLoaders),
                DgsNamedRootNode(project, settings, "Directives", service.dgsComponentIndex.directives),
                DgsNamedRootNode(project, settings, "Runtime wiring", service.dgsComponentIndex.runtimeWirings),
                DgsNamedRootNode(project, settings, "Scalars", service.dgsComponentIndex.scalars),
                DgsNamedRootNode(project, settings, "Custom context", service.dgsComponentIndex.customContexts),
            )
        } else {
            emptyList()
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }
}

class DgsNamedRootNode(
    project: Project?,
    settings: ViewSettings?,
    private val text: String,
    private val components: Set<NamedNavigationComponent>,
    private val elementIcon: Icon = AllIcons.Nodes.Method
) : ProjectViewNode<String>(project, text, settings) {
    override fun update(presentation: PresentationData) {
        presentation.apply {
            val toolTip = children.mapNotNull { it.name }.joinToString(", ")
            val textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
            addText(ColoredFragment(text, toolTip, textAttributes))
            setIcon(DgsConstants.dgsIcon)
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        if (project != null) {
            return components.map { component ->
                DgsNode(project, settings, component.name, component.psiAnnotation, elementIcon)
            }.toMutableList()
        }

        return mutableListOf()
    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }
}

class DgsNode(
    project: Project?,
    settings: ViewSettings?,
    private val text: String,
    private val psiElement: PsiElement?,
    private val theIcon: Icon = AllIcons.Nodes.Method
) : ProjectViewNode<String>(project, text, settings) {
    override fun update(presentation: PresentationData) {
        presentation.apply {
            val toolTip = children.mapNotNull { it.name }.joinToString(", ")
            val textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
            addText(ColoredFragment(text, toolTip, textAttributes))
            setIcon(theIcon)
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        return mutableListOf()
    }

    override fun contains(file: VirtualFile): Boolean {
        return false
    }

    override fun navigate(requestFocus: Boolean) {
        if (project != null && psiElement != null) {

            val fileEditorManager = FileEditorManager.getInstance(project!!)
            fileEditorManager.openEditor(
                OpenFileDescriptor(
                    project!!,
                    psiElement.containingFile.virtualFile,
                    psiElement.textOffset
                ), true
            )
        }

    }

    override fun canNavigate(): Boolean {
        return psiElement != null
    }

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

    override fun canNavigateToSource(): Boolean {
        return true
    }
}