package com.netflix.dgs.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.netflix.dgs.plugin.hints.DgsComponentInspector

class DgsComponentInspectorTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return DefaultLightProjectDescriptor()
    }

    override fun setUp() {
        super.setUp()
        loadLibrary(project, module, "graphql-dgs", "graphql-dgs-4.9.2.jar")

    }

    fun testMissingDgsComponentAnnotation() {
        myFixture.configureByFile("MissingDgsComponent.java")
        myFixture.enableInspections(DgsComponentInspector::class.java)

        myFixture.checkHighlighting(true, false, false, true)
        myFixture.launchAction(myFixture.findSingleIntention("A class should be annotated @DgsComponent when DGS annotations are used within the class"))
        myFixture.checkResultByFile("FixedMissingDgsComponent.java")
    }


    private fun loadLibrary(projectDisposable: Disposable, module: Module, libraryName: String, libraryJarName: String) {
        PsiTestUtil.addLibrary(projectDisposable, module, libraryName, "src/test/testData/lib/", libraryJarName)
    }

    override fun getTestDataPath() = "src/test/testData/dgscomponenthint"


}
