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

import com.netflix.dgs.plugin.hints.DgsInputArgumentInspector
// Temporarily comment tests due to flaky behavior in checkHighlighting warnings
// Tests complain about missing java.lang.String intermittently
/*
class DgsInputArgumentInspectorTest : DgsTestCase() {

    fun testMissingInputArgumentSimpleTypes() {
        myFixture.configureByFiles("java/MissingSimpleTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument String testString"))
        myFixture.checkResultByFile("java/FixedSimpleTypes.java")
    }

    fun testMissingInputArgumentSimpleTypesForKotlin() {
        myFixture.configureByFiles("kotlin/MissingSimpleTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument testString: String?"))
        myFixture.checkResultByFile("kotlin/FixedSimpleTypes.kt")
    }

    fun testMissingInputArgumentSimpleNonNullableTypes() {
        myFixture.configureByFiles("java/MissingSimpleNonNullableTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument String testString"))
        myFixture.checkResultByFile("java/FixedSimpleNonNullableTypes.java")
    }

    fun testMissingInputArgumentSimpleNonNullableTypesForKotlin() {
        myFixture.configureByFiles("kotlin/MissingSimpleNonNullableTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument testString: String"))
        myFixture.checkResultByFile("kotlin/FixedSimpleNonNullableTypes.kt")
    }

    fun testMissingInputArgumentEnumType() {
        myFixture.configureByFiles("java/MissingEnumType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument (collectionType=Colors.class) Colors testEnum"))
        myFixture.checkResultByFile("java/FixedEnumType.java")
    }

    fun testMissingInputArgumentEnumTypeForKotlin() {
        myFixture.configureByFiles("kotlin/MissingEnumType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument (collectionType=Colors) testEnum: Colors?"))
        myFixture.checkResultByFile("kotlin/FixedEnumType.kt")
    }

    fun testMissingInputArgumentComplexType() {
        myFixture.configureByFiles("java/MissingComplexType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument TestInput testInput"))
        myFixture.checkResultByFile("java/FixedComplexType.java")
    }

    fun testMissingInputArgumentComplexTypeForKotlin() {
        myFixture.configureByFiles("kotlin/MissingComplexType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument testInput: TestInput?"))
        myFixture.checkResultByFile("kotlin/FixedComplexType.kt")
    }

    fun testMissingInputArgumentCollectionType() {
        myFixture.configureByFiles("java/MissingCollectionType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument (collectionType=TestInput.class) List<TestInput> testInput"))
        myFixture.checkResultByFile("java/FixedCollectionType.java")
    }

    fun testMissingInputArgumentCollectionTypeForKotlin() {
        myFixture.configureByFiles("kotlin/MissingCollectionType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument (collectionType=TestInput) testInput: List<TestInput?>?"))
        myFixture.checkResultByFile("kotlin/FixedCollectionType.kt")
    }

    fun testMissingInputArgumentSimpleListTypes() {
        myFixture.configureByFiles("java/MissingSimpleListTypes.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument List<String> testString"))
        myFixture.checkResultByFile("java/FixedSimpleListTypes.java")
    }

    fun testMissingInputArgumentSimpleListTypesForKotlin() {
        myFixture.configureByFiles("kotlin/MissingSimpleListTypes.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument testStrings: List<String?>?"))
        myFixture.checkResultByFile("kotlin/FixedSimpleListTypes.kt")
    }

    fun testMissingInputArgumentScalarType() {
        myFixture.configureByFiles("java/MissingScalarType.java", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument OffsetDateTime testScalar"))
        myFixture.checkResultByFile("java/FixedScalarType.java")
    }

    fun testMissingInputArgumentScalarTypeForKotlin() {
        myFixture.configureByFiles("kotlin/MissingScalarType.kt", "InputArguments.graphql")

        myFixture.enableInspections(DgsInputArgumentInspector::class.java)
        myFixture.checkHighlighting(true, false, true, true)
        myFixture.launchAction(myFixture.findSingleIntention("You can use @InputArgument to extract parameters, e.g. @InputArgument testScalar: OffsetDateTime?"))
        myFixture.checkResultByFile("kotlin/FixedScalarType.kt")
    }

}
*/