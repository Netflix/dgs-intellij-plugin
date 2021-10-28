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
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaChangeListener
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaEventListener
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.Topic
import com.netflix.dgs.plugin.DgsDataFetcher
import org.jetbrains.kotlin.idea.extensions.gradle.getTopLevelBuildScriptPsiFile
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType


interface DgsService {
    fun getDgsComponentIndex(): DgsComponentIndex
}

class DgsServiceImpl(private val project: Project) : DgsService, Disposable {
    private var cachedComponentIndex: DgsComponentIndex? = null

    override fun getDgsComponentIndex(): DgsComponentIndex {
        return if (cachedComponentIndex != null) {
            cachedComponentIndex!!
        } else {
            val dgsComponentIndex = DgsComponentIndex()
            val psiManager = PsiManager.getInstance(project)
            val typeDefinitionRegistry =
                GraphQLSchemaProvider.getInstance(project)
                    .getRegistryInfo(project.getTopLevelBuildScriptPsiFile()!!).typeDefinitionRegistry
            val processor = DgsSourceCodeProcessor(dgsComponentIndex, typeDefinitionRegistry)

            FileTypeIndex.processFiles(
                JavaFileType.INSTANCE,
                { file ->
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

            cachedComponentIndex = dgsComponentIndex

            val topic: Topic<GraphQLSchemaEventListener> =
                GraphQLSchemaChangeListener.TOPIC as Topic<GraphQLSchemaEventListener>
            project.messageBus.connect(this).subscribe(
                topic,
                GraphQLSchemaEventListener {
                    cachedComponentIndex = null
                })

//            PsiManager.getInstance(project).addPsiTreeChangeListener(object: PsiTreeChangeAdapter() {
//                override fun childrenChanged(event: PsiTreeChangeEvent) {
//                    println(event.child)
//                }
//            }, this)

            project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    events.forEach {
                        if(JavaFileType.INSTANCE == it.file?.fileType) {
                            val psiFile = PsiManager.getInstance(project).findFile(it.file!!)
                            if(psiFile?.getChildrenOfType<PsiAnnotation>()?.any(DgsDataFetcher.Companion::isDataFetcherAnnotation) == true) {
                                cachedComponentIndex = null
                            }
                        }
                    }
                }
            })


            dgsComponentIndex

        }
    }

    override fun dispose() {

    }
}

data class DgsComponentIndex(val dataFetchers: MutableSet<DgsDataFetcher> = mutableSetOf())