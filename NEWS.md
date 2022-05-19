## 2022-05-19 v1.3.4

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.3.3...v1.3.4)

### Technical tasks
* [MODEXPW-95](https://issues.folio.org/browse/MODEXPW-95) spring-boot-starter parent version upgrade

## 2022-04-08 v1.3.3

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.3.2...v1.3.3)

### Bug fixes
* [MODEXPW-96](https://issues.folio.org/browse/MODEXPW-96) Orders with no Account number not exported with "Default integration"

## 2022-04-08 v1.3.2

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.3.1...v1.3.2)

### Bug fixes
* [MODEXPW-93](https://issues.folio.org/browse/MODEXPW-93) Job ID is not set correctly during EDIFACT mapping

## 2022-03-24 v1.3.1

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.3.0...v1.3.1)

### Bug fixes
* [MODEXPW-87](https://issues.folio.org/browse/MODEXPW-87) bugfest order export jobs showing "Currency" not found error

## 2022-03-03 v1.3.0

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.2.2...v1.3.0)

### Technical tasks
* [MODEXPW-66](https://issues.folio.org/browse/MODEXPW-66) mod-data-export-worker: folio-spring-base v3 update

### Stroies
* [MODEXPW-32](https://issues.folio.org/browse/MODEXPW-32) Copy FTP upload service from mod-invoice into mod-data-export-worker
* [MODEXPW-42](https://issues.folio.org/browse/MODEXPW-42) Implement export flow Mapping FOLIO orders to EDIFACT order file
* [MODEXPW-44](https://issues.folio.org/browse/MODEXPW-44) Implement the preview of matched records
* [MODEXPW-45](https://issues.folio.org/browse/MODEXPW-45) Get matching records based on provided identifiers
* [MODEXPW-46](https://issues.folio.org/browse/MODEXPW-46) Get matching records based on provided query
* [MODEXPW-48](https://issues.folio.org/browse/MODEXPW-48) Remove uploaded edit records if user cancel editing
* [MODEXPW-51](https://issues.folio.org/browse/MODEXPW-51) Records processing errors handling
* [MODEXPW-52](https://issues.folio.org/browse/MODEXPW-52) Reuse export-common library
* [MODEXPW-56](https://issues.folio.org/browse/MODEXPW-56) Return the number of records in a file
* [MODEXPW-59](https://issues.folio.org/browse/MODEXPW-59) Create API to start bulk-edit job execution
* [MODEXPW-69](https://issues.folio.org/browse/MODEXPW-69) Records processing preview handling
* [MODEXPW-71](https://issues.folio.org/browse/MODEXPW-71) Uploading file with modified name fails
* [MODEXPW-72](https://issues.folio.org/browse/MODEXPW-72) Update mod-data-export-worker API documentation

### Bug fixes
* [MODEXPW-28](https://issues.folio.org/browse/MODEXPW-28) Tenant deletion failed due to hsql error
* [MODEXPW-67](https://issues.folio.org/browse/MODEXPW-67) FolioExecutionContext is initialized with wrong tenant id if Spring Batch job launches asynchronously in multi tenant cluster
* [MODEXPW-68](https://issues.folio.org/browse/MODEXPW-68) Remaining fixes and improvements for FOLIO orders to EDIFACT format mapping
* [MODEXPW-70](https://issues.folio.org/browse/MODEXPW-70) Name of downloaded file does not adhere to the naming standard
* [MODEXPW-76](https://issues.folio.org/browse/MODEXPW-76) Download matched records (CSV)  does not appear in query search screen
* [MODEXPW-79](https://issues.folio.org/browse/MODEXPW-79) Errors upon bulk-edit user update

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
