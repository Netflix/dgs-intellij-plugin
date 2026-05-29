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

package com.netflix.dgs.plugin.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.jsgraphql.psi.impl.GraphQLIdentifierImpl
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.services.DgsService
import com.netflix.dgs.plugin.services.internal.GraphQLSchemaRegistry
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

class DgsAnnotationGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        sourceElement ?: return null

        val project = sourceElement.project
        val dgsService = project.getService(DgsService::class.java)
        if (!dgsService.isDgsProject(project)) return null

        val context = extractContext(sourceElement) ?: return null
        if (!isDgsDataAnnotation(context.annotation)) return null

        val schemaRegistry = project.getService(GraphQLSchemaRegistry::class.java)

        return when (context.attrName) {
            "parentType" -> resolveType(context.value, sourceElement, schemaRegistry)
            "name" -> if (shortName(context.annotation) == "DgsEntityFetcher") resolveType(context.value, sourceElement, schemaRegistry) else null
            "field" -> {
                val parentType = resolveParentType(context.annotation, shortName(context.annotation)) ?: return null
                resolveField(parentType, context.value, sourceElement, schemaRegistry)
            }
            else -> null
        }
    }

    private data class AnnotationContext(val attrName: String, val value: String, val annotation: PsiElement)

    private fun extractContext(element: PsiElement): AnnotationContext? {
        // Java: cursor inside a @Foo(attr = ...) parameter
        val javaPair = PsiTreeUtil.getParentOfType(element, PsiNameValuePair::class.java, false)
        if (javaPair != null) {
            val valueExpr = javaPair.value ?: return null
            // Ignore clicks on the attribute name; only resolve when cursor is in the value.
            if (!PsiTreeUtil.isAncestor(valueExpr, element, false)) return null
            val annotation = javaPair.parent?.parent as? PsiAnnotation ?: return null
            // UAST evaluates literals and constant references (Constants.MOVIE_TYPE) uniformly.
            val value = (valueExpr.toUElement() as? UExpression)?.evaluateString() ?: return null
            return AnnotationContext(javaPair.name ?: "value", value, annotation)
        }
        // Kotlin: cursor inside a @Foo(attr = ...) argument
        val ktArg = PsiTreeUtil.getParentOfType(element, KtValueArgument::class.java, false)
        if (ktArg != null) {
            val valueExpr = ktArg.getArgumentExpression() ?: return null
            if (!PsiTreeUtil.isAncestor(valueExpr, element, false)) return null
            val annotation = ktArg.parent?.parent as? KtAnnotationEntry ?: return null
            val value = (valueExpr.toUElement() as? UExpression)?.evaluateString() ?: return null
            val argName = ktArg.getArgumentName()?.asName?.identifier ?: "value"
            return AnnotationContext(argName, value, annotation)
        }
        return null
    }

    private fun shortName(annotation: PsiElement): String = when (annotation) {
        is PsiAnnotation -> annotation.qualifiedName?.substringAfterLast('.') ?: ""
        is KtAnnotationEntry -> annotation.shortName?.identifier ?: ""
        else -> ""
    }

    private fun isDgsDataAnnotation(annotation: PsiElement): Boolean =
        shortName(annotation) in DGS_SHORT_NAMES

    private fun resolveType(
        typeName: String,
        sourceElement: PsiElement,
        schemaRegistry: GraphQLSchemaRegistry
    ): Array<PsiElement>? {
        val typePsi = schemaRegistry.psiForType(sourceElement, typeName).orElse(null) ?: return null
        return arrayOf(nameIdentifier(typePsi))
    }

    private fun resolveField(
        parentType: String,
        fieldName: String,
        sourceElement: PsiElement,
        schemaRegistry: GraphQLSchemaRegistry
    ): Array<PsiElement>? {
        val fieldPsi = schemaRegistry.psiForSchemaType(sourceElement, parentType, fieldName)?.orElse(null) ?: return null
        return arrayOf(nameIdentifier(fieldPsi))
    }

    // Returning the inner identifier (rather than the whole def) gives IntelliJ a clean hover label.
    private fun nameIdentifier(element: PsiElement): PsiElement =
        PsiTreeUtil.findChildOfType(element, GraphQLIdentifierImpl::class.java) ?: element

    private fun resolveParentType(annotation: PsiElement, shortName: String): String? =
        when (shortName) {
            "DgsQuery" -> "Query"
            "DgsMutation" -> "Mutation"
            "DgsSubscription" -> "Subscription"
            else -> siblingValue(annotation, "parentType")
        }

    private fun siblingValue(annotation: PsiElement, attrName: String): String? {
        val uAnnotation = annotation.toUElement() as? UAnnotation ?: return null
        return uAnnotation.findAttributeValue(attrName)?.evaluateString()
    }

    companion object {
        private val DGS_SHORT_NAMES = setOf(
            "DgsData", "DgsQuery", "DgsMutation", "DgsSubscription", "DgsEntityFetcher"
        )
    }
}
