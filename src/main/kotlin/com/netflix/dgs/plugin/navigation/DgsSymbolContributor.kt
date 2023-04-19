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

package com.netflix.dgs.plugin.navigation

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.services.DgsService
import javax.swing.Icon

class DgsSymbolContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val dgsService = project.getService(DgsService::class.java)
        return dgsService.dgsComponentIndex.getAllComponents().map { it.name }.toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        val dgsService = project.getService(DgsService::class.java)
        return dgsService.dgsComponentIndex.getAllComponents()
            .filter { it.name == name }
            .map { DgsComponentNavigationItem(it.name, project, it.psiAnnotation, it.psiAnnotation.parentOfType<PsiClass>()?.qualifiedName, it.type.description) }
            .toTypedArray()
    }
}

class DgsComponentNavigationItem(private val name: String, private val project: Project, private val psiElement: PsiElement, private val location: String?, private val type: String): NavigationItem {
    override fun getName(): String {
        return name
    }

    override fun getPresentation(): ItemPresentation {
        return DgsComponentPresentation(name, location, type)
    }

    override fun navigate(requestFocus: Boolean) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openEditor(
            OpenFileDescriptor(
                project,
                psiElement.containingFile.virtualFile,
                psiElement.textOffset
            ), true
        )
    }

    override fun canNavigate(): Boolean {
        return true
    }

    override fun canNavigateToSource(): Boolean {
        return true
    }
}

class DgsComponentPresentation(private val name: String, private val location: String?, private val type: String): ItemPresentation {
    override fun getPresentableText(): String {
        return "$name $type"
    }

    override fun getIcon(unused: Boolean): Icon {
        return DgsConstants.dgsIcon
    }

    override fun getLocationString(): String? {
        return location
    }
}