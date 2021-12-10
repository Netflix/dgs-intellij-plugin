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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElement

class DataFetcherToSchemaMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val dgsService = element.project.getService(DgsService::class.java)
        if(!dgsService.isDgsProject(element.project)) {
            return
        }

        val uElement = element.toUElement()

        if (uElement is UAnnotation) {

            if (DgsDataFetcher.isDataFetcherAnnotation(uElement) || DgsEntityFetcher.isEntityFetcherAnnotation(uElement)) {

                val dgsDataFetcher = dgsService.dgsComponentIndex.dataFetchers.find { it.psiAnnotation == element }
                val dgsEntityFetcher = dgsService.dgsComponentIndex.entityFetchers.find { it.psiAnnotation == element }

                if (dgsDataFetcher?.schemaPsi != null || dgsEntityFetcher?.schemaPsi != null) {

                    val psiIdentifier = PsiTreeUtil.findChildOfType(element, PsiIdentifier::class.java)?:element
                    val target = dgsDataFetcher?.schemaPsi?: dgsEntityFetcher!!.schemaPsi
                    val builder =
                        NavigationGutterIconBuilder.create(DgsConstants.dgsIcon)
                            .setTargets(target)
                            .setTooltipText("Navigate to GraphQL schema type")
                            .createLineMarkerInfo(psiIdentifier)

                    result.add(builder)
                }
            }

        }

    }
}