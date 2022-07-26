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
import com.intellij.lang.jsgraphql.psi.*
import com.intellij.lang.jsgraphql.psi.impl.GraphQLFieldDefinitionImpl
import com.intellij.lang.jsgraphql.psi.impl.GraphQLIdentifierImpl
import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider
import com.intellij.lang.jsgraphql.types.schema.idl.TypeDefinitionRegistry
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastVisitorAdapter
import com.intellij.util.containers.isEmpty
import com.netflix.dgs.plugin.InputArgumentUtils
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor
import kotlin.streams.toList

@Suppress("UElementAsPsi")
class DgsInputArgumentValidationInspector : AbstractBaseUastLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitMethod(node: UMethod): Boolean {

                if (InputArgumentUtils.hasDgsAnnotation(node) ) {
                    val dgsDataAnnotation = InputArgumentUtils.getDgsAnnotation(node)
                    val dgsService = dgsDataAnnotation.project.getService(DgsService::class.java)
                    val typeDefinitionRegistry = GraphQLSchemaProvider.getInstance(dgsDataAnnotation.project).getRegistryInfo(node.navigationElement).typeDefinitionRegistry

                    val dgsDataFetcher = dgsService.dgsComponentIndex.dataFetchers.find { it.psiAnnotation.toUElement() == dgsDataAnnotation.toUElement() }
                    if (dgsDataFetcher?.schemaPsi != null) {
                        val isJavaFile = dgsDataFetcher.psiFile is PsiJavaFile
                        val arguments = (dgsDataFetcher.schemaPsi as GraphQLFieldDefinitionImpl).argumentsDefinition?.inputValueDefinitionList
                        if (arguments != null && arguments.size > 0) {

                            // validate each argument specified in the schema against the data fetcher's input arguments
                            node.uastParameters.stream().filter { it.hasAnnotation(InputArgumentUtils.DGS_INPUT_ARGUMENT_ANNOTATION) }.toList().forEach { iter ->
                                val inputArgName = getInputArgumentName(iter)
                                val schemaSearchResult = arguments.stream().filter { graphQLInputValueDefinition ->
                                    inputArgName == (graphQLInputValueDefinition.nameIdentifier as GraphQLIdentifierImpl).name }.toList()

                                // if the list is empty, there is no corresponding input argument with the same name defined in the schema
                                if (schemaSearchResult.isEmpty()) {
                                    val inputArgumentsList = mutableListOf<String>()
                                    var validArgumentNames = ""
                                    // construct a list of valid argument names for the hint and fix
                                    arguments.forEach {
                                        validArgumentNames = validArgumentNames.plus((it.nameIdentifier as GraphQLIdentifierImpl).name).plus(", ")
                                    }
                                    validArgumentNames = validArgumentNames.removeSuffix(", ")

                                    val message = MyBundle.getMessage(
                                            "dgs.inspection.dgsinputargumentnamevalidation.hint",
                                            inputArgName,
                                            validArgumentNames
                                    )
                                    registerProblemWithArgumentName(holder, node, iter, inputArgumentsList, message)
                                } else {
                                    // validate the type of the input argument
                                    val graphQLInputValueDefinition = schemaSearchResult[0]
                                    val inputArgument = iter

                                        val fixedInputArgument = InputArgumentUtils.getHintForInputArgument(graphQLInputValueDefinition!!, typeDefinitionRegistry, isJavaFile)
                                        // enable hinting only if the argument is not a custom scalar or if it does not have the expected type
                                        if (!InputArgumentUtils.isCustomScalarType(graphQLInputValueDefinition.type!!, typeDefinitionRegistry)
                                                && !hasExpectedAnnotation(graphQLInputValueDefinition, inputArgument, typeDefinitionRegistry, isJavaFile)) {
                                            val message = MyBundle.getMessage(
                                                    "dgs.inspection.dgsinputargumentvalidation.hint",
                                                    fixedInputArgument
                                            )
                                            registerProblemWithArgumentType(holder, node, inputArgument, message, fixedInputArgument)
                                        }
                                }
                            }
                        }
                    }
                }
                return super.visitMethod(node)
            }
        }, false)
    }

    private fun getInputArgumentName(param: UParameter) : String {
        val nameValue = param.getAnnotation(InputArgumentUtils.DGS_INPUT_ARGUMENT_ANNOTATION)?.findAttribute("name")?.attributeValue
        if (nameValue != null) {
            val jvmConstant = nameValue as JvmAnnotationConstantValue
            if (jvmConstant.constantValue != null) {
                return jvmConstant.constantValue as String
            }
        }
        return param.name
    }

    private fun hasExpectedAnnotation(graphQLInput: GraphQLInputValueDefinition, inputArgument: UParameter, typeDefinitionRegistry: TypeDefinitionRegistry, isJavaFile: Boolean) : Boolean {
        val inputArgumentAnnotation = inputArgument.getAnnotation(InputArgumentUtils.DGS_INPUT_ARGUMENT_ANNOTATION)
        if (inputArgumentAnnotation != null) {
            // Parse the raw type from the input argument and verify match
            val inputArgumentType = inputArgument.text.removePrefix(inputArgumentAnnotation.text).replace(inputArgument.name, "").replace(":", "").trim()
            val expectedType = InputArgumentUtils.getType(graphQLInput.type!!, isJavaFile)
            return expectedType == inputArgumentType
        }

        return false
    }

    private fun registerProblemWithArgumentType(
        holder: ProblemsHolder,
        node: UMethod,
        inputArgument: UParameter,
        message: String,
        fixedInputArgument: String
    ) {
        when(val element = node?.toUElement()){
            is UMethod -> {
                val pointer = SmartPointerManager.createPointer(element)
                node.identifyingElement?.let {
                    holder.registerProblem(
                        inputArgument.navigationElement,
                        message,
                        ProblemHighlightType.WEAK_WARNING,
                        DgsInputArgumentQuickFix(pointer, fixedInputArgument, fixedInputArgument)
                    )
                }
            }
        }
    }

    private fun registerProblemWithArgumentName(holder: ProblemsHolder, node: UMethod, inputArgument: UParameter, inputArgumentsList: List<String>, message: String) {
        node.identifyingElement?.let {
            holder.registerProblem(
                inputArgument.navigationElement,
                message,
                ProblemHighlightType.WEAK_WARNING
            )
        }
    }


    class DgsInputArgumentQuickFix(private val methodPointer: SmartPsiElementPointer<UMethod>, private val newInputArgument: String, private val fixedArgument: String) : LocalQuickFix {
        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return "Fix annotation to $fixedArgument"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val method = methodPointer.element ?: return
            val file = methodPointer.element?.sourcePsi?.parentOfType<PsiFile>()
            if(file is PsiJavaFile) {
                val factory: PsiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
                    val param = factory.createParameterFromText(newInputArgument, method)
                    descriptor.psiElement.replace(param)
                project.getService(DgsService::class.java).clearCache()
            } else if(file is KtFile) {
                val psiFactory = KtPsiFactory(project)
                val param = psiFactory.createParameter(newInputArgument)
                descriptor.psiElement.replace(param)
                project.getService(DgsService::class.java).clearCache()
            }
        }
    }
}
