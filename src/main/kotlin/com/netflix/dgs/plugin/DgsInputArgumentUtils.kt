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

import com.intellij.lang.jsgraphql.psi.*
import com.intellij.lang.jsgraphql.psi.impl.GraphQLIdentifierImpl
import com.intellij.lang.jsgraphql.types.language.EnumTypeDefinition
import com.intellij.lang.jsgraphql.types.language.ScalarTypeDefinition
import com.intellij.lang.jsgraphql.types.schema.idl.TypeDefinitionRegistry
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.uast.UMethod

object InputArgumentUtils {

    private const val DGS_DATA_ANNOTATION = "com.netflix.graphql.dgs.DgsData"
    private const val DGS_QUERY_ANNOTATION = "com.netflix.graphql.dgs.DgsQuery"
    private const val DGS_MUTATION_ANNOTATION = "com.netflix.graphql.dgs.DgsMutation"
    private const val DGS_SUBSCRIPTION_ANNOTATION = "com.netflix.graphql.dgs.DgsSubscription"
    const val DGS_INPUT_ARGUMENT_ANNOTATION = "com.netflix.graphql.dgs.InputArgument"

    private val knownTypes = mapOf("String" to "String",
        "StringValue" to "String",
        "Float" to "Double",
        "FloatValue" to "Double",
        "Boolean" to "Boolean",
        "BooleanValue" to "Boolean",
        "ID" to "String",
        "IDValue" to "String",
        "LocalTime" to "LocalTime",
        "LocalDate" to "LocalDate",
        "LocalDateTime" to "LocalDateTime",
        "TimeZone" to "String",
        "Date" to "LocalDate",
        "DateTime" to "OffsetDateTime",
        "Time" to "OffsetTime",
        "Currency" to "Currency",
        "Instant" to "Instant",
        "RelayPageInfo" to "PageInfo",
        "PageInfo" to "PageInfo",
        "JSON" to "Object",
        "Url" to "URL")

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

    fun getHintForInputArgument(input: GraphQLInputValueDefinition, typeRegistry: TypeDefinitionRegistry, isJavaFile: Boolean) : String {
        return if (isJavaFile) {
            getHintForInputArgumentInJava(input, typeRegistry)
        } else {
            getHintForInputArgumentInKotlin(input, typeRegistry)
        }
    }

    private fun getHintForInputArgumentInJava(input: GraphQLInputValueDefinition, typeRegistry: TypeDefinitionRegistry) : String {
        val argName = (input.nameIdentifier as GraphQLIdentifierImpl).name
        val inputArgumentHint = StringBuilder("@InputArgument ")

        inputArgumentHint.append(getType(input.type!!, true) + " " + argName)
        return inputArgumentHint.toString()

    }

    private fun getHintForInputArgumentInKotlin(input: GraphQLInputValueDefinition, typeRegistry: TypeDefinitionRegistry) : String {
        val argName = (input.nameIdentifier as GraphQLIdentifierImpl).name
        val inputArgumentHint = StringBuilder("@InputArgument ")

        inputArgumentHint.append(argName + ": "+ getType(input.type!!, false)  + " ")
        return inputArgumentHint.toString()
    }

    private fun isSimpleType(typeName: String) : Boolean {
        return typeName == "String" || typeName == "Integer" || typeName == "Int" || typeName == "Boolean" || typeName == "Double"
    }

    private fun isEnumType(inputType: GraphQLType, typeRegistry: TypeDefinitionRegistry) : Boolean {
        if (inputType is GraphQLTypeName) {
            val type = typeRegistry.types()[(inputType as PsiNamedElement).name]
            return type != null && type is EnumTypeDefinition || typeRegistry.enumTypeExtensions().containsKey((inputType as PsiNamedElement).name)
        }
        return false
    }

    fun isCustomScalarType(inputType: GraphQLType, typeRegistry: TypeDefinitionRegistry) : Boolean {
        if (inputType is GraphQLTypeName) {
            val inputTypeName = (inputType as PsiNamedElement).name!!
            if (knownTypes.containsKey(inputTypeName) || inputTypeName == "Int" || inputTypeName == "IntValue") {
                return false
            }
            return typeRegistry.scalars().contains(inputTypeName) ||  typeRegistry.scalarTypeExtensions().containsKey(inputTypeName)
        }
        return false
    }

    private fun isListType(inputType: GraphQLType) : Boolean {
        return when (inputType) {
            is GraphQLTypeName -> false
            is GraphQLListType -> true
            is GraphQLNonNullType -> {
                return isListType(inputType.type)
            }
            else -> false
        }
    }

    fun getType(inputType: GraphQLType, isJavaType: Boolean) : String {
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

    private fun getRawType(typeName: String, isJavaFile: Boolean) : String {
        if ( isJavaFile && (typeName == "Int" || typeName == "IntValue")) {
            return "Integer"
        } else if (typeName == "Int" || typeName == "IntValue") {
            return "Int"
        }

        val type = if (knownTypes.containsKey(typeName)) {
            knownTypes[typeName]!!
        } else {
            typeName
        }
        return type
    }
}
