# Changelog

## [Unreleased]
### Changed 
- Client version updated
- StepAspect now handled inside client
- Item name truncation in ItemTreeUtils class was removed as pointless
- Item name truncation corrected for ReportPortalExtension class
- `junit-jupiter-api` dependency now marked as `api` (compile-time dependency) 

## Added
- `junit-jupiter-engine` as 'implementation' (runtime dependency) to simplify setup for users


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
