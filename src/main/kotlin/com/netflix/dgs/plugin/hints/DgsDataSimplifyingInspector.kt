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
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.MyBundle
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJavaToKotlinConverter.Companion.addImports
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class DgsDataSimplifyingInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitMethod(node: UMethod): Boolean {
                if (node.hasAnnotation(DGS_DATA_ANNOTATION)) {
                    val dgsDataAnnotation = node.getAnnotation(DGS_DATA_ANNOTATION)
                    val parentTypeValue =
                        (dgsDataAnnotation?.findAttribute("parentType")?.attributeValue as JvmAnnotationConstantValue).constantValue
                    if (parentTypeValue == "Query" || parentTypeValue == "Mutation" || parentTypeValue == "Subscription") {
                        val message = MyBundle.getMessage(
                            "dgs.inspection.dgsdata.simplify",
                            parentTypeValue,
                            "@Dgs${parentTypeValue}"
                        )
                        holder.registerProblem(
                            dgsDataAnnotation.toUElement()?.sourcePsi!!,
                            message,
                            ProblemHighlightType.WEAK_WARNING,
                            DgsDataQuickFix("@Dgs${parentTypeValue}", message)
                        )
                    }
                }

                return super.visitMethod(node)
            }
        }, false)
    }

    companion object {
        const val DGS_DATA_ANNOTATION = "com.netflix.graphql.dgs.DgsData"
    }

    class DgsDataQuickFix(private val newAnnotation: String, private val fixName: String) : LocalQuickFix {
        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return fixName
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val factory: PsiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
            val file = descriptor.psiElement.parentOfType<PsiFile>()
            val method = descriptor.psiElement.toUElement()?.getParentOfType<UMethod>()

            val annotationFQN = "com.netflix.graphql.dgs.${newAnnotation.substringAfter("@")}"

            if(file is PsiJavaFile) {
                val importStatement = factory.createImportStatement(factory.createTypeByFQClassName(annotationFQN).resolve()!!)
                val importList = file.importList
                if(importList?.importStatements?.any { it.qualifiedName == annotationFQN } == false) {
                    importList.add(importStatement)
                }

                method?.sourcePsi?.addBefore(factory.createAnnotationFromText("@DgsQuery", null), descriptor.psiElement)
            } else if(file is KtFile) {
                (method?.sourcePsi as KtFunction).addAnnotation(FqName(annotationFQN))
            }

            descriptor.psiElement.delete()
        }

    }
}