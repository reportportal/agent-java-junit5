# Changelog

## [Unreleased]
### Changed
- Client version updated on [5.2.14](https://github.com/reportportal/client-java/releases/tag/5.2.14), by @HardNorth
- Do not hide test description when test is disabled with reason, by @HardNorth

## [5.3.1]
### Added
- `@Description` annotation support, by @oleksandr-fomenko
### Changed
- Client version updated on [5.2.13](https://github.com/reportportal/client-java/releases/tag/5.2.13), by @HardNorth
### Removed
- JSR 305 dependency, by @HardNorth

## [5.3.0]
### Added
- `finishTest` method which controls finishing Tests with statuses, to distinguish them from suites and configuration methods, by @HardNorth
### Changed
- JUnit 5 dependency marked as `compileOnly` to avoid overriding JUnit 5 API version for users, by @HardNorth
- JSR 305 dependency marked as `implementation` to force users specify their own versions, by @HardNorth
- Client version updated on [5.2.5](https://github.com/reportportal/client-java/releases/tag/5.2.5), by @HardNorth
### Removed
- Setting of Unique ID on Test Step and Configuration start, as redundant action, by @HardNorth

## [5.2.1]
### Changed
- Client version updated on [5.2.4](https://github.com/reportportal/client-java/releases/tag/5.2.4), by @HardNorth
### Removed
- `commons-model` dependency to rely on `clinet-java` exclusions in security fixes, by @HardNorth

## [5.2.0]
### Changed
- Client version updated on [5.2.0](https://github.com/reportportal/client-java/releases/tag/5.2.0), by @HardNorth
- JUnit 5 dependency marked as `implementation` to force users specify their own versions, by @HardNorth
### Removed
- Deprecated code, by @HardNorth

## [5.1.11]
### Changed
- Client version updated on [5.1.24](https://github.com/reportportal/client-java/releases/tag/5.1.24), by @HardNorth

## [5.1.10]
### Changed
- Client version updated on [5.1.22](https://github.com/reportportal/client-java/releases/tag/5.1.22), by @HardNorth

## [5.1.9]
### Changed
- Slf4j version updated on version 2.0.4 to support newer versions of Logback with security fixes, by @HardNorth

## [5.1.8]
### Changed
- Client version updated on [5.1.21](https://github.com/reportportal/client-java/releases/tag/5.1.21), by @HardNorth

## [5.1.7]
### Added
- Test constructor exception handling, by @HardNorth
### Changed
- Client version updated on [5.1.18](https://github.com/reportportal/client-java/releases/tag/5.1.18), by @HardNorth

## [5.1.6]
### Changed
- Client version updated on [5.1.16](https://github.com/reportportal/client-java/releases/tag/5.1.16), by @HardNorth

## [5.1.5]
### Changed
- Client version updated on [5.1.11](https://github.com/reportportal/client-java/releases/tag/5.1.11), by @HardNorth
### Removed
- `ReportPortalExtension` does not implement `AfterEachCallback` anymore, by @HardNorth
- `finally` keyword, see [JEP 421](https://openjdk.java.net/jeps/421), by @HardNorth
### Fixed
- Issue [#106](https://github.com/reportportal/agent-java-junit5/issues/106): Legacy Assumptions support, by @HardNorth

## [5.1.4]
### Added
- Assumptions handling in before methods, issue [#102](https://github.com/reportportal/agent-java-junit5/issues/102), by @HardNorth

## [5.1.3]
### Added
- Assumptions handling, issue [#102](https://github.com/reportportal/agent-java-junit5/issues/102), by @HardNorth
### Removed
- Suite item status reporting (server calculates it itself), by @HardNorth
### Changed
- Client version updated on [5.1.10](https://github.com/reportportal/client-java/releases/tag/5.1.10), by @HardNorth

## [5.1.2]
### Added
- Test Case ID templating, by @HardNorth
### Changed
- Client version updated on [5.1.9](https://github.com/reportportal/client-java/releases/tag/5.1.9), by @HardNorth
- Slf4j version updated on 1.7.36, by @HardNorth

## [5.1.1]
### Changed
- Client version updated on [5.1.4](https://github.com/reportportal/client-java/releases/tag/5.1.4)
- Slf4j version updated on 1.7.32 to support newer versions of Logback with security fixes

## [5.1.0]
### Added
- Class level `@Attributes` annotation support
### Changed
- Version promoted to stable release
- Client version updated on [5.1.0](https://github.com/reportportal/client-java/releases/tag/5.1.0)

## [5.1.0-RC-4]
### Added
- JSR-305 annotations
- ReportPortalExtension#getItemId method
### Changed
- Deprecated code remove

## [5.1.0-RC-3]
### Changed
- Client version updated on [5.1.0-RC-12](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-12)

## [5.1.0-RC-1]
### Changed
- Client version updated on [5.1.0-RC-5](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-5)
- Version changed on 5.1.0

## [5.1.0-BETA-5]
### Fixed
- Main branch fixes integration

## [5.1.0-BETA-4]
### Fixed
- A crash on Launch start due to attribute modification
### Changed
- Revert fix for issue #81 since it doesn't fix it
- Launch finish hook registration moved into a separate protected method
- Client version updated on [5.1.0-BETA-1](https://github.com/reportportal/client-java/releases/tag/5.1.0-BETA-1)
- Deprecated code was removed
- JUnit 5 version updated on 5.6.3
- Implicit Guava dependency removed

## [5.0.6]
### Fixed
- Failed `@AfterEach` methods now also fail parent items
- A test step in case of failed `@BeforeEach` method now reports as 'Skipped'
### Changed
- Client version updated on [5.0.21](https://github.com/reportportal/client-java/releases/tag/5.0.21)

## [5.0.5]
### Changed
- Client version updated on [5.0.18](https://github.com/reportportal/client-java/releases/tag/5.0.18)

## [5.0.3]
### Changed 
- Client version updated
- StepAspect now handled inside client
- Item name truncation in ItemTreeUtils class was removed as pointless
- Item name truncation corrected for ReportPortalExtension class
- `junit-jupiter-api` dependency now marked as `api` (it's now compile-time dependency)
- Many of methods were marked as `protected` to ease extension development (Issue #71)  

## [5.0.2]
### Changed 
- Test Case ID generation improved
- Client version updated

## [5.0.0]

## [5.0.0-RC-1]
## Added
- Manual nested step finishing on a parent test item finish
- Support of dynamic nested tests of arbitrary depth
- Customization methods

### Removed
- Explicit extension registration in the package

## [5.0.0-BETA-16]
### Added
- Release pipelines moved into github actions
### Fixed
- Test template finish logic to avoid parallel execution failures
### Changed
- README.md updated with the latest artifact versions
- Bumping up client version to support new features
- All `@NotNull` annotations replaced with `@Nonnull`
- Parameters extraction logic moved into the client

## [4.0.0]
##### Released: XXX Dec 2018

### New Features
* Initial release
