## v3.0.0 in progress
### Breaking changes
* Delete PUT endpoint from authority-source-files api([MODELINKS-161](https://issues.folio.org/browse/MODELINKS-161))

### New APIs versions
* Provides `API_NAME vX.Y`
* Requires `API_NAME vX.Y`

### Features
* Implement Authority Archiving for deleted authorities ([MODELINKS-138](https://issues.folio.org/browse/MODELINKS-138))
* Add scheduled job to expire archive records with tenant-level retention policy ([MODELINKS-139](https://issues.folio.org/browse/MODELINKS-139))
* Update authority-source-files DELETE endpoint error message([MODELINKS-162](https://issues.folio.org/browse/MODELINKS-162))
* Update authority-source-files POST API request body and DB table schemas with new fields([MODELINKS-123](https://issues.folio.org/browse/MODELINKS-123))
* Change the AuthoritySourceType field type from String to Enum([MODELINKS-170](https://issues.folio.org/browse/MODELINKS-170))
* Prohibit update/delete of authority consortium shadow copy([MODELINKS-174](https://issues.folio.org/browse/MODELINKS-174))
* Prohibit authority source files creation from consortium member tenant([MODELINKS-174](https://issues.folio.org/browse/MODELINKS-174))
* Adjust /authority-storage/authorities endpoint to allow retrieving authorities archives and only IDs of records([MODELINKS-142](https://issues.folio.org/browse/MODELINKS-142))
* Propagate authority source files to member tenants([MODELINKS-175](https://issues.folio.org/browse/MODELINKS-175))
* Implement next hrid endpoint for authority source file([MODELINKS-122](https://issues.folio.org/browse/MODELINKS-122))

### Bug fixes
* Fix secure setup of system users by default ([MODELINKS-135](https://issues.folio.org/browse/MODELINKS-135))
* Updating authority's source file field to null is failed ([MODELINKS-143](https://issues.folio.org/browse/MODELINKS-143))
* Failed to send update event if sourceFile is null ([MODELINKS-144](https://issues.folio.org/browse/MODELINKS-144))
* Remove foreign key for authority_data_stat ([MODELINKS-155](https://issues.folio.org/browse/MODELINKS-155))
* Fix empty links list propagation ([MODELINKS-166](https://issues.folio.org/browse/MODELINKS-166))

### Tech Dept
* Description ([ISSUE_NUMBER](https://issues.folio.org/browse/ISSUE_NUMBER))

### Dependencies
* Bump `folio-spring-support` from `7.2.0` to `7.2.1`

---

## v2.0.0 2023-10-12
### Breaking changes
* Change '/links/authority/stats' endpoint path to '/links/stats/authority' ([MODELINKS-77](https://issues.folio.org/browse/MODELINKS-77))

### APIs versions
* Provides `instance-authority-links-statistics v2.0`
* Provides `instance-authority-links-suggestions v1.2`
* Provides `instance-authority-links v2.1`
* Provides `instance-authority-linking-rules v1.1`
* Provides `authority-storage 2.0`
* Provides `authority-source-files 1.0`
* Provides `authority-note-types 1.0`
* Removes `linked-bib-update-statistics v1.0`

### Features
* Remove field and subfields from links endpoint, use linking rule ([MODELINKS-47](https://issues.folio.org/browse/MODELINKS-47))
* Add PATCH and GET instance-authority linking rule endpoints ([MODELINKS-80](https://issues.folio.org/browse/MODELINKS-80))
* Extend GET /links/instances/{id} with link status, errorCause ([MODELINKS-68](https://issues.folio.org/browse/MODELINKS-68))
* Implement endpoint to suggest links for MARC-bibliographic record ([MODELINKS-82](https://issues.folio.org/browse/MODELINKS-82))
* Add possibility of matching by Authority id for links suggestions ([MODELINKS-113](https://issues.folio.org/browse/MODELINKS-113))
* Relocate Authority API and associated reference APIs ([MODELINKS-106](https://issues.folio.org/browse/MODELINKS-106))
* Add possibility of ignoring autoLinkingEnabled flag for links suggestions ([MODELINKS-113](https://issues.folio.org/browse/MODELINKS-114))
* Set not specified source file name in stats endpoint ([MODELINKS-65](https://issues.folio.org/browse/MODELINKS-65))
* Add PATCH and GET linking rule endpoints ([MODELINKS-80](https://issues.folio.org/browse/MODELINKS-80))
* Extend GET /links/instances/{id} with link status, errorCause ([MODELINKS-68](https://issues.folio.org/browse/MODELINKS-68))
* Add endpoint to suggest links for MARC-bibliographic record ([MODELINKS-82](https://issues.folio.org/browse/MODELINKS-82))
* Add flag to "/links-suggestions" endpoint to indicate which fields to use for suggestions ([MODELINKS-114](https://issues.folio.org/browse/MODELINKS-114))
* Implement authority propagation for member tenants ([MODELINKS-116](https://issues.folio.org/browse/MODELINKS-116))

## Bug fixes
* Return only authority stats that are related to actual authority ([MODELINKS-57](https://issues.folio.org/browse/MODELINKS-57), [MODELINKS-72](https://issues.folio.org/browse/MODELINKS-72))
* Updating authorSourceFile name from UUID to fileName CSV ([MODELINKS-59](https://issues.folio.org/browse/MODELINKS-59))
* Metadata generation is provided when startedByUser is deleted ([MODELINKS-64](https://issues.folio.org/browse/MODELINKS-64))
* Add users get permission to system-user ([MODELINKS-52](https://issues.folio.org/browse/MODELINKS-52))
* Decrease default instance batch size to fix 414 Request-URI Too Long ([MODELINKS-78](https://issues.folio.org/browse/MODELINKS-78))

### Tech Dept
* Upgrade folio-spring-base to v7.1.0 ([MODELINKS-99](https://issues.folio.org/browse/MODELINKS-99))
* Expose database parameters to environment variables ([MODELINKS-102](https://issues.folio.org/browse/MODELINKS-102))
* Change system-user lastname to "Automated linking update" ([MODELINKS-52](https://issues.folio.org/browse/MODELINKS-52))
* Refactor handling subfields in links ([MODELINKS-47](https://issues.folio.org/browse/MODELINKS-47))
* Delete Kafka topics on disabling module for tenant ([MODELINKS-87](https://issues.folio.org/browse/MODELINKS-87))
* Increase unit test code coverage for the module ([MODELINKS-130](https://issues.folio.org/browse/MODELINKS-130))
* Expose database parameters to environment variables

### Dependencies
Bump `folio-spring-base` from `6.0.1` to `7.2.0`
Bump `folio-service-tools` from `3.0.2` to `3.1.0`
Bump `spring-boot` from `3.0.2` to `3.1.4`
Bump `hypersistence-utils` from `3.2.0` to `3.5.3`
Bump `mapstruct` from `1.5.3.Final` to `1.5.5.Final`
Bump `marc4j` from `2.9.2` to `2.9.5`
Bump `maven-openapi-generator` from `6.2.1` to `7.0.1`

---

## v1.0.0 Released 2023-02-21
### APIs versions
* Provides `instance-authority-links v1.0`
* Provides `instance-authority-links-statistics v1.0`
* Provides `instance-authority-linking-rules v1.0`
* Requires `login v7.0`
* Requires `permissions v5.3`
* Requires `users v16.0`
* Requires `mapping-rules-provider v2.0`
* Requires `source-storage-source-records v3.1`
* Requires `authority-source-files v1.0`
* Requires `instance-storage v10.0`

### Features
* Create endpoint for saving instance-authority links ([MODELINKS-2](https://issues.folio.org/browse/MODELINKS-2))
* Create endpoint for retrieving instance-authority links ([MODELINKS-3](https://issues.folio.org/browse/MODELINKS-3))
* Create endpoint for retrieving total links number for authority IDs ([MODELINKS-8](https://issues.folio.org/browse/MODELINKS-8))
* Create endpoint for retrieving instance-authority linking links ([MODELINKS-24](https://issues.folio.org/browse/MODELINKS-24))
* Create endpoint for retrieving authority update statistics ([MODELINKS-34](https://issues.folio.org/browse/MODELINKS-34))
* Create endpoint for retrieving links statistics ([MODELINKS-35](https://issues.folio.org/browse/MODELINKS-35))
* Consume authority updates and trigger updates for linked instances ([MODELINKS-18](https://issues.folio.org/browse/MODELINKS-18))

### Dependencies
* Add `java` `17`
* Add `folio-spring-base` `6.0.1`
* Add `folio-service-tools` `3.0.0`
* Add `spring-boot` `3.0.2`
* Add `spring-kafka` `3.0.2`
* Add `hypersistence-utils` `3.2.0`
* Add `mapstruct` `1.5.3.Final`
* Add `lombok` `1.18.24`
* Add `marc4j` `2.9.2`
* Add `liquibase` `4.19.0`
* Add `testcontainers` `1.17.6`
* Add `wiremock` `2.27.2`
* Add `awaitility` `4.2.0`
