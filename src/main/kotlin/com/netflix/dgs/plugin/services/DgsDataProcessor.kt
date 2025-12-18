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

package com.netflix.dgs.plugin.services

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.containers.orNull
import com.netflix.dgs.plugin.*
import com.netflix.dgs.plugin.services.internal.GraphQLSchemaRegistry
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType

class DgsComponentProcessor(
    private val graphQLSchemaRegistry: GraphQLSchemaRegistry,
    private val dgsComponentIndex: DgsComponentIndex
) : Processor<UAnnotation> {

    // Track methods that have been processed with @DgsData.List to avoid duplicate processing
    private val processedDataFetcherMethods = mutableSetOf<PsiElement>()

    override fun process(uAnnotation: UAnnotation): Boolean {

        val uMethod = uAnnotation.getParentOfType<UMethod>()

        if (uMethod != null) {
            when {
                DgsDataFetcher.isDataFetcherAnnotation(uAnnotation) -> processDataFetcher(uMethod, uAnnotation)
                DgsEntityFetcher.isEntityFetcherAnnotation(uAnnotation) -> processEntityFetcher(uMethod, uAnnotation)
                DgsRuntimeWiring.isDgsRuntimeWiringAnnotation(uAnnotation) -> processRuntimeWiring(uMethod, uAnnotation)
            }
        } else {
            val uClass = uAnnotation.getParentOfType<UClass>()
            if (uClass != null) {
                when {
                    DgsDataLoader.isDataLoaderAnnotation(uAnnotation) -> processDataLoader(uClass, uAnnotation)
                    DgsDirective.isDgsDirectiveAnnotation(uAnnotation) -> processDirective(uAnnotation, uClass)
                    DgsScalar.isDgsScalarAnnotation(uAnnotation) -> processScalar(uAnnotation, uClass)
                }
            }
        }

        return true
    }

    private fun processScalar(uAnnotation: UAnnotation, uClass: UClass) {
        val nameFromAnnotation = DgsDirective.getNameFromAnnotation(uAnnotation)
        if (nameFromAnnotation != null) {
            val dgsScalar = DgsScalar(
                nameFromAnnotation,
                uClass.sourcePsi!!,
                uAnnotation.sourcePsi!!,
                uAnnotation.sourcePsi!!.containingFile,
                graphQLSchemaRegistry.psiForScalar(uAnnotation.sourcePsi!!, nameFromAnnotation).orNull()
            )

            dgsComponentIndex.scalars.add(dgsScalar)
        }
    }

    private fun processRuntimeWiring(uMethod: UMethod, uAnnotation: UAnnotation) {
        val dgsRuntimeWiring = DgsRuntimeWiring(
            uMethod.name,
            uMethod.sourcePsi!!,
            uAnnotation.sourcePsi!!,
            uMethod.sourcePsi!!.containingFile
        )
        dgsComponentIndex.runtimeWirings.add(dgsRuntimeWiring)
    }

    private fun processDirective(uAnnotation: UAnnotation, uClass: UClass) {
        val nameFromAnnotation = DgsDirective.getNameFromAnnotation(uAnnotation)
        if (nameFromAnnotation != null) {
            val dgsDirective = DgsDirective(
                nameFromAnnotation,
                uClass.sourcePsi!!,
                uAnnotation.sourcePsi!!,
                uAnnotation.sourcePsi?.containingFile!!,
                graphQLSchemaRegistry.psiForDirective(uAnnotation.sourcePsi!!, nameFromAnnotation).orNull()
            )

            dgsComponentIndex.directives.add(dgsDirective)
        }
    }

    private fun processDataLoader(uClass: UClass, uAnnotation: UAnnotation) {
        val nameFromAnnotation = DgsDataLoader.getNameFromAnnotation(uAnnotation)
        if (nameFromAnnotation != null) {
            val dgsDataLoader = DgsDataLoader(
                nameFromAnnotation,
                uClass.sourcePsi!!,
                uAnnotation.sourcePsi!!,
                uAnnotation.sourcePsi?.containingFile!!
            )
            dgsComponentIndex.dataLoaders.add(dgsDataLoader)
        }
    }

    private fun processEntityFetcher(uMethod: UMethod, uAnnotation: UAnnotation) {
        val field = DgsEntityFetcher.getName(uMethod)

        val dgsEntityFetcher = DgsEntityFetcher(
            field,
            uMethod.sourcePsi!!,
            uAnnotation.sourcePsi!!,
            uAnnotation.sourcePsi?.containingFile!!,
            graphQLSchemaRegistry.psiForSchemaType(uMethod, "_entities", field)?.orNull()
        )

        dgsComponentIndex.entityFetchers.add(dgsEntityFetcher)
    }

    private fun processDataFetcher(uMethod: UMethod, uAnnotation: UAnnotation) {
        val methodPsi = uMethod.sourcePsi!!

        // Only process from @DgsData.List if the current annotation IS the list itself (explicit @DgsData.List)
        // If the current annotation is @DgsData and a list exists, it's implicit @Repeatable - process the individual annotation
        if (uAnnotation.qualifiedName == "com.netflix.graphql.dgs.DgsData.List") {
            // Explicit @DgsData.List - process all annotations from the container
            val listAnnotation = uAnnotation.sourcePsi as? PsiAnnotation
            if (listAnnotation != null) {
                // Check if we've already processed this method's @DgsData.List to avoid duplicates
                if (processedDataFetcherMethods.contains(methodPsi)) {
                    return
                }
                processedDataFetcherMethods.add(methodPsi)

                // Access the value attribute which contains the array of @DgsData annotations
                val valueAttribute = listAnnotation.findAttributeValue("value")
                val annotations = when (valueAttribute) {
                    is PsiArrayInitializerMemberValue -> {
                        // Directly access array elements to preserve individual annotation values
                        valueAttribute.initializers.filterIsInstance<PsiAnnotation>()
                    }
                    is PsiAnnotation -> {
                        // Single annotation case
                        listOf(valueAttribute)
                    }
                    else -> {
                        // Fallback to tree search
                        PsiTreeUtil.findChildrenOfType(listAnnotation, PsiAnnotation::class.java).toList()
                    }
                }

                annotations.forEach {
                    val parentType = DgsDataFetcher.getParentType(it)
                    val field = DgsDataFetcher.getFieldFromAnnotation(it)?:uMethod.name

                    //Because we use the stubs index, we might process a @DgsQuery annotation as @DgsData as well, which won't have parentType.
                    if (parentType != null) {
                        val dgsDataFetcher = DgsDataFetcher(
                            parentType,
                            field,
                            methodPsi,
                            it,
                            uAnnotation.sourcePsi?.containingFile!!,
                            graphQLSchemaRegistry.psiForSchemaType(uMethod, parentType, field)?.orNull()
                        )

                        dgsComponentIndex.dataFetchers.add(dgsDataFetcher)

                        // If parentType is an interface, also create entries for all implementing types
                        val implementingTypes = graphQLSchemaRegistry.getTypesImplementingInterface(uMethod, parentType)
                        implementingTypes.forEach { implementingType ->
                            val implDgsDataFetcher = DgsDataFetcher(
                                implementingType,
                                field,
                                methodPsi,
                                it,
                                uAnnotation.sourcePsi?.containingFile!!,
                                graphQLSchemaRegistry.psiForSchemaType(uMethod, implementingType, field)?.orNull()
                            )
                            dgsComponentIndex.dataFetchers.add(implDgsDataFetcher)
                        }
                    }
                }
            }
        } else {
            // Individual @DgsData annotation - process it directly
            // This handles both single @DgsData and implicit @Repeatable cases
            val annotationPsi = uAnnotation.sourcePsi as? PsiAnnotation
            if (annotationPsi != null) {
                val parentType = DgsDataFetcher.getParentType(annotationPsi)
                val field = DgsDataFetcher.getFieldFromAnnotation(annotationPsi) ?: uMethod.name

                //Because we use the stubs index, we might process a @DgsQuery annotation as @DgsData as well, which won't have parentType.
                if (parentType != null) {
                    val dgsDataFetcher = DgsDataFetcher(
                        parentType,
                        field,
                        methodPsi,
                        annotationPsi,
                        uAnnotation.sourcePsi?.containingFile!!,
                        graphQLSchemaRegistry.psiForSchemaType(uMethod, parentType, field)?.orNull()
                    )

                    dgsComponentIndex.dataFetchers.add(dgsDataFetcher)

                    // If parentType is an interface, also create entries for all implementing types
                    val implementingTypes = graphQLSchemaRegistry.getTypesImplementingInterface(uMethod, parentType)
                    implementingTypes.forEach { implementingType ->
                        val implDgsDataFetcher = DgsDataFetcher(
                            implementingType,
                            field,
                            methodPsi,
                            annotationPsi,
                            uAnnotation.sourcePsi?.containingFile!!,
                            graphQLSchemaRegistry.psiForSchemaType(uMethod, implementingType, field)?.orNull()
                        )
                        dgsComponentIndex.dataFetchers.add(implDgsDataFetcher)
                    }
                }
            }
        }
    }
}