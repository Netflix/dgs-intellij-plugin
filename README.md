# dgs-intellij-plugin

![Build](https://github.com/Netflix/dgs-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/17852-dgs.svg)](https://plugins.jetbrains.com/plugin/17852-dgs)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/17852-dgs.svg)](https://plugins.jetbrains.com/plugin/17852-dgs)

<!-- Plugin description -->
This plugin helps to build GraphQL applications in Java using the DGS framework.
The [DGS Framework](https://github.com/Netflix/dgs-framework) is open sourced by Netflix and builds on Spring Boot.

The plugin integrates with the [JS GraphQL](https://plugins.jetbrains.com/plugin/8097-js-graphql) plugin and adds DGS specific features such as code intentions and data fetcher navigation.
<!-- Plugin description end -->

## Configuration with .graphqlconfig
The DGS IntelliJ plugin works with the underlying [JS GraphQL IntelliJ Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) to support additional functionality provided by the DGS Framework. The JS GraphQL plugin accepts a `.graphqlconfig` file for specifying different parameters such as schema location and project setup.

> **Note**
> When using a `.graphqlconfig` file, it's important that schema locations are specified using `includes` so that your application's schema is found correctly. If you use `schemaPath`, you will run into errors because you are overriding all of the possible schema locations. If you find errors about missing type definitions or federation directives, it's likely as a result of this issue.


## Status

The plugin is currently under early development and considered experimental.

