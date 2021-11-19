<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# dgs-intellij-plugin Changelog

## [Unreleased]
### Added
* Navigation from a schema file to data fetchers in Java/Kotlin code
* Navigation from data fetchers back to the schema definitions
* A hint/quickfix for when a `@DgsComponent` annotation is missing
* A hint/quickfix to simplify `@DgsData(parentType="Query")` to `@DgsQuery`. The same for `Mutation` and `Subscription`.
* A hint when a federated type is defined (using the `@key` directive) and no `@DgsEntityFetcher` is defined for the type.
* The plugin is recommended in Intellij for projects using the `com.netflix.graphql.dgs:graphql-dgs` dependency. 

All the listed functionality is supported both for Java and Kotlin.

