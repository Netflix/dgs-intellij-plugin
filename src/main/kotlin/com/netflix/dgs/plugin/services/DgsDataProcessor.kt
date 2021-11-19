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

import com.intellij.util.Processor
import com.intellij.util.containers.orNull
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher
import com.netflix.dgs.plugin.services.internal.GraphQLSchemaRegistry
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType

class UDgsDataProcessor(
    private val graphQLSchemaRegistry: GraphQLSchemaRegistry,
    private val dgsComponentIndex: DgsComponentIndex
) : Processor<UAnnotation> {
    override fun process(uAnnotation: UAnnotation): Boolean {

        val uMethod = uAnnotation.getParentOfType<UMethod>()

        if (uMethod != null) {
            if(DgsDataFetcher.isDataFetcherAnnotation(uAnnotation)) {

                val parentType = DgsDataFetcher.getParentType(uMethod)
                val field = DgsDataFetcher.getField(uMethod)

                //Because we use the stubs index, we might process a @DgsQuery annotation as @DgsData as well, which won't have parentType.
                if (parentType != null) {
                    val dgsDataFetcher = DgsDataFetcher(
                        parentType,
                        field,
                        uMethod.sourcePsi!!,
                        uAnnotation.sourcePsi!!,
                        uAnnotation.sourcePsi?.containingFile!!,
                        graphQLSchemaRegistry.psiForSchemaType(uMethod, parentType, field)?.orNull()
                    )

                    dgsComponentIndex.dataFetchers.add(dgsDataFetcher)
                }
            } else if (DgsEntityFetcher.isEntityFetcherAnnotation(uAnnotation)) {
                val field = DgsEntityFetcher.getName(uMethod)

                val dgsEntityFetcher = DgsEntityFetcher(
                    field,
                    uMethod?.sourcePsi!!,
                    uAnnotation.sourcePsi!!,
                    uAnnotation.sourcePsi?.containingFile!!,
                    graphQLSchemaRegistry.psiForSchemaType(uMethod, "_entities", field)?.orNull()
                )

                dgsComponentIndex.entityFetchers.add(dgsEntityFetcher)
            }
        }

        return true
    }
}