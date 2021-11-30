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

package com.netflix.dgs.plugin.services.internal;

import com.intellij.lang.jsgraphql.schema.GraphQLSchemaProvider;
import com.intellij.lang.jsgraphql.types.language.*;
import com.intellij.lang.jsgraphql.types.schema.idl.TypeDefinitionRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class GraphQLSchemaRegistry {

    private final Project project;

    public GraphQLSchemaRegistry(Project project) {
        this.project = project;
    }

    public @Nullable
    Optional<PsiElement> psiForSchemaType(@NotNull PsiElement psiElement, @Nullable String parentType, @Nullable String field) {

        TypeDefinitionRegistry registry = getRegistry(psiElement);
        ObjectTypeDefinition type = getTypeDefinition(registry, parentType);
        if (type != null) {
            Optional<FieldDefinition> schemaField = type.getFieldDefinitions().stream().filter(f -> f.getName().equals(field)).findAny();
            if (schemaField.isPresent()) {
                return Optional.ofNullable(schemaField.get().getSourceLocation().getElement());
            }
        } else if ("_entities".equals(parentType)) {
            type = getTypeDefinition(registry, field);
            if (type != null) {
                return Optional.ofNullable(type.getElement());
            } else {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public Optional<PsiElement> psiForDirective(@NotNull PsiElement psiElement, @NotNull String name) {
        TypeDefinitionRegistry registry = getRegistry(psiElement);
        Optional<DirectiveDefinition> directiveDefinition = registry.getDirectiveDefinition(name);
        return directiveDefinition.map(AbstractNode::getElement);
    }

    public Optional<PsiElement> psiForScalar(@NotNull PsiElement psiElement, @NotNull String name) {
        TypeDefinitionRegistry registry = getRegistry(psiElement);
        ScalarTypeDefinition scalarTypeDefinition = registry.scalars().get(name);
        if (scalarTypeDefinition != null) {
            return Optional.ofNullable(scalarTypeDefinition.getElement());
        } else {
            return Optional.empty();
        }
    }

    private ObjectTypeDefinition getTypeDefinition(TypeDefinitionRegistry registry, String schemaType) {

        List<ObjectTypeExtensionDefinition> objectTypeExtensionDefinitions = registry.objectTypeExtensions().get(schemaType);
        ObjectTypeDefinition objectType = null;
        if (objectTypeExtensionDefinitions != null && !objectTypeExtensionDefinitions.isEmpty()) {
            objectType = objectTypeExtensionDefinitions.get(0);
        } else {
            Optional<ObjectTypeDefinition> objectTypeDefinition = registry.getType(schemaType, ObjectTypeDefinition.class);
            if (objectTypeDefinition.isPresent()) {
                objectType = objectTypeDefinition.get();
            }
        }
        return objectType;
    }

    private TypeDefinitionRegistry getRegistry(@NotNull PsiElement psiElement) {
        return GraphQLSchemaProvider.getInstance(project)
                .getRegistryInfo(psiElement).getTypeDefinitionRegistry();
    }
}