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
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil
import com.intellij.lang.jvm.JvmAnnotation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightPsiClassBuilder
import com.intellij.psi.util.PsiUtil
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.MyBundle
import org.jetbrains.uast.UClass
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor
import java.util.*


class DgsComponentInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitClass(node: UClass): Boolean {
                val hasDgsComponentAnnotation: Boolean =
                    node.javaPsi.hasAnnotation("com.netflix.graphql.dgs.DgsComponent")
                if (Arrays.stream(node.methods)
                        .anyMatch { m ->
                            dgsAnnotations.stream().anyMatch { annotation -> m.javaPsi.hasAnnotation(annotation) }
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

    companion object {
        private val log = Logger.getInstance("dgs")
        private val dgsAnnotations = setOf(
            "com.netflix.graphql.dgs.DgsData",
            "com.netflix.graphql.dgs.DgsQuery",
            "com.netflix.graphql.dgs.DgsMutation",
            "com.netflix.graphql.dgs.DgsSubscription",
            "com.netflix.graphql.dgs.DgsRuntimeWiring",
            "com.netflix.graphql.dgs.DgsScalar",
            "com.netflix.graphql.dgs.DgsCodeRegistry",
            "com.netflix.graphql.dgs.DgsDefaultTypeResolver",
            "com.netflix.graphql.dgs.DgsDirective",
            "com.netflix.graphql.dgs.DgsEntityFetcher",
            "com.netflix.graphql.dgs.DgsTypeDefinitionRegistry",
            "com.netflix.graphql.dgs.DgsTypeResolver",
        )
    }

    object DgsComponentQuickfix : LocalQuickFix {
        override fun getFamilyName(): @IntentionFamilyName String {
            return name
        }

        override fun getName(): String {
            return MyBundle.message("dgs.inspection.missing.component.annotation")
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val clazz: PsiClass = descriptor.psiElement.parent as PsiClass
            val factory: PsiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
            val annotationFromText: PsiAnnotation = factory.createAnnotationFromText("@DgsComponent", null)


            val importStatement = factory.createImportStatement(factory.createTypeByFQClassName("com.netflix.graphql.dgs.DgsComponent").resolve()!!)
            val importList = (clazz.containingFile as PsiJavaFile).importList
            if(importList?.importStatements?.any { it.qualifiedName == "com.netflix.graphql.dgs.DgsComponent" } == false) {
                importList.add(importStatement)
            }

            clazz.modifierList?.addBefore(annotationFromText, clazz.modifierList!!.firstChild)
        }
    }
}