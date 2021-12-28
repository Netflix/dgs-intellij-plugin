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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
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
import com.netflix.dgs.plugin.DgsCustomContext;
import com.netflix.dgs.plugin.services.DgsComponentIndex;
import com.netflix.dgs.plugin.services.DgsComponentProcessor;
import com.netflix.dgs.plugin.services.DgsService;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex;
import org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UastContextKt;

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
    }

    private volatile long javaModificationCount;
    private volatile long kotlinModificationCount;
    private final AtomicBoolean dependencyFound = new AtomicBoolean(false);
    private final AtomicBoolean dependenciesProcessed = new AtomicBoolean(false);

    @Override
    public DgsComponentIndex getDgsComponentIndex() {
        ModificationTracker javaModificationTracker = PsiModificationTracker.SERVICE.getInstance(project).forLanguage(JavaLanguage.INSTANCE);
        ModificationTracker kotlinModificationTracker = PsiModificationTracker.SERVICE.getInstance(project).forLanguage(KotlinLanguage.INSTANCE);

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
                    if(uElement != null) {
                        processor.process(uElement);
                    }
                    return true;
                });
            });

            stubIndex.processElements(JavaStubIndexKeys.SUPER_CLASSES, "DgsCustomContextBuilder", project, GlobalSearchScope.projectScope(project), PsiReferenceList.class, refList -> {
                PsiClass clazz = PsiTreeUtil.getParentOfType(refList, PsiClass.class);

                if(clazz != null) {
                    dgsComponentIndex.getCustomContexts().add(new DgsCustomContext(clazz.getName(), clazz, clazz.getContainingFile()));
                }

                return true;
            });

            StubIndexKey<String, KtAnnotationEntry> key = KotlinAnnotationsIndex.getInstance().getKey();
            stubIndex.processAllKeys(key, project, annotation -> {
                if (annotations.contains(annotation)) {
                    StubIndex.getElements(key, annotation, project, GlobalSearchScope.projectScope(project), KtAnnotationEntry.class).forEach(dataFetcherAnnotation -> {
                        UAnnotation uElement = (UAnnotation) UastContextKt.toUElement(dataFetcherAnnotation);
                        if(uElement != null) {
                            processor.process(uElement);
                        }
                    });
                }
                return true;
            });

            StubIndexKey<String, KtClassOrObject> superClassIndexKey = KotlinSuperClassIndex.getInstance().getKey();
            stubIndex.processElements(superClassIndexKey, "DgsCustomContextBuilder", project, GlobalSearchScope.projectScope(project), KtClassOrObject.class, clazz -> {
                dgsComponentIndex.getCustomContexts().add(new DgsCustomContext(clazz.getName(), clazz, clazz.getContainingFile()));
               return true;
            });
//            stubIndex.processAllKeys(superClassIndexKey, project, item -> {

            cachedComponentIndex = dgsComponentIndex;

            ProjectView.getInstance(project).refresh();

            return dgsComponentIndex;
        }
    }

    @Override
    public boolean isDgsProject(Project project) {
        if(!dependenciesProcessed.get()) {
            ProjectRootManager.getInstance(project).orderEntries().librariesOnly().compileOnly().forEachLibrary(l -> {
                String name = l.getName();
                if(name != null && name.contains("com.netflix.graphql.dgs:graphql-dgs")) {
                    dependencyFound.set(true);
                    return false;
                }
                return true;
            });

            dependenciesProcessed.getAndSet(true);
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
