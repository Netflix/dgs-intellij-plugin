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

package com.netflix.dgs.plugin.services.internal;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaChangeListener;
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.netflix.dgs.plugin.DgsDataFetcher;
import com.netflix.dgs.plugin.services.DgsComponentIndex;
import com.netflix.dgs.plugin.services.DgsService;
import com.netflix.dgs.plugin.services.DgsSourceCodeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.util.List;

public class DgsServiceImpl implements DgsService, Disposable {

    private final Project project;
    private DgsComponentIndex cachedComponentIndex;

    public DgsServiceImpl(Project project) {
        this.project = project;
    }

    @Override
    public DgsComponentIndex getDgsComponentIndex() {

        if (cachedComponentIndex != null) {
            return cachedComponentIndex;
        } else {
            DgsComponentIndex dgsComponentIndex = new DgsComponentIndex();
            var psiManager = PsiManager.getInstance(project);
            var projectPsiFile = PsiManager.getInstance(project).findFile(project.getProjectFile());

            var typeDefinitionRegistry =
                    GraphQLSchemaProvider.getInstance(project)
                            .getRegistryInfo(projectPsiFile).getTypeDefinitionRegistry();

            var processor = new DgsSourceCodeProcessor(dgsComponentIndex, typeDefinitionRegistry);

            FileTypeIndex.processFiles(
                    JavaFileType.INSTANCE,
                    file -> {
                        var psiFile = psiManager.findFile(file);
                        if (psiFile != null) {
                            processor.process(psiFile);
                        }
                        return true;
                    },
                    GlobalSearchScope.getScopeRestrictedByFileTypes(
                            GlobalSearchScope.projectScope(project),
                            JavaFileType.INSTANCE
                    )
            );

            cachedComponentIndex = dgsComponentIndex;

            var topic = GraphQLSchemaChangeListener.TOPIC;
            project.getMessageBus().connect(this).subscribe(
                    topic,
                    version -> cachedComponentIndex = null
            );

            project.getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    events.stream().forEach(event -> {
                        if(JavaFileType.INSTANCE == event.getFile().getFileType() || KotlinFileType.INSTANCE ==event.getFile().getFileType()) {
                            var psiFile = PsiManager.getInstance(project).findFile(event.getFile());



                            if(PsiTreeUtil.findChildrenOfType(psiFile, PsiAnnotation.class).stream().anyMatch(annotation -> DgsDataFetcher.Companion.isDataFetcherAnnotation(annotation))) {
                                cachedComponentIndex.fileUpdated(psiFile);
                                processor.process(psiFile);

                                EditorFactory.getInstance().refreshAllEditors();
                            }
                        }
                    });
                }
            });

            return dgsComponentIndex;
        }
    }

    @Override
    public void dispose() {

    }
}
