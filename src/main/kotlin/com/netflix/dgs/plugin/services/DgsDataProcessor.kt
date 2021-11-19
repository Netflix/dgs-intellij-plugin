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
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.containers.orNull
import com.netflix.dgs.plugin.DgsDataFetcher
import com.netflix.dgs.plugin.DgsEntityFetcher
import com.netflix.dgs.plugin.services.internal.GraphQLSchemaRegistry

class DgsDataProcessor(private val graphQLSchemaRegistry: GraphQLSchemaRegistry, private val dgsComponentIndex: DgsComponentIndex) : Processor<PsiAnnotation> {
    override fun process(psiAnnotation: PsiAnnotation): Boolean {
        val psiMethod = PsiTreeUtil.getParentOfType(psiAnnotation, PsiMethod::class.java)
        if(psiMethod != null) {

            if (DgsDataFetcher.isDataFetcherAnnotation(psiAnnotation)) {
                val parentType = DgsDataFetcher.getParentType(psiMethod)
                val field = DgsDataFetcher.getField(psiMethod)

                //Because we use the stubs index, we might process a @DgsQuery annotation as @DgsData as well, which won't have parentType.
                if (parentType != null) {
                    val dgsDataFetcher = DgsDataFetcher(
                            parentType,
                            field,
                            psiMethod,
                            psiAnnotation,
                            psiAnnotation.containingFile,
                            graphQLSchemaRegistry.psiForSchemaType(psiMethod, parentType, field)?.orNull()
                    )

                    dgsComponentIndex.dataFetchers.add(dgsDataFetcher)
                }
            } else if (DgsEntityFetcher.isEntityFetcherAnnotation(psiAnnotation)) {
                val parentType = DgsEntityFetcher.getParentType(psiMethod)
                val field = DgsEntityFetcher.getField(psiMethod)

                val dgsEntityFetcher = DgsEntityFetcher(
                        parentType,
                        field,
                        psiMethod,
                        psiAnnotation,
                        psiAnnotation.containingFile,
                        graphQLSchemaRegistry.psiForSchemaType(psiMethod, parentType, field)?.orNull()
                )

                dgsComponentIndex.entityFetchers.add(dgsEntityFetcher)
            }
        }

        return true
    }
}