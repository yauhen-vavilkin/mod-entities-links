## 1.0.1
### Bug fixes
* Change system-user lastname to "Automated linking update" ([MODELINKS-52](https://issues.folio.org/browse/MODELINKS-52))
* Return only authority stats that are related to actual authority ([MODELINKS-57](https://issues.folio.org/browse/MODELINKS-57))
* Update authority linking rules for 600/610/611/630/650/651/655 ([MODELINKS-58](https://issues.folio.org/browse/MODELINKS-58))
* Respond with authorSourceFile name instead od ID in statistics endpoint ([MODELINKS-59](https://issues.folio.org/browse/MODELINKS-59))
* Respond with metadata even if user was deleted in statistics endpoint ([MODELINKS-64](https://issues.folio.org/browse/MODELINKS-64))
* Respond with "Not specified" when source file name is empty in statistics endpoint ([MODELINKS-65](https://issues.folio.org/browse/MODELINKS-65))

### Dependencies
* Bump `folio-service-tools` from `3.0.0` to `3.0.2`

## 1.0.0
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
