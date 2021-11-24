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

package com.netflix.dgs.plugin.provider

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.netflix.dgs.plugin.DgsConstants
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class DgsImplicitUsageProvider: ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        val uElement = element.toUElement()
        if(uElement is UMethod) {
            return uElement.annotations.any { DgsConstants.dgsAnnotations.contains(it.qualifiedName) }
        }

        return false
    }

    override fun isImplicitRead(element: PsiElement) = false
    override fun isImplicitWrite(element: PsiElement) = false
}