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

data class DgsEntityFetcher(val name: String, val psiMethod: PsiElement, val psiAnnotation: PsiElement, val psiFile: PsiFile, val schemaPsi: PsiElement?) {
    companion object {
        fun isEntityFetcherAnnotation(annotation: UAnnotation): Boolean {
            return annotation.qualifiedName == "com.netflix.graphql.dgs.DgsEntityFetcher"
        }

        fun isEntityFetcherAnnotation(annotation: PsiAnnotation): Boolean {
            return annotation.qualifiedName == "com.netflix.graphql.dgs.DgsEntityFetcher"
        }

        fun getEntityFetcherAnnotation(method: PsiMethod) =
            (method.annotations.find { a -> isEntityFetcherAnnotation(a) }
                ?: throw IllegalArgumentException("Method ${method.name} is not an entity fetcher"))

        fun getNameFromAnnotation(annotation: PsiAnnotation): String? {
            return (annotation.toUElement() as UAnnotation).findAttributeValue("name")?.evaluateString()
        }

        fun getName(method: PsiMethod): String {
            val entityFetcherAnnotation = getEntityFetcherAnnotation(method)
            return getNameFromAnnotation(entityFetcherAnnotation)?:method.name
        }
    }
}