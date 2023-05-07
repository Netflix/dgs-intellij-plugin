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

package com.netflix.dgs.plugin.hints

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLObjectTypeExtensionDefinition
import com.intellij.lang.jsgraphql.psi.impl.GraphQLDirectiveImpl
import com.intellij.lang.jsgraphql.psi.impl.GraphQLIdentifierImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService


class DgsEntityFetcherInspector : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val dgsService = element.project.getService(DgsService::class.java)
                if(!dgsService.isDgsProject(element.project)) {
                    return
                }

                var directives: List<GraphQLDirective> = emptyList()
                if (element is GraphQLObjectTypeDefinition) {
                    directives = element.directives
                }
                if (element is GraphQLObjectTypeExtensionDefinition){
                    directives = element.directives
                }
                if (directives.any { (it.nameIdentifier as GraphQLIdentifierImpl?)?.name == "key" }) {
                    // look up the corresponding entity fetcher in the type registry
                    val entityFetcher = dgsService.dgsComponentIndex.entityFetchers.find { it.schemaPsi == element }
                    if (entityFetcher == null) {
                        val message = MyBundle.getMessage(
                            "dgs.inspection.missing.entityfetcher.annotation"
                        )

                        holder.registerProblem(
                                (directives[0] as GraphQLDirectiveImpl),
                                message,
                                ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }
    }
}