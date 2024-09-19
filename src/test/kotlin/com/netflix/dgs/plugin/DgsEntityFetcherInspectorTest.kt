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

import com.netflix.dgs.plugin.hints.DgsEntityFetcherInspector
import org.junit.jupiter.api.Test


class DgsEntityFetcherInspectorTest : DgsTestCase() {

    @Test
    fun testMissingEntityFetcher() {
        fixture.configureByFiles("FederatedEntity.graphql", "MissingDgsEntityFetcher.java")
        fixture.enableInspections(DgsEntityFetcherInspector::class.java)

        fixture.checkHighlighting(true, false, true, true)
    }

    @Test
    fun testWithResolvableFalse() {
        fixture.configureByFiles("FederatedEntityWithResolvableFalse.graphql", "MissingDgsEntityFetcher.java")
        fixture.enableInspections(DgsEntityFetcherInspector::class.java)

        fixture.checkHighlighting()
    }
}
