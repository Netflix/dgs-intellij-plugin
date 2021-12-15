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
        Optional<ObjectTypeDefinition> objectType = getTypeDefinition(registry, parentType);
        if (objectType.isPresent()) {
            Optional<FieldDefinition> schemaField = objectType.get().getFieldDefinitions().stream().filter(f -> f.getName().equals(field)).findAny();
            if (schemaField.isPresent()) {
                return Optional.ofNullable(schemaField.get().getSourceLocation().getElement());
            }
        } else if ("_entities".equals(parentType)) {
            Optional<ObjectTypeDefinition> entitiesType = getTypeDefinition(registry, field);
            if (entitiesType.isPresent()) {
                return Optional.ofNullable(entitiesType.get().getElement());
            }
        } else {
            Optional<InterfaceTypeDefinition> interfaceType = getInterfaceTypeDefinition(registry, parentType);
            if (interfaceType.isPresent()) {
                Optional<FieldDefinition> schemaField = interfaceType.get().getFieldDefinitions().stream().filter(f -> f.getName().equals(field)).findAny();
                if (schemaField.isPresent()) {
                    return Optional.ofNullable(schemaField.get().getSourceLocation().getElement());
                }
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

    private Optional<ObjectTypeDefinition> getTypeDefinition(TypeDefinitionRegistry registry, String schemaType) {
        Optional<ObjectTypeDefinition> objectTypeDefinition = registry.getType(schemaType, ObjectTypeDefinition.class);
        if (objectTypeDefinition.isPresent()) {
            return Optional.ofNullable(objectTypeDefinition.get());
        }

        List<ObjectTypeExtensionDefinition> objectTypeExtensionDefinitions = registry.objectTypeExtensions().get(schemaType);
        if (objectTypeExtensionDefinitions != null && !objectTypeExtensionDefinitions.isEmpty()) {
            return  Optional.ofNullable(objectTypeExtensionDefinitions.get(0));
        }
        return Optional.empty();
    }

    private Optional<InterfaceTypeDefinition> getInterfaceTypeDefinition(TypeDefinitionRegistry registry, String schemaType) {
        Optional<InterfaceTypeDefinition> interfaceTypeDefinition = registry.getType(schemaType, InterfaceTypeDefinition.class);
        if (interfaceTypeDefinition.isPresent()) {
            return Optional.ofNullable(interfaceTypeDefinition.get());
        }

        List<InterfaceTypeExtensionDefinition> interfaceTypeExtensionDefinitions = registry.interfaceTypeExtensions().get(schemaType);
        if (interfaceTypeExtensionDefinitions != null && !interfaceTypeExtensionDefinitions.isEmpty()) {
            return  Optional.ofNullable(interfaceTypeExtensionDefinitions.get(0));
        }
        return Optional.empty();
    }

    private TypeDefinitionRegistry getRegistry(@NotNull PsiElement psiElement) {
        return GraphQLSchemaProvider.getInstance(project)
                .getRegistryInfo(psiElement).getTypeDefinitionRegistry();
    }
}