# Changelog

## [Unreleased]
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
