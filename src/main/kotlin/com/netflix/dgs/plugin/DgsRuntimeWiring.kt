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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UAnnotation

data class DgsRuntimeWiring(
    override val name: String,
    val psiMethod: PsiElement,
    override val psiAnnotation: PsiElement,
    val psiFile: PsiFile
): NamedNavigationComponent {
    companion object {
        fun isDgsRuntimeWiringAnnotation(annotation: UAnnotation) = annotation.qualifiedName == "com.netflix.graphql.dgs.DgsRuntimeWiring"
    }

    override val type = DgsComponentType.RUNTIME_WIRING
}