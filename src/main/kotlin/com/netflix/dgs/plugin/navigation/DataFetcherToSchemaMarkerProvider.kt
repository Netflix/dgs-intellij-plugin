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

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.lang.jsgraphql.types.language.ObjectTypeDefinition
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.findParentOfType
import com.intellij.util.containers.orNull
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.services.DgsService
import com.netflix.dgs.plugin.services.DgsSourceCodeProcessor
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement
import com.intellij.openapi.diagnostic.Logger

class DataFetcherToSchemaMarkerProvider : RelatedItemLineMarkerProvider() {
    private val LOG: Logger = Logger.getInstance(DataFetcherToSchemaMarkerProvider::class.java)
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element is PsiAnnotation) {

            if (DgsDataFetcher.isDataFetcherAnnotation(element)) {
                LOG.info("GRAPHQL::processing marker for element $element")
                val dgsService = element.project.getService(DgsService::class.java)
                val index = dgsService.getDgsComponentIndex();
                val dgsDataFetcher = index.dataFetchers.find { it.psiAnnotation == element }

                if (dgsDataFetcher?.schemaPsi != null) {
                    LOG.info("GRAPHQL::adding icon for element $element")
                        val builder =
                            NavigationGutterIconBuilder.create(IconLoader.getIcon("/icons/dgs.svg", this::class.java))
                                .setTargets(dgsDataFetcher.schemaPsi)
                                .setTooltipText("Navigate to GraphQL schema type").createLineMarkerInfo(element)

                        result.add(builder)
                }
            }

        }

    }
}