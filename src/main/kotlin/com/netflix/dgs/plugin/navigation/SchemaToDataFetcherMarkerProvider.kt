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
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.services.DgsService

class SchemaToDataFetcherMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val dgsService = element.project.getService(DgsService::class.java)

        if (element is GraphQLFieldDefinition) {
            val dataFetcher = dgsService.getDgsComponentIndex().dataFetchers.find { it.schemaPsi == element }

            if (dataFetcher != null) {
                val builder =
                    NavigationGutterIconBuilder.create(IconLoader.getIcon("/icons/dgs.svg", this::class.java))
                        .setTargets(dataFetcher.psiAnnotation)
                        .setTooltipText("Navigate to DGS data fetcher")
                        .createLineMarkerInfo(element)

                result.add(builder)
            }
        }
    }
}