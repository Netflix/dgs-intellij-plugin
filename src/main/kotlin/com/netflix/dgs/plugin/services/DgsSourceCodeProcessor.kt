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

class DgsSourceCodeProcessor(
    private val dgsComponentIndex: DgsComponentIndex,
    private val typeDefinitionRegistry: TypeDefinitionRegistry
) : Processor<PsiFile> {
    override fun process(file: PsiFile): Boolean {
        PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .filter(DgsDataFetcher.Companion::isDataFetcherMethod)
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


        return true
    }
}