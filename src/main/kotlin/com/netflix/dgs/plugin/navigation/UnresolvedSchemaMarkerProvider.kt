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
import com.intellij.lang.jsgraphql.psi.impl.GraphQLIdentifierImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.services.DgsService
import javax.swing.Icon

private val ROOT_OPERATION_TYPES = setOf("Query", "Mutation", "Subscription")
private val DIRECTIVES_REQUIRING_RESOLUTION = setOf("requires", "external")

class UnresolvedSchemaMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "DGS Unresolved Schema Elements"

    override fun getIcon(): Icon = DgsConstants.dgsUnresolvedIcon

    override fun getId(): String = "dgs.unresolved.schema.marker"

    override fun isEnabledByDefault(): Boolean = true

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val dgsService = element.project.getService(DgsService::class.java)
        if (!dgsService.isDgsProject(element.project)) {
            return
        }

        // Find the identifier child element to pin the gutter icon, ignoring any preceding comments
        // Then get the actual leaf element (GraphQLIdentifierImpl is not a leaf, it has a NAME child)
        val identifier = PsiTreeUtil.findChildOfType(element, GraphQLIdentifierImpl::class.java) ?: element
        val psiLeaf = PsiTreeUtil.getDeepestFirst(identifier)

        val iconBuilder = when (element) {
            is GraphQLFieldDefinition -> {
                // Only flag fields that genuinely need a data fetcher:
                //   - Fields on Query/Mutation/Subscription (root operations always need one)
                //   - Fields with @requires/@external (federation resolution can't auto-resolve)
                // Sub-fields on regular types are auto-resolved by DGS from the parent object's
                // properties or matching methods (including methods with arguments), so they
                // don't need an explicit fetcher.
                val parentTypeName = (element.parent?.parent as? GraphQLNamedElement)?.name
                val isRootOperationField = parentTypeName in ROOT_OPERATION_TYPES
                val requiresResolutionLogic = element.directives.any { directive ->
                    (directive.nameIdentifier as? GraphQLIdentifierImpl)?.name in DIRECTIVES_REQUIRING_RESOLUTION
                }
                if (!isRootOperationField && !requiresResolutionLogic) {
                    null
                } else {
                    val fetcher = dgsService.dgsComponentIndex.dataFetchers.find { it.schemaPsi == element }
                    if (fetcher == null) {
                        NavigationGutterIconBuilder.create(DgsConstants.dgsUnresolvedIcon)
                            .setTargets(emptyList())
                            .setTooltipText("No DGS data fetcher found")
                            .createLineMarkerInfo(psiLeaf)
                    } else null
                }
            }
            is GraphQLObjectTypeDefinition, is GraphQLObjectTypeExtensionDefinition -> {
                val entityFetcher = dgsService.dgsComponentIndex.entityFetchers.find { it.schemaPsi == element }
                if (entityFetcher == null) {
                    NavigationGutterIconBuilder.create(DgsConstants.dgsUnresolvedIcon)
                        .setTargets(emptyList())
                        .setTooltipText("No DGS entity fetcher found")
                        .createLineMarkerInfo(psiLeaf)
                } else null
            }
            is GraphQLScalarTypeDefinition -> {
                val scalar = dgsService.dgsComponentIndex.scalars.find { it.schemaPsi == element }
                if (scalar == null) {
                    NavigationGutterIconBuilder.create(DgsConstants.dgsUnresolvedIcon)
                        .setTargets(emptyList())
                        .setTooltipText("No DGS scalar implementation found")
                        .createLineMarkerInfo(psiLeaf)
                } else null
            }
            is GraphQLDirectiveDefinition -> {
                val directive = dgsService.dgsComponentIndex.directives.find { it.schemaPsi == element }
                if (directive == null) {
                    NavigationGutterIconBuilder.create(DgsConstants.dgsUnresolvedIcon)
                        .setTargets(emptyList())
                        .setTooltipText("No DGS directive implementation found")
                        .createLineMarkerInfo(psiLeaf)
                } else null
            }
            else -> null
        }

        iconBuilder?.apply { result.add(this) }
    }
}