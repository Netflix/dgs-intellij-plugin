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
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.uast.UastVisitorAdapter
import com.intellij.util.castSafelyTo
import com.netflix.dgs.plugin.DgsConstants
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor
import java.util.*


class DgsComponentInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            @Suppress("UElementAsPsi")
            override fun visitClass(node: UClass): Boolean {
                val dgsService = node.project.getService(DgsService::class.java)
                if(!dgsService.isDgsProject(node.project)) {
                    return false
                }

                val hasDgsComponentAnnotation: Boolean =
                    node.javaPsi.hasAnnotation("com.netflix.graphql.dgs.DgsComponent")
                if (Arrays.stream(node.methods)
                        .anyMatch { m ->
                            DgsConstants.dgsAnnotations.stream().anyMatch { annotation -> m.javaPsi.hasAnnotation(annotation) }
                        } && !hasDgsComponentAnnotation
                ) {
                    node.identifyingElement?.let {
                        holder.registerProblem(
                            it,
                            MyBundle.getMessage("dgs.inspection.missing.component.annotation"),
                            ProblemHighlightType.WARNING,
                            DgsComponentQuickfix
                        )
                    }
                    return true
                }
                return false
            }
        }, false)
    }

    object DgsComponentQuickfix : LocalQuickFix {
        override fun getFamilyName(): @IntentionFamilyName String {
            return name
        }

        override fun getName(): String {
            return MyBundle.message("dgs.inspection.missing.component.annotation")
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val clazz: UClass = descriptor.psiElement.getUastParentOfType(UClass::class.java)!!

            val sourcePsi = clazz.sourcePsi
            if(sourcePsi is PsiClass) {
                val factory: PsiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
                val annotationFromText: PsiAnnotation = factory.createAnnotationFromText("@DgsComponent", null)
                val firstModifier = sourcePsi.modifierList?.firstChild
                if(firstModifier != null) {
                    sourcePsi.modifierList?.addBefore(annotationFromText, firstModifier)
                } else {
                    sourcePsi.addBefore(annotationFromText, sourcePsi)
                }

                val importStatement = factory.createImportStatement(factory.createTypeByFQClassName("com.netflix.graphql.dgs.DgsComponent").resolve()!!)
                sourcePsi.containingFile.castSafelyTo<PsiJavaFile>()?.importList?.add(importStatement)
            } else if(sourcePsi is KtClass) {
                val fqName = FqName("com.netflix.graphql.dgs.DgsComponent")
                sourcePsi.addAnnotation(fqName)
            }

        }
    }
}