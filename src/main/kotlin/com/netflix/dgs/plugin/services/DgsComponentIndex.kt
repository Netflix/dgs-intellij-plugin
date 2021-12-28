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

import com.netflix.dgs.plugin.*
import java.util.concurrent.CopyOnWriteArraySet

class DgsComponentIndex {
    val dataFetchers: MutableSet<DgsDataFetcher> = CopyOnWriteArraySet()
    val entityFetchers: MutableSet<DgsEntityFetcher> = CopyOnWriteArraySet()
    val scalars: MutableSet<DgsScalar> = CopyOnWriteArraySet()
    val runtimeWirings: MutableSet<DgsRuntimeWiring> = CopyOnWriteArraySet()
    val customContexts: MutableSet<DgsCustomContext> = CopyOnWriteArraySet()
    val directives: MutableSet<DgsDirective> = CopyOnWriteArraySet()
    val dataLoaders: MutableSet<DgsDataLoader> = CopyOnWriteArraySet()

    fun getAllComponents(): Set<NamedNavigationComponent> {
        return dataFetchers.asSequence()
            .plus(entityFetchers.asSequence())
            .plus(scalars.asSequence())
            .plus(runtimeWirings.asSequence())
            .plus(customContexts.asSequence())
            .plus(directives.asSequence())
            .plus(dataLoaders.asSequence()).toSet()
    }
}