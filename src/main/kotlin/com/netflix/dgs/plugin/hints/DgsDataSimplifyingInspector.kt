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
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor
import kotlin.system.measureTimeMillis

class DgsDataSimplifyingInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitMethod(node: UMethod): Boolean {
                val time = measureTimeMillis {

                    if (node.hasAnnotation(DGS_DATA_ANNOTATION)) {
                        val dgsDataAnnotation = node.getAnnotation(DGS_DATA_ANNOTATION)
                        val parentTypeAttribute = dgsDataAnnotation?.findAttribute("parentType")
                        val parentTypeValue =
                            if (parentTypeAttribute != null && parentTypeAttribute.attributeValue != null) {
                                (parentTypeAttribute.attributeValue as JvmAnnotationConstantValue).constantValue
                            } else null

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
                }

                println("DgsDataSimplifyingInspector took $time ms")
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
            val fieldValue = (descriptor.psiElement.toUElement() as UAnnotation).findAttributeValue("field")?.evaluateString()
            val method = descriptor.psiElement.toUElement()?.getParentOfType<UMethod>()

            val annotationFQN = "com.netflix.graphql.dgs.${newAnnotation.substringAfter("@")}"

            if(file is PsiJavaFile) {
                val importStatement = factory.createImportStatement(factory.createTypeByFQClassName(annotationFQN).resolve()!!)
                val importList = file.importList
                if(importList?.importStatements?.any { it.qualifiedName == annotationFQN } == false) {
                    importList.add(importStatement)
                }

                val newAnnotation = if(fieldValue != null && fieldValue != method?.name) {
                    factory.createAnnotationFromText("${newAnnotation}(field = \"$fieldValue\")", null)
                } else {
                    factory.createAnnotationFromText(newAnnotation, null)
                }

                method?.sourcePsi?.addBefore(newAnnotation, method.modifierList)

                project.getService(DgsService::class.java).clearCache()
            } else if(file is KtFile) {
                    if(fieldValue != null && fieldValue != method?.name) {
                        (method?.sourcePsi as KtFunction).addAnnotation(FqName(annotationFQN), "field = \"${fieldValue}\"")
                    } else {
                        (method?.sourcePsi as KtFunction).addAnnotation(FqName(annotationFQN))
                    }
            }

            descriptor.psiElement.delete()
        }

    }
}