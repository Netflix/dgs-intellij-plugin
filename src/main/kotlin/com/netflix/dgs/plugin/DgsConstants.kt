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

import com.intellij.openapi.util.IconLoader

object DgsConstants {
    val dgsAnnotations = setOf(
        "com.netflix.graphql.dgs.DgsData",
        "com.netflix.graphql.dgs.DgsQuery",
        "com.netflix.graphql.dgs.DgsMutation",
        "com.netflix.graphql.dgs.DgsSubscription",
        "com.netflix.graphql.dgs.DgsRuntimeWiring",
        "com.netflix.graphql.dgs.DgsScalar",
        "com.netflix.graphql.dgs.DgsCodeRegistry",
        "com.netflix.graphql.dgs.DgsDefaultTypeResolver",
        "com.netflix.graphql.dgs.DgsDirective",
        "com.netflix.graphql.dgs.DgsEntityFetcher",
        "com.netflix.graphql.dgs.DgsTypeDefinitionRegistry",
        "com.netflix.graphql.dgs.DgsTypeResolver",
    )

    val dgsIcon = IconLoader.getIcon("/icons/dgs.svg", this::class.java)
}