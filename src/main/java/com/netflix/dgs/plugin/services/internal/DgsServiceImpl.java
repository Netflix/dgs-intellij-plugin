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

import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.netflix.dgs.plugin.services.DgsComponentIndex;
import com.netflix.dgs.plugin.services.DgsComponentProcessor;
import com.netflix.dgs.plugin.services.DgsService;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex;
import org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DgsServiceImpl implements DgsService, Disposable {
    private final Project project;
    private final Set<String> annotations = Set.of(
            "DgsQuery",
            "DgsMutation",
            "DgsSubscription",
            "DgsData",
            "DgsEntityFetcher",
            "DgsDataLoader",
            "DgsDirective",
            "DgsRuntimeWiring",
            "DgsScalar");
    private volatile DgsComponentIndex cachedComponentIndex;

    public DgsServiceImpl(Project project) {
        this.project = project;

        project.getMessageBus().connect().subscribe(ProjectDataImportListener.TOPIC,
                new ProjectDataImportListener() {
                    @Override
                    public void onImportFinished(@Nullable String projectPath) {
                        dependencyFound.set(false);
                        dependenciesProcessed.set(false);

                    }
                });
    }

    private volatile long javaModificationCount;
    private volatile long kotlinModificationCount;
    private final AtomicBoolean dependencyFound = new AtomicBoolean(false);
    private final AtomicBoolean dependenciesProcessed = new AtomicBoolean(false);

    @Override
    public DgsComponentIndex getDgsComponentIndex() {

        if (DumbService.isDumb(project)) {
            return new DgsComponentIndex();
        }

        ModificationTracker javaModificationTracker = PsiModificationTracker.getInstance(project).forLanguage(JavaLanguage.INSTANCE);
        ModificationTracker kotlinModificationTracker = PsiModificationTracker.getInstance(project).forLanguage(KotlinLanguage.INSTANCE);

        if (cachedComponentIndex != null && javaModificationCount == javaModificationTracker.getModificationCount() && kotlinModificationCount == kotlinModificationTracker.getModificationCount()) {
            return cachedComponentIndex;
        } else {
            javaModificationCount = javaModificationTracker.getModificationCount();
            kotlinModificationCount = kotlinModificationTracker.getModificationCount();

            StubIndex stubIndex = StubIndex.getInstance();

            DgsComponentIndex dgsComponentIndex = new DgsComponentIndex();
            GraphQLSchemaRegistry graphQLSchemaRegistry = project.getService(GraphQLSchemaRegistry.class);
            var processor = new DgsComponentProcessor(graphQLSchemaRegistry, dgsComponentIndex);

            annotations.forEach(dataFetcherAnnotation -> {
                stubIndex.processElements(JavaStubIndexKeys.ANNOTATIONS, dataFetcherAnnotation, project, GlobalSearchScope.projectScope(project), PsiAnnotation.class, annotation -> {
                    UAnnotation uElement = (UAnnotation) UastContextKt.toUElement(annotation);
                    if (uElement != null) {
                        processor.process(uElement);
                    }
                    return true;
                });
            });

            stubIndex.processElements(JavaStubIndexKeys.SUPER_CLASSES, "DgsCustomContextBuilder", project, GlobalSearchScope.projectScope(project), PsiReferenceList.class, refList -> {
                PsiClass clazz = PsiTreeUtil.getParentOfType(refList, PsiClass.class);
                dgsComponentIndex.addDgsCustomContext(clazz);
                return true;
            });

            StubIndexKey<String, KtAnnotationEntry> key = KotlinAnnotationsIndex.Helper.getIndexKey();
            List<String> list = new ArrayList<>();
            stubIndex.processAllKeys(key, project, e -> {
                ProgressManager.checkCanceled();
                return list.add(e);
            });
            for (String annotation : list) {
                if (annotations.contains(annotation)) {
                    StubIndex.getElements(key, annotation, project, GlobalSearchScope.projectScope(project), KtAnnotationEntry.class).forEach(dataFetcherAnnotation -> {
                        UAnnotation uElement = (UAnnotation) UastContextKt.toUElement(dataFetcherAnnotation);
                        if (uElement != null) {
                            processor.process(uElement);
                        }
                    });
                }
            }

            StubIndexKey<String, KtClassOrObject> superClassIndexKey = KotlinSuperClassIndex.Helper.getIndexKey();
            stubIndex.processElements(superClassIndexKey, "DgsCustomContextBuilder", project, GlobalSearchScope.projectScope(project), KtClassOrObject.class, clazz -> {
                dgsComponentIndex.addDgsCustomContext(clazz);
                return true;
            });

            cachedComponentIndex = dgsComponentIndex;
            ProjectView.getInstance(project).refresh();

            return dgsComponentIndex;
        }
    }

    @Override
    public boolean isDgsProject(Project project) {
        if (!dependenciesProcessed.get()) {
            for (Module m : ModuleManager.getInstance(project).getModules()) {
                var libraries = ModuleRootManager.getInstance(m).orderEntries().librariesOnly().compileOnly();
                libraries.forEachLibrary(l -> {
                    dependenciesProcessed.getAndSet(true);
                    String name = l.getName();
                    if (name != null && name.contains("com.netflix.graphql.dgs")) {
                        dependencyFound.set(true);
                        return false;
                    }
                    return true;
                });

                if(dependencyFound.get()) {
                    return true;
                }
            }

            return false;
        }

        return dependencyFound.get();
    }

    @Override
    public void clearCache() {
        cachedComponentIndex = null;
    }

    @Override
    public void dispose() {

    }
}
