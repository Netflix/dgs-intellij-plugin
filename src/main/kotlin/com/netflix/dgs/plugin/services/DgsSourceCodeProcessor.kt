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

import com.intellij.lang.jsgraphql.types.language.ObjectTypeDefinition
import com.intellij.lang.jsgraphql.types.schema.idl.TypeDefinitionRegistry
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.containers.orNull
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher

class DgsSourceCodeProcessor(
    private val dgsComponentIndex: DgsComponentIndex,
    private val typeDefinitionRegistry: TypeDefinitionRegistry
) : Processor<PsiFile> {
    override fun process(file: PsiFile): Boolean {
        val children = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
        children
            .filter(DgsDataFetcher.Companion::isDataFetcherMethod )
            .map { method ->
                val parentType = DgsDataFetcher.getParentType(method)
                val graphQLType = typeDefinitionRegistry.objectTypeExtensions()[parentType]?.get(0)
                    ?: typeDefinitionRegistry.getType(parentType, ObjectTypeDefinition::class.java)
                        .orNull()

                val fieldName = DgsDataFetcher.getField(method)
                val fieldDefinition = graphQLType?.fieldDefinitions?.find { it.name == fieldName }
                val schemaPsi = fieldDefinition?.sourceLocation?.element

                DgsDataFetcher(parentType, fieldName, method, DgsDataFetcher.getDataFetcherAnnotation(method), file, schemaPsi)

            }
            .forEach(dgsComponentIndex.dataFetchers::add)

        children
            .filter(DgsEntityFetcher.Companion::isEntityFetcherMethod)
            .map { method ->
                val parentType = DgsEntityFetcher.getParentType(method)

                val fieldName = DgsEntityFetcher.getField(method)
                val graphQLType = typeDefinitionRegistry.objectTypeExtensions()[fieldName]?.get(0)
                    ?: typeDefinitionRegistry.getType(fieldName, ObjectTypeDefinition::class.java)
                        .orNull()
                val schemaPsi = graphQLType?.sourceLocation?.element

                DgsEntityFetcher(parentType, fieldName, method, DgsEntityFetcher.getEntityFetcherAnnotation(method), file, schemaPsi)
            }
            .forEach(dgsComponentIndex.entityFetchers::add)


        return true
    }
}