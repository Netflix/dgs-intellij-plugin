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

package com.netflix.dgs.plugin.services

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.netflix.dgs.plugin.DgsDataFetcher
import org.jetbrains.kotlin.idea.extensions.gradle.getTopLevelBuildScriptPsiFile

interface DgsService {
    fun getDgsComponentIndex(): DgsComponentIndex
}

class DgsServiceImpl(private val project: Project) : DgsService {
    override fun getDgsComponentIndex(): DgsComponentIndex {
        val dgsComponentIndex = DgsComponentIndex()
        val psiManager = PsiManager.getInstance(project)
        val typeDefinitionRegistry =
            GraphQLSchemaProvider.getInstance(project).getRegistryInfo(project.getTopLevelBuildScriptPsiFile()!!).typeDefinitionRegistry
        val processor = DgsSourceCodeProcessor(dgsComponentIndex, typeDefinitionRegistry)

        FileTypeIndex.processFiles(
            JavaFileType.INSTANCE,
            {file ->
                val psiFile = psiManager.findFile(file)
                if (psiFile != null) {
                    processor.process(psiFile)
                }
                true
            },
            GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.projectScope(project),
                JavaFileType.INSTANCE
            )
        )

        return dgsComponentIndex
    }
}

data class DgsComponentIndex(val dataFetchers: MutableSet<DgsDataFetcher> = mutableSetOf())