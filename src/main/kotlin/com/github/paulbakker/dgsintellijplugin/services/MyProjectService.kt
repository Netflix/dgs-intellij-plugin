package com.github.paulbakker.dgsintellijplugin.services

import com.intellij.openapi.project.Project
import com.github.paulbakker.dgsintellijplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
