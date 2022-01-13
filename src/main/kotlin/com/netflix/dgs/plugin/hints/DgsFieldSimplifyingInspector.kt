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

import com.intellij.codeInspection.*
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class DgsFieldSimplifyingInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitMethod(method: UMethod): Boolean {
                val dgsService = method.project.getService(DgsService::class.java)
                if(!dgsService.isDgsProject(method.project)) {
                    return false
                }

                val psiAnnotation = method.annotations.find { DgsDataFetcher.isDataFetcherAnnotation(it) }
                if (psiAnnotation != null) {
                    val fieldAttribute = psiAnnotation.findAttribute("field")
                    if (fieldAttribute != null) {
                        val fieldValue = (fieldAttribute.attributeValue as JvmAnnotationConstantValue?)?.constantValue
                        if (fieldValue == method.name) {
                            val message = MyBundle.getMessage(
                                "dgs.inspection.fieldvalue.simplify"
                            )
                            holder.registerProblem(
                                psiAnnotation.toUElement()?.sourcePsi!!,
                                message,
                                ProblemHighlightType.WEAK_WARNING,
                                DgsFieldNameQuickFix()
                            )
                        }
                    }
                }

                return true
            }
        }, false)
    }

    class DgsFieldNameQuickFix : LocalQuickFix {
        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return "Field name can be omitted"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val newAnnotation : PsiElement = when (descriptor.psiElement) {
                is PsiAnnotation -> {
                    val newAnnotationText = replaceField(descriptor.psiElement.text)
                    PsiElementFactory.getInstance(project).createAnnotationFromText(newAnnotationText, null)
                }
                is KtAnnotationEntry -> {
                    val newAnnotationText = replaceField(descriptor.psiElement.text)
                    KtPsiFactory(project).createAnnotationEntry(newAnnotationText) as PsiElement
                }
                else -> return
            }

            descriptor.psiElement.replace(newAnnotation)
            project.getService(DgsService::class.java).clearCache()
        }

        private fun replaceField(annotationText: String): String {
            return annotationText
                .replace(Regex("""field[\s]*=[\s]*".*""""), "")
                .replace(Regex(""",[\s]*"""), "")
                .replace("()", "")
        }
    }
}