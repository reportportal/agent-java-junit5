# Changelog

## [Unreleased]

## [5.1.0-ALPHA-5]
### Fixed
- A crash on Launch start due to attribute modification
### Changed
- Client version updated on [5.1.0-ALPHA-4](https://github.com/reportportal/client-java/releases/tag/5.1.0-ALPHA-4)
- Deprecated code was removed

## [5.0.4]
### Changed 
- Client version updated on [5.0.18](https://github.com/reportportal/client-java/releases/tag/5.0.18)
### Fixed
- A try to fix issue #81: the agent is not creating launches

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
