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

package com.netflix.dgs.plugin.services;

import com.intellij.psi.PsiFile;
import com.netflix.dgs.plugin.DgsDataFetcher;
import com.netflix.dgs.plugin.DgsEntityFetcher;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DgsComponentIndex {
    private final Set<DgsDataFetcher> dataFetchers = new CopyOnWriteArraySet<>();
    private final Set<DgsEntityFetcher> entityFetchers = new CopyOnWriteArraySet<>();

    public Set<DgsDataFetcher> getDataFetchers() {
        return dataFetchers;
    }

    public Set<DgsEntityFetcher> getEntityFetchers() {
        return entityFetchers;
    }

    public void fileUpdated(PsiFile psiFile) {
        dataFetchers.removeIf(dgsDataFetcher -> dgsDataFetcher.getPsiFile() == psiFile);
    }
}
