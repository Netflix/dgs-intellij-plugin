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

package com.netflix.dgs.plugin

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElement

data class DgsDataFetcher(
    val parentType: String,
    val field: String,
    val psiMethod: PsiElement,
    override val psiAnnotation: PsiElement,
    val psiFile: PsiFile,
    val schemaPsi: PsiElement?
): NamedNavigationComponent {
    companion object {
        private val annotationQualifiedNames = setOf(
            "com.netflix.graphql.dgs.DgsQuery",
            "com.netflix.graphql.dgs.DgsMutation",
            "com.netflix.graphql.dgs.DgsSubscription",
            "com.netflix.graphql.dgs.DgsData"
        )

        fun isDataFetcherAnnotation(annotation: PsiAnnotation): Boolean {
           return annotationQualifiedNames.contains(annotation.qualifiedName)
        }

        fun isDataFetcherAnnotation(annotation: UAnnotation): Boolean {
            return annotationQualifiedNames.contains(annotation.qualifiedName)
        }

        private fun getParentType(annotation: PsiAnnotation): String? {
            return when (annotation.qualifiedName) {
                "com.netflix.graphql.dgs.DgsQuery" -> "Query"
                "com.netflix.graphql.dgs.DgsMutation" -> "Mutation"
                "com.netflix.graphql.dgs.DgsSubscription" -> "Subscription"
                "com.netflix.graphql.dgs.DgsData" -> (annotation.toUElement() as UAnnotation).findAttributeValue("parentType")
                    ?.evaluateString()
                else -> throw IllegalArgumentException("Annotation ${annotation.qualifiedName} is not a data fetcher annotation")
            }
        }

        fun getParentType(method: PsiMethod): String? {
            val annotation = getDataFetcherAnnotation(method)
            return getParentType(annotation)
        }

        private fun getDataFetcherAnnotation(method: PsiMethod) =
            (method.annotations.find { a -> isDataFetcherAnnotation(a) }
                ?: throw IllegalArgumentException("Method ${method.name} is not a data fetcher"))

        private fun getFieldFromAnnotation(annotation: PsiAnnotation): String? {
            return (annotation.toUElement() as UAnnotation).findAttributeValue("field")?.evaluateString()
        }

        fun getField(method: PsiMethod): String {
            val dataFetcherAnnotation = getDataFetcherAnnotation(method)
            return getFieldFromAnnotation(dataFetcherAnnotation) ?: method.name
        }
    }

    override val name = "${parentType}.${field}"
}