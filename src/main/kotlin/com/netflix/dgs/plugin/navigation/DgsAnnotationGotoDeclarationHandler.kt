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
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class DgsAnnotationGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        sourceElement ?: return null

        val dgsService = sourceElement.project.getService(DgsService::class.java)
        if (!dgsService.isDgsProject(sourceElement.project)) return null

        val context = extractContext(sourceElement) ?: return null
        if (!isDgsDataAnnotation(context.annotation)) return null

        return when (context.attrName) {
            "parentType", "typename" -> resolveType(context.value, dgsService)
            "field" -> {
                val parentType = resolveParentType(context.annotation, shortName(context.annotation)) ?: return null
                resolveField(parentType, context.value, dgsService)
            }
            else -> null
        }
    }

    private data class AnnotationContext(val attrName: String, val value: String, val annotation: PsiElement)

    private fun extractContext(element: PsiElement): AnnotationContext? {
        val stringElement: PsiElement =
            PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java, false)
                ?: PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
                ?: return null

        val value = stringValue(stringElement) ?: return null

        if (stringElement is PsiLiteralExpression) {
            val pair = stringElement.parent as? PsiNameValuePair ?: return null
            val annotation = pair.parent?.parent as? PsiAnnotation ?: return null
            return AnnotationContext(pair.name ?: "value", value, annotation)
        }
        if (stringElement is KtStringTemplateExpression) {
            val arg = stringElement.parent as? KtValueArgument ?: return null
            val annotation = arg.parent?.parent as? KtAnnotationEntry ?: return null
            val argName = arg.getArgumentName()?.asName?.identifier ?: "value"
            return AnnotationContext(argName, value, annotation)
        }
        return null
    }

    private fun stringValue(element: PsiElement): String? = when (element) {
        is PsiLiteralExpression -> element.value as? String
        is KtStringTemplateExpression ->
            if (element.hasInterpolation()) null
            else element.entries.firstOrNull()?.text
        else -> null
    }

    private fun shortName(annotation: PsiElement): String = when (annotation) {
        is PsiAnnotation -> annotation.qualifiedName?.substringAfterLast('.') ?: ""
        is KtAnnotationEntry -> annotation.shortName?.identifier ?: ""
        else -> ""
    }

    private fun isDgsDataAnnotation(annotation: PsiElement): Boolean =
        shortName(annotation) in DGS_SHORT_NAMES

    private fun resolveType(typeName: String, dgsService: DgsService): Array<PsiElement>? {
        val fetcher = dgsService.dgsComponentIndex.dataFetchers
            .firstOrNull { it.parentType == typeName && it.schemaPsi != null }
        val fieldPsi = fetcher?.schemaPsi ?: return null
        val typePsi = fieldPsi.parent?.parent ?: fieldPsi.parent ?: return null
        return arrayOf(typePsi)
    }

    private fun resolveField(parentType: String, fieldName: String, dgsService: DgsService): Array<PsiElement>? {
        val fetcher = dgsService.dgsComponentIndex.dataFetchers
            .firstOrNull { it.parentType == parentType && it.field == fieldName && it.schemaPsi != null }
        val schemaPsi = fetcher?.schemaPsi ?: return null
        return arrayOf(schemaPsi)
    }

    private fun resolveParentType(annotation: PsiElement, shortName: String): String? =
        when (shortName) {
            "DgsQuery" -> "Query"
            "DgsMutation" -> "Mutation"
            "DgsSubscription" -> "Subscription"
            else -> siblingValue(annotation, "parentType")
        }

    private fun siblingValue(annotation: PsiElement, attrName: String): String? = when (annotation) {
        is PsiAnnotation -> (annotation.findAttributeValue(attrName) as? PsiLiteralExpression)?.value as? String
        is KtAnnotationEntry -> annotation.valueArguments
            .firstOrNull { it.getArgumentName()?.asName?.identifier == attrName }
            ?.getArgumentExpression()
            ?.let { it as? KtStringTemplateExpression }
            ?.entries?.firstOrNull()?.text
        else -> null
    }

    companion object {
        private val DGS_SHORT_NAMES = setOf(
            "DgsData", "DgsQuery", "DgsMutation", "DgsSubscription", "DgsEntityFetcher"
        )
    }
}
