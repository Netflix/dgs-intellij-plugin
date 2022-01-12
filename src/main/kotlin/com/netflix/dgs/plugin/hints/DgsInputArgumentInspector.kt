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
import com.intellij.lang.jsgraphql.psi.impl.GraphQLFieldDefinitionImpl
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.InputArgumentUtils
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

@Suppress("UElementAsPsi")
class DgsInputArgumentInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {

            override fun visitMethod(node: UMethod): Boolean {

                if (InputArgumentUtils.hasDgsAnnotation(node) ) {
                        val dgsDataAnnotation = InputArgumentUtils.getDgsAnnotation(node)
                        val dgsService = dgsDataAnnotation.project.getService(DgsService::class.java)
                        val typeDefinitionRegistry = GraphQLSchemaProvider.getInstance(dgsDataAnnotation.project).getRegistryInfo(node.navigationElement).typeDefinitionRegistry

                    val dgsDataFetcher = dgsService.dgsComponentIndex.dataFetchers.find { it.psiAnnotation.toUElement() == dgsDataAnnotation.toUElement() }
                        if (dgsDataFetcher?.schemaPsi != null) {
                            val isJavaFile = dgsDataFetcher?.psiFile is PsiJavaFile
                            val arguments = (dgsDataFetcher?.schemaPsi as GraphQLFieldDefinitionImpl).argumentsDefinition?.inputValueDefinitionList
                            if (arguments != null && arguments.size > 0 && !node.uastParameters.any { it.hasAnnotation(InputArgumentUtils.DGS_INPUT_ARGUMENT_ANNOTATION) }) {
                                val inputArgumentsHint: String = InputArgumentUtils.getHintForInputArgument(arguments[0], typeDefinitionRegistry,  isJavaFile)
                                val inputArgumentsList = mutableListOf<String>()
                                arguments.forEach {
                                    // do not add a hint for custom scalar types
                                    if (! InputArgumentUtils.isCustomScalarType(it.type!!, typeDefinitionRegistry)) {
                                        val parameter = InputArgumentUtils.getHintForInputArgument(it, typeDefinitionRegistry, isJavaFile)
                                        inputArgumentsList.add(parameter)
                                    }
                                }

                                val message = MyBundle.getMessage(
                                    "dgs.inspection.dgsinputargument.hint",
                                    inputArgumentsHint
                                )

                                val pointer = SmartPointerManager.createPointer(node.toUElement() as UMethod)
                                node.identifyingElement?.let {
                                    holder.registerProblem(it.navigationElement,
                                            message,
                                            ProblemHighlightType.WEAK_WARNING,
                                            DgsInputArgumentQuickFix(pointer, inputArgumentsList, message)
                                    )
                                }
                            }
                        }
                    }
                return super.visitMethod(node)
            }
        }, false)
    }


    class DgsInputArgumentQuickFix(private val methodPointer: SmartPsiElementPointer<UMethod>, private val newInputArguments: List<String>, private val fixName: String) : LocalQuickFix {
        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return fixName
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            val method = methodPointer.element ?: return

            val file = methodPointer.element?.sourcePsi?.parentOfType<PsiFile>()
            if(file is PsiJavaFile) {
                val factory: PsiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
                newInputArguments.forEach {
                    val param = factory.createParameterFromText(it, method)
                    method.parameterList.add(param)
                }
                project.getService(DgsService::class.java).clearCache()
            } else if(file is KtFile) {
                val psiFactory = KtPsiFactory(project)
                newInputArguments.forEach {
                    val param = psiFactory.createParameter(it)
                        (method.sourcePsi as KtFunction).valueParameterList?.addParameter(param)
                }
                project.getService(DgsService::class.java).clearCache()
            }
        }
    }
}