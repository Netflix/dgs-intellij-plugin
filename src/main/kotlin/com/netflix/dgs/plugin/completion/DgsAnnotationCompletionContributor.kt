/*
 * Copyright 2026 Netflix, Inc.
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

package com.netflix.dgs.plugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.jsgraphql.icons.GraphQLIcons
import com.intellij.lang.jsgraphql.types.schema.idl.TypeUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher
import com.netflix.dgs.plugin.services.DgsService
import com.netflix.dgs.plugin.services.internal.GraphQLSchemaRegistry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

class DgsAnnotationCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(PsiLiteralExpression::class.java),
            DgsCompletionProvider
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(KtStringTemplateExpression::class.java),
            DgsCompletionProvider
        )
    }

    private object DgsCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position
            val project = position.project

            val dgsService = project.getService(DgsService::class.java)
            if (!dgsService.isDgsProject(project)) return

            val ctx = extractContext(position) ?: return
            val uAnnotation = ctx.annotationPsi.toUElement() as? UAnnotation ?: return
            if (!isDgsAnnotation(uAnnotation)) return

            val registry = project.getService(GraphQLSchemaRegistry::class.java)

            when (ctx.attrName) {
                "parentType" -> {
                    registry.objectTypeNames(position).forEach {
                        result.addElement(boosted(LookupElementBuilder.create(it).withIcon(GraphQLIcons.Schema.Type)))
                    }
                    registry.interfaceTypeNames(position).forEach {
                        result.addElement(boosted(LookupElementBuilder.create(it).withIcon(GraphQLIcons.Schema.Interface)))
                    }
                }
                "name" -> {
                    if (uAnnotation.qualifiedName == DGS_ENTITY_FETCHER_FQN) {
                        registry.entityTypeNames(position).forEach {
                            result.addElement(boosted(LookupElementBuilder.create(it).withIcon(GraphQLIcons.Schema.Type)))
                        }
                    }
                }
                "field" -> {
                    val parentType = resolveParentType(uAnnotation) ?: return
                    registry.fieldDefinitions(position, parentType).forEach { f ->
                        result.addElement(
                            boosted(
                                LookupElementBuilder.create(f.name)
                                    .withIcon(GraphQLIcons.Schema.Field)
                                    .withTypeText(TypeUtil.simplePrint(f.type))
                            )
                        )
                    }
                }
            }
        }
    }

    private data class AnnotationContext(val attrName: String, val annotationPsi: PsiElement)

    companion object {
        private const val DGS_ENTITY_FETCHER_FQN = "com.netflix.graphql.dgs.DgsEntityFetcher"

        // Rank schema-aware items above IntelliJ's generic word/file completion.
        private const val PRIORITY = 100.0

        private fun boosted(element: LookupElement): LookupElement =
            PrioritizedLookupElement.withPriority(element, PRIORITY)

        private fun extractContext(element: PsiElement): AnnotationContext? {
            // Java
            val javaPair = PsiTreeUtil.getParentOfType(element, PsiNameValuePair::class.java, false)
            if (javaPair != null) {
                val valueExpr = javaPair.value ?: return null
                if (!PsiTreeUtil.isAncestor(valueExpr, element, false)) return null
                val annotation = javaPair.parent?.parent ?: return null
                return AnnotationContext(javaPair.name ?: "value", annotation)
            }
            // Kotlin
            val ktArg = PsiTreeUtil.getParentOfType(element, KtValueArgument::class.java, false)
            if (ktArg != null) {
                val valueExpr = ktArg.getArgumentExpression() ?: return null
                if (!PsiTreeUtil.isAncestor(valueExpr, element, false)) return null
                val annotation = ktArg.parent?.parent ?: return null
                val argName = ktArg.getArgumentName()?.asName?.identifier ?: "value"
                return AnnotationContext(argName, annotation)
            }
            return null
        }

        private fun isDgsAnnotation(annotation: UAnnotation): Boolean =
            DgsDataFetcher.isDataFetcherAnnotation(annotation) ||
                DgsEntityFetcher.isEntityFetcherAnnotation(annotation)

        private fun resolveParentType(annotation: UAnnotation): String? =
            when (annotation.qualifiedName) {
                "com.netflix.graphql.dgs.DgsQuery" -> "Query"
                "com.netflix.graphql.dgs.DgsMutation" -> "Mutation"
                "com.netflix.graphql.dgs.DgsSubscription" -> "Subscription"
                else -> annotation.findAttributeValue("parentType")?.evaluateString()
            }
    }
}
