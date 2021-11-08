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

import com.intellij.AppTopics
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaChangeListener
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaEventListener
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.Topic
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher
import org.jetbrains.kotlin.idea.extensions.gradle.getTopLevelBuildScriptPsiFile
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


interface DgsService {
    fun getDgsComponentIndex(): DgsComponentIndex
}

class DgsServiceImpl(private val project: Project) : DgsService, FileDocumentManagerListener, Disposable {
    private var cachedComponentIndex: DgsComponentIndex? = null
    private val LOG: Logger = Logger.getInstance(DgsServiceImpl::class.java)

    private val cacheLock: ReadWriteLock = ReentrantReadWriteLock(true)
    private val writeLock = cacheLock.writeLock()

    init {
        val topic: Topic<GraphQLSchemaEventListener> =
            GraphQLSchemaChangeListener.TOPIC as Topic<GraphQLSchemaEventListener>
        project.messageBus.connect(this).subscribe(
            topic,
            GraphQLSchemaEventListener {
                cachedComponentIndex = null
            })
        project.messageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, this)
    }

    override fun beforeDocumentSaving(document: Document) {

        val file = FileDocumentManager.getInstance().getFile(document)
        val psiFile = PsiManager.getInstance(project).findFile(file!!)
        val typeDefinitionRegistry =
            GraphQLSchemaProvider.getInstance(project)
                .getRegistryInfo(project.getTopLevelBuildScriptPsiFile()!!).typeDefinitionRegistry

        try {
            writeLock.lock()
            LOG.info("GRAPHQL::Before document saving.")
            cachedComponentIndex?.fileUpdated(psiFile!!)
            val dgsComponentIndex = if (cachedComponentIndex != null) {
                cachedComponentIndex as DgsComponentIndex
            } else {
                DgsComponentIndex()
            }
            val processor = DgsSourceCodeProcessor(dgsComponentIndex, typeDefinitionRegistry)
            LOG.info("GRAPHQL::Before processing file in beforeDocumentSaving ${psiFile}.")
            processor.process(psiFile!!)
            cachedComponentIndex = dgsComponentIndex
        } finally {
            writeLock.unlock()
            DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            EditorFactory.getInstance().refreshAllEditors()

        }
    }

    override fun getDgsComponentIndex(): DgsComponentIndex {
        try {
            writeLock.lock()
            return if (cachedComponentIndex != null) {
                LOG.info("GRAPHQL:: Returning cached Index ${cachedComponentIndex!!.dataFetchers}")
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
                return dgsComponentIndex
            }
        } finally {
            writeLock.unlock()
        }
    }

    override fun dispose() {

    }
}

data class DgsComponentIndex(val dataFetchers: MutableSet<DgsDataFetcher> = mutableSetOf(), val entityFetchers: MutableSet<DgsEntityFetcher> = mutableSetOf()) {

    private val LOG: Logger = Logger.getInstance(DgsComponentIndex::class.java)
    fun fileUpdated(file: PsiFile) {
        LOG.info("GRAPHQL::Removing data fetchers for ${file}")
        dataFetchers.removeAll { it.psiFile == file }
    }
}