## 2023-02-24 v3.0.0

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v2.0.7...v3.0.0)

### Technical tasks
* [MODEXPW-347](https://issues.folio.org/browse/MODEXPW-347) Bulk Operations: save entities in json together with csv
* [MODEXPW-342](https://issues.folio.org/browse/MODEXPW-342) Refreshing mechanism for presigned url (Circulation log)
* [MODEXPW-333](https://issues.folio.org/browse/MODEXPW-333) Update the module to Spring boot v3.0.0 and identify issues
* [MODEXPW-332](https://issues.folio.org/browse/MODEXPW-332) Update to Java 17
* [MODEXPW-292](https://issues.folio.org/browse/MODEXPW-292) Logging improvement - Configuration

### Stories
* [MODEXPW-362](https://issues.folio.org/browse/MODEXPW-362) Bulk Edit Query job should contain link to json with results

### Bug fixes
* [MODEXPW-369](https://issues.folio.org/browse/MODEXPW-369) "Authority control" job failed because of a permission error.
* [MODEXPW-368](https://issues.folio.org/browse/MODEXPW-368) Align csv-file format with bulk-operations
* [MODEXPW-367](https://issues.folio.org/browse/MODEXPW-367) Authority control: Invalid fromDate/toDate format
* [MODEXPW-364](https://issues.folio.org/browse/MODEXPW-364) Broken user birthday field fails bulk edit job
* [MODEXPW-344](https://issues.folio.org/browse/MODEXPW-344) Upgrades: Spring Boot 2.7.6, ssh-sftp 2.9.2, netty 4.1.86, commons-net 3.9.0
* [MODEXPW-321](https://issues.folio.org/browse/MODEXPW-321) null instead of Record Identifier

## 2022-10-26 v2.0.0

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.6...v2.0.0)

### Technical tasks
[MODEXPW-259](https://issues.folio.org/browse/MODEXPW-259) mod-data-export-worker: spring upgrade
[MODEXPW-207](https://issues.folio.org/browse/MODEXPW-207) Add personal data disclosure form
[MODEXPW-199](https://issues.folio.org/browse/MODEXPW-199) Upgrade Users interface to 16.0

### Stories
[MODEXPW-273](https://issues.folio.org/browse/MODEXPW-273) Populate kafka payload with "jobName" for Export History
[MODEXPW-250](https://issues.folio.org/browse/MODEXPW-250) E-mail can be edited partially (not entire only)
[MODEXPW-246](https://issues.folio.org/browse/MODEXPW-246) Implement Re-send EDIFACT export logic
[MODEXPW-245](https://issues.folio.org/browse/MODEXPW-245) Preview of changed records for holdings update
[MODEXPW-240](https://issues.folio.org/browse/MODEXPW-240) Populate kafka payload with required fileds for Export History
[MODEXPW-236](https://issues.folio.org/browse/MODEXPW-236) Improvements for users in-app approach
[MODEXPW-235](https://issues.folio.org/browse/MODEXPW-235) Holdings in-app approach: Download Holdings Preview API
[MODEXPW-233](https://issues.folio.org/browse/MODEXPW-233) Migrate MinIO adapter changes to master branch
[MODEXPW-232](https://issues.folio.org/browse/MODEXPW-232) users in-app approach: Download Users Preview API
[MODEXPW-222](https://issues.folio.org/browse/MODEXPW-222) Bulk edit: Holdings locations - Holdings Preview API
[MODEXPW-220](https://issues.folio.org/browse/MODEXPW-220) Bulk edit: holdings locations - Create BulkEditHoldingsContentUpdateService
[MODEXPW-219](https://issues.folio.org/browse/MODEXPW-219) Bulk edit: holdings locations - Content update validator
[MODEXPW-218](https://issues.folio.org/browse/MODEXPW-218) eHoldings: rework export job not to store all job data in-memory
[MODEXPW-217](https://issues.folio.org/browse/MODEXPW-217) Bulk edit: holdings locations - Update BulkEditController to support holdings content update
[MODEXPW-216](https://issues.folio.org/browse/MODEXPW-216) Get matching records CSV file by holdings records identifiers CSV file
[MODEXPW-213](https://issues.folio.org/browse/MODEXPW-213) items-in-app-update: item loan types
[MODEXPW-190](https://issues.folio.org/browse/MODEXPW-190) users-in-app-update: Content update validator
[MODEXPW-189](https://issues.folio.org/browse/MODEXPW-189) users-in-app-update: E-mails bulk-edit updating
[MODEXPW-181](https://issues.folio.org/browse/MODEXPW-181) users-in-app-update: Update ITEMS content update in BulkEditController
[MODEXPW-179](https://issues.folio.org/browse/MODEXPW-179) users-in-app-update: Create BulkEditUserContentUpdateService
[MODEXPW-43](https://issues.folio.org/browse/MODEXPW-43) Add step to save orders in EDIFACT format in the Minio object storage

### Bug fixes
[MODEXPW-291](https://issues.folio.org/browse/MODEXPW-291) Missing bulk edit Inventory-holdings permissions
[MODEXPW-285](https://issues.folio.org/browse/MODEXPW-285) User is able to modify Holdings in MARC via Bulk Edit app
[MODEXPW-255](https://issues.folio.org/browse/MODEXPW-255) Bulk edit: 10528 upstream timed out (110: Connection timed out) Error
[MODEXPW-242](https://issues.folio.org/browse/MODEXPW-242) Bulk Edit: uploading file with identifiers fails when call number contains semicolon
[MODEXPW-237](https://issues.folio.org/browse/MODEXPW-237) Not all items in mod-inventory-storage on PTF env have value in _version table
[MODEXPW-229](https://issues.folio.org/browse/MODEXPW-229) custom-fields query fails due to CQL format
[MODEXPW-212](https://issues.folio.org/browse/MODEXPW-212) "Preview of records matched" is not populated using limited permissions
[MODEXPW-211](https://issues.folio.org/browse/MODEXPW-211) "A job instance already exists" error bulk editing Items on large bulk edit job
[MODEXPW-203](https://issues.folio.org/browse/MODEXPW-203) "Fail to upload file" error with large amount of Users barcodes
[MODEXPW-183](https://issues.folio.org/browse/MODEXPW-183) Less than 10 records returned for preview of matched records

## 2022-09-07 v1.4.6

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.5...v1.4.6)

### Stories
* [MODEXPW-238](https://issues.folio.org/browse/MODEXPW-238) Optimize remote files composing

## 2022-09-02 v1.4.5

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.4...v1.4.5)

### Stories
* [MODEXPW-227](https://issues.folio.org/browse/MODEXPW-227) Change isolation level to READ_COMMITTED for spring batch
* [MODEXPW-198](https://issues.folio.org/browse/MODEXPW-198) Removing shared resources
* [MODEXPW-197](https://issues.folio.org/browse/MODEXPW-197) MinIO Adapter migration
* [MODEXPW-196](https://issues.folio.org/browse/MODEXPW-196) LocalFS -> MinIO Adapter implementation

## 2022-08-29 v1.4.4

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.3...v1.4.4)

### Bug fixes
* [MODEXPW-226](https://issues.folio.org/browse/MODEXPW-226) Expense class not included as combined code in edit file

## 2022-08-12 v1.4.3

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.2...v1.4.3)

### Technical tasks
* [MODEXPW-193](https://issues.folio.org/browse/MODEXPW-193) Support of special symbols in the barcodes

### Stories
* [MODEXPW-174](https://issues.folio.org/browse/MODEXPW-174) eHoldings: remove data duplication in batch job execution context

### Bug fixes
* [MODEXPW-209](https://issues.folio.org/browse/MODEXPW-209) Order line is exported when "Manual" checkbox is enabled in PO
* [MODEXPW-175](https://issues.folio.org/browse/MODEXPW-175) eHoldings: export stops after job failure
* [MODEXPW-170](https://issues.folio.org/browse/MODEXPW-170) Status of export job of "Package" record (with almost 10k "Titles") hang with "In progress" value

## 2022-08-02 v1.4.2

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.1...v1.4.2)

### Bug fixes
* [MODEXPW-200](https://issues.folio.org/browse/MODEXPW-200) Jobs pile up in scheduled status when eHoldings, Edifact and Bulk edit run together

## 2022-07-22 v1.4.1

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.4.0...v1.4.1)

### Technical tasks
* [MODEXPW-188](https://issues.folio.org/browse/MODEXPW-188) Remove asynchronous launching job for Bursar
* [MODEXPW-164](https://issues.folio.org/browse/MODEXPW-164) Increase maximal upload file size

### Bug fixes
* [MODEXPW-192](https://issues.folio.org/browse/MODEXPW-192) 500 Error occurred Clearing Items locations on the MG bugfest
* [MODEXPW-184](https://issues.folio.org/browse/MODEXPW-184) Are you sure form does not show updated item's status value for initial Checked out status
* [MODEXPW-182](https://issues.folio.org/browse/MODEXPW-182) 500 Error occurred while Item Status updating
* [MODEXPW-166](https://issues.folio.org/browse/MODEXPW-166) Bulk Edit: 500 Server Error when editing specific records

## 2022-07-08 v1.4.0

[Full Changelog](https://github.com/folio-org/mod-data-export-worker/compare/v1.3.0...v1.4.0)

### Technical tasks
* [MODEXPW-173](https://issues.folio.org/browse/MODEXPW-173) Close FolioExecutionContext if it is opened
* [MODEXPW-148](https://issues.folio.org/browse/MODEXPW-148) Remove org.json:json, license is not open source
* [MODEXPW-145](https://issues.folio.org/browse/MODEXPW-145) Preview API upgrading
* [MODEXPW-125](https://issues.folio.org/browse/MODEXPW-125) Remove dependency to deprecated module
* [MODEXPW-119](https://issues.folio.org/browse/MODEXPW-119) Improvements for Are you sure form
* [MODEXPW-90](https://issues.folio.org/browse/MODEXPW-90) Items Content Update API /bulk-edit/{jobId}/items-content-update/upload
* [MODEXPW-74](https://issues.folio.org/browse/MODEXPW-74) mod-data-export-worker: folio-spring-base v4.1.0 update

### Stroies
* [MODEXPW-156](https://issues.folio.org/browse/MODEXPW-156) Prepare infrastructure to migrate mod-data-export
* [MODEXPW-134](https://issues.folio.org/browse/MODEXPW-134) On the update report selected and not affected records
* [MODEXPW-127](https://issues.folio.org/browse/MODEXPW-127) Update result of User and Item retrieval jobs
* [MODEXPW-123](https://issues.folio.org/browse/MODEXPW-123) Add additional user identifier types support for bulk edit
* [MODEXPW-121](https://issues.folio.org/browse/MODEXPW-121) Export eHoldings: Add mod-agreements support
* [MODEXPW-120](https://issues.folio.org/browse/MODEXPW-120) Export eHoldings: Add mod-notes support
* [MODEXPW-116](https://issues.folio.org/browse/MODEXPW-116) Download matched records (CSV) returns users instead of items when using Query items (Bulk Edit)
* [MODEXPW-109](https://issues.folio.org/browse/MODEXPW-109) Update user records with custom fields
* [MODEXPW-103](https://issues.folio.org/browse/MODEXPW-103) Log error and don't throw exception if order lines were not found
* [MODEXPW-102](https://issues.folio.org/browse/MODEXPW-102) Improve error handling if provided data from EDIFACT export configuration has incorrect format
* [MODEXPW-92](https://issues.folio.org/browse/MODEXPW-92) Item Preview API (/bulk-edit/{jobId}/preview/items)
* [MODEXPW-86](https://issues.folio.org/browse/MODEXPW-86) Saving edited item locations
* [MODEXPW-85](https://issues.folio.org/browse/MODEXPW-85) Improve user processor logic to support arrays in custom fields
* [MODEXPW-80](https://issues.folio.org/browse/MODEXPW-80) Retrieve item records based on the provided identifiers

### Bug fixes
* [MODEXPW-155](https://issues.folio.org/browse/MODEXPW-155) Incorrect identifier in the error accordion after completing bulk edit
* [MODEXPW-149](https://issues.folio.org/browse/MODEXPW-149) "Bad request" retrieved during Items bulk edit if the location name contains "/"
* [MODEXPW-135](https://issues.folio.org/browse/MODEXPW-135) Circulation log export shows another tenant/user's data
* [MODEXPW-132](https://issues.folio.org/browse/MODEXPW-132) Errors when uploading file with valid "Items former identifiers"
* [MODEXPW-131](https://issues.folio.org/browse/MODEXPW-131) Downloaded matched records differs from "Preview of matched records" if identifiers return more than one item
* [MODEXPW-130](https://issues.folio.org/browse/MODEXPW-130) Bulk Edit allows to clear "Patron group" for Users profile by uploading modified records
* [MODEXPW-126](https://issues.folio.org/browse/MODEXPW-126) The Errors accordion after bulk edit is populated with errors occurred during the matching identifiers
* [MODEXPW-115](https://issues.folio.org/browse/MODEXPW-115) Download link shouldn't be available for empty file with matched records
* [MODEXPW-114](https://issues.folio.org/browse/MODEXPW-114) "Download changed records (CSV)" is missing from Actions Menu
* [MODEXPW-94](https://issues.folio.org/browse/MODEXPW-94) Spring4Shell Morning Glory R2 2022 (CVE-2022-22965)

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
