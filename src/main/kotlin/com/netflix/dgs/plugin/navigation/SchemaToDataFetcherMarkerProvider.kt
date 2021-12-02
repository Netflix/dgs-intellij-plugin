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
import com.intellij.lang.jsgraphql.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.services.DgsService

class SchemaToDataFetcherMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val dgsService = element.project.getService(DgsService::class.java)
        val psiLeaf = PsiTreeUtil.getDeepestFirst(element)

        val iconBuilder = when (element) {
            is GraphQLFieldDefinition -> {
                dgsService.dgsComponentIndex.dataFetchers.find { it.schemaPsi == element }?.let {
                        NavigationGutterIconBuilder.create(DgsConstants.dgsIcon)
                            .setTargets(it.psiAnnotation)
                            .setTooltipText("Navigate to DGS data fetcher")
                            .createLineMarkerInfo(psiLeaf)
                }
            }
            is GraphQLObjectTypeDefinition, is GraphQLObjectTypeExtensionDefinition -> {
                dgsService.dgsComponentIndex.entityFetchers.find { it.schemaPsi == element }?.let {
                        NavigationGutterIconBuilder.create(DgsConstants.dgsIcon)
                            .setTargets(it.psiAnnotation)
                            .setTooltipText("Navigate to DGS entity fetcher")
                            .createLineMarkerInfo(psiLeaf)
                }
            }
            is GraphQLScalarTypeDefinition -> {
                dgsService.dgsComponentIndex.scalars.find { it.schemaPsi == element }?.let {
                        NavigationGutterIconBuilder.create(DgsConstants.dgsIcon)
                            .setTargets(it.psiAnnotation)
                            .setTooltipText("Navigate to DGS scalar implementation")
                            .createLineMarkerInfo(psiLeaf)
                }
            }
            is GraphQLDirectiveDefinition -> {
                dgsService.dgsComponentIndex.directives.find { it.schemaPsi == element }?.let {
                        NavigationGutterIconBuilder.create(DgsConstants.dgsIcon)
                            .setTargets(it.psiAnnotation)
                            .setTooltipText("Navigate to DGS directive implementation")
                            .createLineMarkerInfo(psiLeaf)
                }
            }
            else -> null
        }

        iconBuilder?.apply { result.add(this) }
    }

}