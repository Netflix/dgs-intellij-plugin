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

import com.intellij.AppTopics;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaChangeListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.netflix.dgs.plugin.services.DgsComponentIndex;
import com.netflix.dgs.plugin.services.DgsDataProcessor;
import com.netflix.dgs.plugin.services.DgsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.util.Set;

public class DgsServiceImpl implements DgsService, Disposable {

    private final Project project;
    private final Set<String> dataFetcherAnnotations = Set.of(
            "DgsQuery",
            "DgsMutation",
            "DgsSubscription",
            "DgsData");
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
            var processor = new DgsDataProcessor(project.getService(GraphQLSchemaRegistry.class), dgsComponentIndex);
            StubIndex stubIndex = StubIndex.getInstance();

            dataFetcherAnnotations.forEach(dataFetcherAnnotation -> {
                stubIndex.processElements(JavaStubIndexKeys.ANNOTATIONS, dataFetcherAnnotation, project, GlobalSearchScope.projectScope(project), PsiAnnotation.class, annotation -> {
                    processor.process(annotation);
                    return true;
                });
            });

            cachedComponentIndex = dgsComponentIndex;

            var topic = GraphQLSchemaChangeListener.TOPIC;
            project.getMessageBus().connect(this).subscribe(
                    topic,
                    version -> cachedComponentIndex = null
            );

            project.getMessageBus().connect(this).subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
                @Override
                public void beforeDocumentSaving(@NotNull Document document) {
                    var file = FileDocumentManager.getInstance().getFile(document);

                    if (JavaFileType.INSTANCE == file.getFileType() || KotlinFileType.INSTANCE == file.getFileType()) {
                        cachedComponentIndex = null;
                    }
                }
            });

            return dgsComponentIndex;
        }
    }

    @Override
    public void dispose() {

    }
}
