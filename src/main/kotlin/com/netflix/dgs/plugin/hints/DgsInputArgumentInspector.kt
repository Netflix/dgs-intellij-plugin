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
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastVisitorAdapter
import com.netflix.dgs.plugin.MyBundle
import com.netflix.dgs.plugin.services.DgsService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

@Suppress("UElementAsPsi")
class DgsInputArgumentInspector : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {
            override fun visitMethod(node: UMethod): Boolean {

                    if (hasDgsAnnotation(node) ) {
                        val dgsDataAnnotation = getDgsAnnotation(node)
                        val dgsService = dgsDataAnnotation.project.getService(DgsService::class.java)
                        val dgsDataFetcher = dgsService.dgsComponentIndex.dataFetchers.find { it.psiAnnotation.toUElement() == dgsDataAnnotation.toUElement() }
                        if (dgsDataFetcher?.schemaPsi != null) {
                            val isJavaFile = dgsDataFetcher?.psiFile is PsiJavaFile
                            val arguments = (dgsDataFetcher?.schemaPsi as GraphQLFieldDefinitionImpl).argumentsDefinition?.inputValueDefinitionList
                            if (arguments != null && arguments.size > 0 && !node.uastParameters.any { it.hasAnnotation(DGS_INPUT_ARGUMENT_ANNOTATION) }) {
                                val inputArgumentsHint: String = getHintForInputArgument(arguments[0], isJavaFile)
                                val inputArgumentsList = mutableListOf<String>()
                                arguments.forEach {
                                    val parameter = getHintForInputArgument(it, isJavaFile)
                                    inputArgumentsList.add(parameter)
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

    private fun getHintForInputArgument(input: GraphQLInputValueDefinition, isJavaFile: Boolean) : String {
        return if (isJavaFile) {
            getHintForInputArgumentInJava(input)
        } else {
            getHintForInputArgumentInKotlin(input)
        }
    }

    private fun getHintForInputArgumentInJava(input: GraphQLInputValueDefinition) : String {
        val argName = (input.nameIdentifier as GraphQLIdentifierImpl).name
        val inputArgumentHint = StringBuilder("@InputArgument ")
        if (input.type is GraphQLListType) {
            val collectionType = getCollectionType(input.type, true)
            if (! isSimpleType(collectionType)) {
                inputArgumentHint.append("(collectionType=$collectionType.class) ")
            }
        }
       inputArgumentHint.append(getType(input.type, true)  + " " + argName)
        return inputArgumentHint.toString()
    }

    private fun getHintForInputArgumentInKotlin(input: GraphQLInputValueDefinition) : String {
        val argName = (input.nameIdentifier as GraphQLIdentifierImpl).name
        val inputArgumentHint = StringBuilder("@InputArgument ")
        if (input.type is GraphQLListType) {
            val collectionType = getCollectionType(input.type, false)
            if (! isSimpleType(collectionType)) {
                inputArgumentHint.append("(collectionType=$collectionType) ")
            }
        }
        inputArgumentHint.append(argName + ": "+ getType(input.type, false)  + " ")
        return inputArgumentHint.toString()
    }

    private fun isSimpleType(typeName: String) : Boolean {
        return typeName == "String" || typeName == "Integer" || typeName == "Int" || typeName == "Boolean" || typeName == "Double"
    }

    private fun getType(inputType: GraphQLType?, isJavaType: Boolean) : String {
        return when (inputType) {
            is GraphQLTypeName -> {
                val rawType = getRawType((inputType as PsiNamedElement).name!!, isJavaType)
                if (isJavaType) {
                    rawType
                } else {
                    "$rawType?"
                }
            }
            is GraphQLListType -> {
                val type = "List<" + getType(inputType.type, isJavaType) + ">"
                if (isJavaType) {
                    type
                } else {
                    "$type?"
                }
            }
            is GraphQLNonNullType -> {
                val type = getType(inputType.type, isJavaType)
                if (isJavaType) {
                    type
                } else {
                    type.removeSuffixIfPresent("?")
                }
            }
            else -> ""
        }
    }

    private fun getCollectionType(inputType: GraphQLType?, isJavaFile: Boolean) : String {
        return when (inputType) {
            is GraphQLTypeName -> {
                getRawType((inputType as PsiNamedElement).name!!, isJavaFile)
            }
            is GraphQLListType -> {
                getType(inputType.type, isJavaFile)
            }
            is GraphQLNonNullType -> {
                getType(inputType.type, isJavaFile)
            }
            else -> ""
        }
    }

    private fun getRawType(typeName: String, isJavaFile: Boolean) : String {

        if ( isJavaFile && (typeName == "Int" || typeName == "IntValue")) {
            return "Integer"
        } else if (typeName == "Int" || typeName == "IntValue") {
            return "Int"
        }

        val type = when (typeName) {
                "String" -> "String"
                "StringValue" -> "String"
                "Float" -> "Double"
                "FloatValue" -> "Double"
                "Boolean" -> "Boolean"
                "BooleanValue" -> "Boolean"
                "ID" -> "String"
                "IDValue" -> "String"
                "LocalTime" -> "LocalTime"
                "LocalDate" -> "LocalDate"
                "LocalDateTime" -> "LocalDateTime"
                "TimeZone" -> "String"
                "Date" -> "LocalDate"
                "DateTime" -> "OffsetDateTime"
                "Time" -> "OffsetTime"
                "Currency" -> "Currency"
                "Instant" -> "Instant"
                "RelayPageInfo" -> "PageInfo"
                "PageInfo" -> "PageInfo"
                "JSON" -> "Object"
                "Url" -> "URL"
                else -> typeName
            }
        return type
    }

    companion object {
        private const val DGS_DATA_ANNOTATION = "com.netflix.graphql.dgs.DgsData"
        private const val DGS_QUERY_ANNOTATION = "com.netflix.graphql.dgs.DgsQuery"
        private const val DGS_MUTATION_ANNOTATION = "com.netflix.graphql.dgs.DgsMutation"
        private const val DGS_SUBSCRIPTION_ANNOTATION = "com.netflix.graphql.dgs.DgsSubscription"
        const val DGS_INPUT_ARGUMENT_ANNOTATION = "com.netflix.graphql.dgs.InputArgument"

        fun hasDgsAnnotation(node: UMethod) : Boolean {
            return(node.hasAnnotation(DGS_QUERY_ANNOTATION) || node.hasAnnotation(DGS_SUBSCRIPTION_ANNOTATION) || node.hasAnnotation(DGS_MUTATION_ANNOTATION)
                    || node.hasAnnotation(DGS_DATA_ANNOTATION))
        }

        fun getDgsAnnotation(node: UMethod) : PsiAnnotation {
            val annotation = if (node.getAnnotation(DGS_DATA_ANNOTATION) != null) {
                node.getAnnotation(DGS_DATA_ANNOTATION)
            } else if (node.getAnnotation(DGS_QUERY_ANNOTATION) != null) {
                node.getAnnotation(DGS_QUERY_ANNOTATION)
            } else if (node.getAnnotation(DGS_MUTATION_ANNOTATION) != null) {
                node.getAnnotation(DGS_MUTATION_ANNOTATION)
            } else {
                node.getAnnotation(DGS_SUBSCRIPTION_ANNOTATION)
            }
            return annotation!!
        }
    }


    class DgsInputArgumentQuickFix(private val methodPointer: SmartPsiElementPointer<UMethod>, private val newInputArguments: List<String>, private val fixName: String) : LocalQuickFix {
        override fun getFamilyName(): String {
            return name
        }

        override fun getName(): String {
            return fixName
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            val method = methodPointer.element
            if(method == null) {
                return
            }

            val file = descriptor.psiElement.parentOfType<PsiFile>()

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
            }
        }
    }
}