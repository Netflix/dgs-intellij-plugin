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

import com.intellij.openapi.application.ReadAction
import com.netflix.dgs.plugin.services.DgsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for multiple @DgsData annotations on a single resolver method.
 * This validates that when a method resolves the same field for multiple types
 * (especially types implementing an interface), each annotation correctly links
 * to its specific type's field in the schema.
 */
class DgsDataMultipleAnnotationsTest : DgsTestCase() {

    /**
     * Tests implicit @Repeatable syntax: multiple @DgsData annotations.
     * This is the preferred syntax where Java automatically creates @DgsData.List container.
     */
    @Test
    fun testImplicitRepeatableAnnotations() {
        fixture.configureByFiles("schema.graphql", "MultipleAnnotationsDataFetcher.java")

        ReadAction.run<Exception> {
            val dgsService = fixture.project.getService(DgsService::class.java)
            val dataFetchers = dgsService.dgsComponentIndex.dataFetchers

            // Test title field - should have 2 data fetchers from implicit @Repeatable
            val titleFetchers = dataFetchers.filter { it.field == "title" }
            assertEquals(2, titleFetchers.size, "Expected 2 data fetchers for 'title' field")

            // Verify Movie.title links correctly
            val movieTitleFetcher = titleFetchers.find { it.parentType == "Movie" }
            assertNotNull(movieTitleFetcher, "Should have data fetcher for Movie.title")
            assertEquals("title", movieTitleFetcher!!.field)
            assertEquals("Movie", movieTitleFetcher.parentType)
            assertNotNull(movieTitleFetcher.schemaPsi, "Movie.title should link to schema")

            // Verify Show.title links correctly
            val showTitleFetcher = titleFetchers.find { it.parentType == "Show" }
            assertNotNull(showTitleFetcher, "Should have data fetcher for Show.title")
            assertEquals("title", showTitleFetcher!!.field)
            assertEquals("Show", showTitleFetcher.parentType)
            assertNotNull(showTitleFetcher.schemaPsi, "Show.title should link to schema")

            // Both should reference the same method
            assertEquals(movieTitleFetcher.psiMethod, showTitleFetcher.psiMethod,
                "Both data fetchers should reference the same method")

            // Verify each annotation PSI is distinct (critical for correct navigation)
            assertNotEquals(movieTitleFetcher.psiAnnotation, showTitleFetcher.psiAnnotation,
                "Each @DgsData annotation should have its own distinct PSI element")

            // Test contentAdvisory field - should have 4 data fetchers from implicit @Repeatable
            val advisoryFetchers = dataFetchers.filter { it.field == "contentAdvisory" }
            assertEquals(4, advisoryFetchers.size, "Expected 4 data fetchers for 'contentAdvisory' field")

            // Verify each type has correct linkage
            val types = listOf("Movie", "Show", "Season", "Episode")
            types.forEach { typeName ->
                val fetcher = advisoryFetchers.find { it.parentType == typeName }
                assertNotNull(fetcher, "Should have data fetcher for $typeName.contentAdvisory")
                assertEquals("contentAdvisory", fetcher!!.field)
                assertEquals(typeName, fetcher.parentType)
                assertNotNull(fetcher.schemaPsi, "$typeName.contentAdvisory should link to schema")
            }

            // All should reference the same method
            val methodReferences = advisoryFetchers.map { it.psiMethod }.distinct()
            assertEquals(1, methodReferences.size, "All data fetchers should reference the same method")
        }
    }

    /**
     * Tests explicit @DgsData.List syntax.
     * This is an alternative syntax that should work identically to implicit @Repeatable.
     */
    @Test
    fun testExplicitDgsDataList() {
        fixture.configureByFiles("schema.graphql", "ExplicitListDataFetcher.java")

        ReadAction.run<Exception> {
            val dgsService = fixture.project.getService(DgsService::class.java)
            val dataFetchers = dgsService.dgsComponentIndex.dataFetchers

            // Test title field - should have 2 data fetchers from explicit @DgsData.List
            val titleFetchers = dataFetchers.filter { it.field == "title" }
            assertEquals(2, titleFetchers.size, "Expected 2 data fetchers for 'title' field")

            // Verify Movie.title
            val movieFetcher = titleFetchers.find { it.parentType == "Movie" }
            assertNotNull(movieFetcher, "Should have data fetcher for Movie.title")
            assertEquals("title", movieFetcher!!.field)
            assertEquals("Movie", movieFetcher.parentType)
            assertNotNull(movieFetcher.schemaPsi, "Movie.title should link to schema")

            // Verify Show.title
            val showFetcher = titleFetchers.find { it.parentType == "Show" }
            assertNotNull(showFetcher, "Should have data fetcher for Show.title")
            assertEquals("title", showFetcher!!.field)
            assertEquals("Show", showFetcher.parentType)
            assertNotNull(showFetcher.schemaPsi, "Show.title should link to schema")

            // Both should reference the same method
            assertEquals(movieFetcher.psiMethod, showFetcher.psiMethod,
                "Both data fetchers should reference the same method")

            // Test contentAdvisory field - should have 4 data fetchers
            val advisoryFetchers = dataFetchers.filter { it.field == "contentAdvisory" }
            assertEquals(4, advisoryFetchers.size, "Expected 4 data fetchers for 'contentAdvisory' field")

            // Verify each type has correct linkage
            val types = listOf("Movie", "Show", "Season", "Episode")
            types.forEach { typeName ->
                val fetcher = advisoryFetchers.find { it.parentType == typeName }
                assertNotNull(fetcher, "Should have data fetcher for $typeName.contentAdvisory")
                assertEquals("contentAdvisory", fetcher!!.field)
                assertEquals(typeName, fetcher.parentType)
                assertNotNull(fetcher.schemaPsi, "$typeName.contentAdvisory should link to schema")
            }

            // All should reference the same method
            val methodReferences = advisoryFetchers.map { it.psiMethod }.distinct()
            assertEquals(1, methodReferences.size, "All data fetchers should reference the same method")
        }
    }
}