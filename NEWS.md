## v1.3.0 - Unreleased

## v1.2.4 - Released
The primary focus of this release is fixing several tenants usage errors for data export process

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.2.3...v1.2.4)

### Bug fixes
* [MODEXPW-137](https://issues.folio.org/browse/MODEXPW-137) - Backport to Kiwi HF#3: Circulation log export shows another tenant/user's data

## v1.2.3 - Released
The primary focus of this release is fixing several tenants usage errors for data export process

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.2.2...v1.2.3)

### Bug fixes
* [MODEXPW-83](https://issues.folio.org/browse/MODEXPW-83) - Backport for Kiwi HF#2: FolioExecutionContext is initialized with wrong tenant id if Spring Batch job launches asynchronously in multi tenant cluster

## v1.2.2 - Released
The primary focus of this release is log4j upgrade to 2.17.0

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.2.1...v1.2.2)

### Bug fixes
* [MODEXPW-57](https://issues.folio.org/browse/MODEXPW-57) - Kiwi R3 2021 - Log4j vulnerability verification and correction


## v1.2.1 - Released
The primary focus of this release is fixing log4j vulnerability

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.2.0...v1.2.1)

### Bug fixes
* [MODEXPW-57](https://issues.folio.org/browse/MODEXPW-57) - Kiwi R3 2021 - Log4j vulnerability verification and correction


## 2021-11-10 v1.2.0
* [MODEXPW-38](https://issues.folio.org/browse/MODEXPW-38) Saving circulation log with notices "send error" fails
* [MODEXPW-35](https://issues.folio.org/browse/MODEXPW-35) Dates in CSV export from Circulation log are not in 24-hour time format
* [MODEXPW-31](https://issues.folio.org/browse/MODEXPW-31) Bursar transfer form cannot specify days outstanding
* [MODEXPW-29](https://issues.folio.org/browse/MODEXPW-29) mod-data-export-worker: folio-spring-base v2 update

## 2021-08-06 v1.1.6
* [MODEXPS-24](https://issues.folio.org/browse/MODEXPS-24) Kafka topic created with incorrect ENV and tenantId combination

## 2021-08-02 v1.1.5
* [MODEXPW-27](https://issues.folio.org/browse/MODEXPW-27) Can not start an export job for configuration with a lot of records for mapping

## 2021-07-15 v1.1.4
* [MODEXPW-24](https://issues.folio.org/browse/MODEXPW-24) Update audit data schema.json

## 2021-07-15 v1.1.3
* [MODEXPW-24](https://issues.folio.org/browse/MODEXPW-24) Change export Circulation log format
* [MODEXPW-25](https://issues.folio.org/browse/MODEXPW-25) Enable mutiowner mapping feature

## 2021-07-05 v1.1.2
* [MODEXPW-23](https://issues.folio.org/browse/MODEXPW-23) URI Too Large for feefineactions call

## 2021-06-30 v1.1.1
* [MODEXPW-22](https://issues.folio.org/browse/MODEXPW-22) Bursar export failed for large patron group

## 2021-06-18 v1.1.0
 * No changes since last release.

## 2021-05-14 v1.0.7
 * [MODEXPW-9](https://issues.folio.org/browse/MODEXPW-9) Add standard health check endpoint

## 2021-05-06 v1.0.6
 * [MODEXPW-17](https://issues.folio.org/browse/MODEXPW-17) Username and password expressed in plain text in module logs

## 2021-04-28 v1.0.5
 * [MODEXPW-16](https://issues.folio.org/browse/MODEXPW-16) Use MinIO client with implicit AWS role authorization

## 2021-04-27 v1.0.4
 * [MODEXPW-16](https://issues.folio.org/browse/MODEXPW-16) Use MinIO client with implicit AWS role authorization

## 2021-04-19 v1.0.3
 * [MODEXPS-15](https://issues.folio.org/browse/MODEXPS-15) Kafka connection does not start without tenant registration

## 2021-04-12 v1.0.2
 * [MODEXPW-5](https://issues.folio.org/browse/MODEXPW-5) Include column headers in .CSV export of Circulation Log
 * [MODEXPW-6](https://issues.folio.org/browse/MODEXPW-6) Enhance fees/fines bursar report settings
 * [MODEXPW-7](https://issues.folio.org/browse/MODEXPW-7) Do not use setting for service point; use 'system' service point
 * [MODEXPW-8](https://issues.folio.org/browse/MODEXPW-8) Add tests to mod-data-export-worker
 * [MODEXPW-12](https://issues.folio.org/browse/MODEXPW-12) Circulation Log export causes error because of non-UUID format of some IDs in the Log
 * [MODEXPW-13](https://issues.folio.org/browse/MODEXPW-13) Headers needed for Circulation Log export

## 2021-03-18 v1.0.1
 * First module release

## 2021-01-29 v0.0.1
 * Initial module setup
