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

import com.netflix.dgs.plugin.hints.DgsInputArgumentValidationInspector
import org.junit.jupiter.api.Test

class DgsInputArgumentValidationInspectorTest : DgsTestCase() {
    @Test
    fun testIncorrectInputArgumentSimpleTypes() {
        myFixture.configureByFiles("java/IncorrectSimpleTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument String testString"))
        myFixture.checkResultByFile("java/FixedSimpleTypes.java")
    }

    @Test
    fun testIncorrectInputArgumentSimpleTypesForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectSimpleTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument testString: String?"))
        myFixture.checkResultByFile("kotlin/FixedSimpleTypes.kt")
    }

    @Test
    fun testIncorrectInputArgumentSimpleListTypes() {
        myFixture.configureByFiles("java/IncorrectSimpleListTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument List<Integer> testIntegers"))
        myFixture.checkResultByFile("java/FixedSimpleListTypes.java")
    }

    @Test
    fun testIncorrectInputArgumentSimpleListTypesForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectSimpleListTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument testIntegers: List<Int?>?"))
        myFixture.checkResultByFile("kotlin/FixedSimpleListTypes.kt")
    }

    @Test
    fun testIncorrectInputArgumentSimpleNonNullableTypes() {
        myFixture.configureByFiles("java/IncorrectSimpleNonNullableTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument Integer testInteger"))
        myFixture.checkResultByFile("java/FixedSimpleNonNullableTypes.java")
    }

    @Test
    fun testIncorrectInputArgumentSimpleNonNullableTypesForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectSimpleNonNullableTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument testInteger: Int"))
        myFixture.checkResultByFile("kotlin/FixedSimpleNonNullableTypes.kt")
    }

    @Test
    fun testIncorrectInputArgumentEnumType() {
        myFixture.configureByFiles("java/IncorrectEnumType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument (collectionType=Colors.class) Colors testEnum"))
        myFixture.checkResultByFile("java/FixedEnumType.java")
    }

    @Test
    fun testIncorrectInputArgumentEnumTypeForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectEnumType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument (collectionType=Colors::class) testEnum: Colors"))
        myFixture.checkResultByFile("kotlin/FixedEnumType.kt")
    }

    @Test
    fun testIncorrectInputArgumentComplexType() {
        myFixture.configureByFiles("java/IncorrectComplexType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument TestInput testInput"))
        myFixture.checkResultByFile("java/FixedComplexType.java")
    }

    @Test
    fun testIncorrectInputArgumentComplexTypeForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectComplexType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument testInput: TestInput?"))
        myFixture.checkResultByFile("kotlin/FixedComplexType.kt")
    }

    @Test
    fun testIncorrectInputArgumentCollectionType() {
        myFixture.configureByFiles("java/IncorrectCollectionType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument (collectionType=TestInput.class) List<TestInput> testInput"))
        myFixture.checkResultByFile("java/FixedCollectionType.java")
    }

    @Test
    fun testIncorrectInputArgumentCollectionTypeForKotlin() {
        myFixture.configureByFiles("kotlin/IncorrectCollectionType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("Fix annotation to @InputArgument (collectionType=TestInput::class) testNonNullableInput: List<TestInput>"))
        myFixture.checkResultByFile("kotlin/FixedCollectionType.kt")
    }

    @Test
    fun testIncorrectInputArgumentName() {
        myFixture.configureByFiles("java/IncorrectInputArgumentName.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
    }

    @Test
    fun testIncorrectInputArgumentWithIncorrectNameAttribute() {
        myFixture.configureByFiles("java/IncorrectInputArgumentNameAttribute.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentValidationInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
    }
}
