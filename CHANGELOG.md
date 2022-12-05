<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# dgs-intellij-plugin Changelog

## [Unreleased]


## [1.2.5]
### Fixed
* Updated to work with IntelliJ build 223

## [1.2.4]
### Fixed
* Updated to work with IntelliJ build 222
* Removed validation on deprecated collection type argument when using @InputArgument for collections and enums.

## [1.2.3]
### Fixed
* Address NPE on DgsInputArgumentValidationInspector:143

## [1.2.2]
### Fixed
* Address NPE on DgsInputArgumentInspector on UMethod.

## [1.2.1]
### Added
* Update the gradle intelliJ plugin build. 2) DGS plugin to work with 2022.1 EAP builds

### Fixed
* Fix validation of kotlin collection type for enums and lists of complex types.


## [1.2.0]
### Added
* Suggest adding `@InputArgument` to methods when the query/mutation has input arguments defined in the schema
* Hint and quick fix incorrectly typed `@InputArgument`
* Search DGS components in find symbol (cmd-alt-o)
*
* Only activate plugin for DGS projects

### Fixed
* `@DgsData.List` is now supported and no longer causes an error
* Don't block rendering of project view while indexing
* Use updated DGS icon

## [1.1.0]
### Added
* A DGS section in the Project tree that lists all the various DGS components in a project
* Navigation schema <-> @DgsDirective@DgsScalar

## [1.0.0]
### Added
* Navigation from a schema file to data fetchers in Java/Kotlin code
* Navigation from data fetchers back to the schema definitions
* A hint/quickfix for when a @DgsComponent@DgsData(parentType=Query)@DgsQueryMutationSubscription@key@DgsEntityFetchercom.netflix.graphql.dgs:graphql-dgs

