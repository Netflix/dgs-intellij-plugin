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

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Paths

abstract class DgsTestCase : LightJavaCodeInsightFixtureTestCase5() {

    @BeforeEach
    fun setUp() {
        loadLibrary(fixture.project, fixture.module, "com.netflix.graphql.dgs:graphql-dgs", "graphql-dgs-4.9.2.jar")
    }

    private fun loadLibrary(
        projectDisposable: Disposable,

        module: Module,
        libraryName: String,
        libraryJarName: String
    ) {
        PsiTestUtil.addLibrary(projectDisposable, module, libraryName, "src/test/testdata/lib/", libraryJarName)
    }

    override fun getTestDataPath() = Paths.get("src/test/testdata/" + this::class.java.simpleName).toAbsolutePath().toString()
}