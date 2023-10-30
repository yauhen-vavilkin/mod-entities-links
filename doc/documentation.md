# Entities-Links Documentation

## Table of contents

<!-- TOC -->
* [Entities-Links Documentation](#entities-links-documentation)
  * [Table of contents](#table-of-contents)
  * [Compiling](#compiling)
  * [Running it](#running-it)
  * [Docker](#docker)
  * [Deploying the module](#deploying-the-module)
    * [Environment variables](#environment-variables)
    * [Configuring spring-boot](#configuring-spring-boot)
  * [Integration](#integration)
    * [Folio modules communication](#folio-modules-communication)
    * [Consuming Kafka messages](#consuming-kafka-messages)
    * [Producing Kafka messages](#producing-kafka-messages)
  * [APIs](#apis)
    * [API instance-authority-links](#api-instance-authority-links)
      * [Examples](#examples)
        * [Retrieve all links by the given instance id:](#retrieve-all-links-by-the-given-instance-id)
        * [Modify links by the given instance id:](#modify-links-by-the-given-instance-id)
        * [Count number of links for each authority id:](#count-number-of-links-for-each-authority-id)
    * [API instance-authority-linking-rules](#api-instance-authority-linking-rules)
      * [Instance to Authority linking rule parameters](#instance-to-authority-linking-rule-parameters)
      * [Examples](#examples-1)
        * [Retrieve instance to authority linking rules collection:](#retrieve-instance-to-authority-linking-rules-collection)
        * [Change auto-linking flag in instance to authority linking rule:](#change-auto-linking-flag-in-instance-to-authority-linking-rule)
    * [API instance-authority-links-statistics](#api-instance-authority-links-statistics)
      * [Examples](#examples-2)
        * [Retrieve instance to authority links statistics collection:](#retrieve-instance-to-authority-links-statistics-collection)
          * [Instance to Authority links statistics parameters](#instance-to-authority-links-statistics-parameters)
        * [Retrieve linked bib updates statistics collection:](#retrieve-linked-bib-updates-statistics-collection)
          * [Linked bib updates statistics parameters](#linked-bib-updates-statistics-parameters)
    * [API instance-authority-links-suggestions](#api-instance-authority-links-suggestions)
      * [Examples](#examples-3)
        * [Retrieve links suggestions for marc records:](#retrieve-links-suggestions-for-marc-records)
<!-- TOC -->

## Compiling

```shell
mvn install
```

See that it says "BUILD SUCCESS" near the end.

## Running it

Run locally with proper environment variables set (see [Environment variables](#environment-variables) below)
on listening port 8081 (default listening port):

```shell
KAFKA_HOST=localhost KAFKA_PORT=9092 \
   java -Dserver.port=8081 -jar target/mod-entities-links-*.jar
```

## Docker

Build the docker container with:

```shell
docker build -t mod-entities-links .
```

Test that it runs with:

```shell
docker run -t -i -p 8081:8081 mod-entities-links
```

## Deploying the module

### Environment variables

| Name                                                    | Default value                         | Description                                                                                                                                                                                          |
|:--------------------------------------------------------|:--------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ENV                                                     | folio                                 | The logical name of the deployment, must be unique across all environments using the same shared Kafka/Elasticsearch clusters, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed                |
| DB_HOST                                                 | localhost                             | Postgres hostname                                                                                                                                                                                    |
| DB_PORT                                                 | 5432                                  | Postgres port                                                                                                                                                                                        |
| DB_USERNAME                                             | folio_admin                           | Postgres username                                                                                                                                                                                    |
| DB_PASSWORD                                             | folio_admin                           | Postgres username password                                                                                                                                                                           |
| DB_DATABASE                                             | okapi_modules                         | Postgres database name                                                                                                                                                                               |
| DB_MAXPOOLSIZE                                          | 10                                    | This property controls the maximum size that the pool is allowed to reach, including both idle and in-use connections                                                                                |
| DB_MINIMUM_IDLE                                         | 10                                    | This property controls the minimum number of idle connections that HikariCP tries to maintain in the pool                                                                                            |
| DB_CONNECTION_TIMEOUT                                   | 30000                                 | This property controls the maximum number of milliseconds that a client will wait for a connection from the pool                                                                                     |
| DB_IDLE_TIMEOUT                                         | 600000                                | This property controls the maximum amount of time that a connection is allowed to sit idle in the pool. This setting only applies when `DB_MINIMUM_IDLE` is defined to be less than `DB_MAXPOOLSIZE` |
| DB_KEEPALIVE_TIME                                       | 0                                     | This property controls how frequently HikariCP will attempt to keep a connection alive, in order to prevent it from being timed out by the database or network infrastructure (0 - disabled)         |
| DB_MAX_LIFETIME                                         | 1800000                               | This property controls the maximum lifetime of a connection in the pool                                                                                                                              |
| DB_VALIDATION_TIMEOUT                                   | 5000                                  | This property controls the maximum amount of time that a connection will be tested for aliveness. This value must be less than the `DB_CONNECTION_TIMEOUT`                                           |
| DB_INITIALIZATION_FAIL_TIMEOUT                          | 30000                                 | This property controls whether the pool will "fail fast" if the pool cannot be seeded with an initial connection successfully                                                                        |
| DB_LEAK_DETECTION_THRESHOLD                             | 30000                                 | This property controls the amount of time that a connection can be out of the pool before a message is logged indicating a possible connection leak (0 - disabled)                                   |
| OKAPI_URL                                               | -                                     | Okapi URL                                                                                                                                                                                            |
| SYSTEM_USER_USERNAME                                    | mod-entities-links                    | Username for system user                                                                                                                                                                             |
| SYSTEM_USER_PASSWORD                                    | -                                     | Password for system user                                                                                                                                                                             |
| KAFKA_HOST                                              | kafka                                 | Kafka broker hostname                                                                                                                                                                                |
| KAFKA_PORT                                              | 9092                                  | Kafka broker port                                                                                                                                                                                    |
| KAFKA_SECURITY_PROTOCOL                                 | PLAINTEXT                             | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                                                          |
| KAFKA_SSL_KEYSTORE_LOCATION                             | -                                     | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                                                         |
| KAFKA_SSL_KEYSTORE_PASSWORD                             | -                                     | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                                                               |
| KAFKA_SSL_TRUSTSTORE_LOCATION                           | -                                     | The location of the Kafka trust store file.                                                                                                                                                          |
| KAFKA_SSL_TRUSTSTORE_PASSWORD                           | -                                     | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.                                           |
| KAFKA_CONSUMER_MAX_POLL_RECORDS                         | 50                                    | Maximum number of records returned in a single call to poll().                                                                                                                                       |
| KAFKA_INSTANCE_AUTHORITY_TOPIC_PARTITIONS               | 10                                    | Amount of partitions for `links.instance-authority` topic.                                                                                                                                           |
| KAFKA_INSTANCE_AUTHORITY_TOPIC_REPLICATION_FACTOR       | -                                     | Replication factor for `links.instance-authority` topic.                                                                                                                                             |
| KAFKA_INSTANCE_AUTHORITY_STATS_TOPIC_PARTITIONS         | 10                                    | Amount of partitions for `links.instance-authority-stats` topic.                                                                                                                                     |
| KAFKA_INSTANCE_AUTHORITY_STATS_TOPIC_REPLICATION_FACTOR | -                                     | Replication factor for `links.instance-authority-stats` topic.                                                                                                                                       |
| KAFKA_AUTHORITIES_CONSUMER_CONCURRENCY                  | 1                                     | Number of kafka concurrent threads for `inventory.authority` message consuming                                                                                                                       |
| KAFKA_INSTANCE_AUTHORITY_STATS_CONSUMER_CONCURRENCY     | 1                                     | Number of kafka concurrent threads for `links.instance-authority-stats` message consuming                                                                                                            |
| KAFKA_INSTANCE_AUTHORITY_CHANGE_PARTITIONS              | 100                                   | Number of instance-authority links `links.instance-authority` event contains while processing authority link source change.                                                                          |
| INSTANCE_STORAGE_QUERY_BATCH_SIZE                       | 50                                    | Number of instances to retrieve from inventory storage per one request (Max 90 - based on maximum URI length).                                                                                       |

### Configuring spring-boot

Spring boot properties can be overridden using the specified environment variables, if it is not it can be done using
one of the following approaches (see also the
documentation [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/1.5.6.RELEASE/reference/html/boot-features-external-config.html)):

1. Using the environment variable `SPRING_APPLICATION_JSON` (
   example: `SPRING_APPLICATION_JSON='{"foo":{"bar":"spam"}}'`)
2. Using the system variables within the `JAVA_OPTIONS` (
   example: `JAVA_OPTIONS=-Xmx400m -Dlogging.level.org.folio.search=debug`)

## Integration

### Folio modules communication

| Module name               | Interface                     | Notes                                                     |
|---------------------------|-------------------------------|-----------------------------------------------------------|
| mod-login                 | login                         | For system user creation and authentication               |
| mod-permissions           | permissions                   | For system user creation                                  |
| mod-users                 | users                         | For system user creation                                  |
| mod-source-record-manager | mapping-rules-provider        | For fetching MARC bibliographic-to-Instance mapping rules |
| mod-source-record-storage | source-storage-source-records | For fetching Authority source records in MARC format      |
| mod-inventory-storage     | authority-source-files        | For fetching Authority source file reference data         |
| mod-inventory-storage     | instance-storage              | For fetching Instance data for statistic                  |

### Consuming Kafka messages

| Topic name                         | Group ID                                   | Notes                                                     |
|------------------------------------|--------------------------------------------|-----------------------------------------------------------|
| {ENV}.[tenant].inventory.authority | {ENV}-mod-entities-links-authorities-group | Filtrating messages that have type UPDATE and DELETE only |

### Producing Kafka messages

| Topic name                              | Notes                                                                                                                                         |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| {ENV}.[tenant].links.instance-authority | Contains instance-authority links changes that should be applied to MARC bibliographic records while processing authority link source change. |

## APIs

### API instance-authority-links

The API enables possibility to link a MARC bib field(s) to MARC authority heading(s)/reference(s) because authority
records are seen as the source of truth about a person/place/corporation/conference/subject/genre.
This allows to indicate on any MARC bib records' 1XX/6XX/7XX/8XX fields that they are controlled by the linked/matched
MARC authority field 1XX/4XX.
Which means that the fields are not editable on bib side, instead manual edits to a MARC authority field 1XX/4XX are
reflected on them.

| METHOD | URL                             | Required permissions                             | DESCRIPTION                                 |
|:-------|:--------------------------------|:-------------------------------------------------|:--------------------------------------------|
| GET    | `/links/instances/{instanceId}` | `entities-links.instances.collection.get`        | Get links collection related to Instance    |
| PUT    | `/links/instances/{instanceId}` | `entities-links.instances.collection.put`        | Update links collection related to Instance |
| POST   | `/links/authorities/bulk/count` | `instance-authority-links.authorities.bulk.post` | Retrieve number of links by authority IDs   |

#### Examples

<a name="retrieve-instance-links"></a>

##### Retrieve all links by the given instance id:

`GET /links/instances/b2658a84-912b-4ed9-83d7-e8201f4d27ec`

Response:
```json
{
  "links": [
    {
      "id": 1,
      "authorityId": "0794f296-4094-4243-b9af-bb4bf51cbfae",
      "authorityNaturalId": "n92099941",
      "instanceId": "b2658a84-912b-4ed9-83d7-e8201f4d27ec",
      "linkingRuleId": 1,
      "status": "ERROR",
      "errorCause": "Some error"
    }
  ],
  "totalRecords": 1
}
```

<a name='modify-instance-links'></a>

##### Modify links by the given instance id:

`PUT /links/instances/b2658a84-912b-4ed9-83d7-e8201f4d27ec`

Request body:
```json
{
  "links": [
    {
      "authorityId": "875e86cf-599d-4293-b966-b34ac6a6d19e",
      "authorityNaturalId": "no50047988",
      "instanceId": "b2658a84-912b-4ed9-83d7-e8201f4d27ec",
      "linkingRuleId": 2
    },
    {
      "authorityId": "77398964-ee2d-4b28-ba2b-e30851d5d963",
      "authorityNaturalId": "mp96352145",
      "instanceId": "b2658a84-912b-4ed9-83d7-e8201f4d27ec",
      "linkingRuleId": 1
    }
  ]
}
```

<a name='count-number-of-links-for-each-authority-id'></a>

##### Count number of links for each authority id:

`POST /links/authorities/bulk/count`

Request body:
```json
{
  "ids": [
    "611d2938-9824-4817-a737-7ff26ab26d03",
    "8e6a8a67-dd6b-4b40-899e-9b9167e9ad20"
  ]
}
```

Response:
```json
{
  "links": [
    {
      "id": "8e6a8a67-dd6b-4b40-899e-9b9167e9ad20",
      "totalLinks": 11
    },
    {
      "id": "611d2938-9824-4817-a737-7ff26ab26d03",
      "totalLinks": 0
    }
  ]
}
```

### API instance-authority-linking-rules

The API enables possibility to retrieve default linking rules.

| METHOD | URL                                          | Required permissions                              | DESCRIPTION                                               |
|:-------|:---------------------------------------------|:--------------------------------------------------|:----------------------------------------------------------|
| GET    | `/linking-rules/instance-authority`          | `instance-authority.linking-rules.collection.get` | Get Instance to Authority linking rules collection        |
| GET    | `/linking-rules/instance-authority/{ruleId}` | `instance-authority.linking-rules.item.get`       | Get Instance to Authority linking rule by ID              |
| PATCH  | `/linking-rules/instance-authority/{ruleId}` | `instance-authority.linking-rules.item.patch`     | Partially update Instance to Authority linking rule by ID |

#### Instance to Authority linking rule parameters

* `id` - Rule ID
* `bibField` - Instance field which would be controlled by authority field
* `authorityField` - Authority field which would be linked to instance field
* `authoritySubfields` - Array of authority subfields that can be linked to instance subfields. Should match instance
  subfields (in exceptions use `subfieldModifications`)
* `subfieldModifications` - Array of subfield modifications
    * `source` - Authority subfield, which would be linked to `target`
    * `target` - Instance subfield, which would be controlled by `source`
* `validation` - Linking rule validations that should be verified before linking
    * `existence` - Map <char, boolean>. Validate if subfield have to exist or not
* `autoLinkingEnabled` - Flag that indicates if the rule can be used for auto-linking

#### Examples

<a name="retrieve-instance-authority-linking-rules"></a>

##### Retrieve instance to authority linking rules collection:

`GET /linking-rules/instance-authority`

Response:
```json
[
  {
    "id": 1,
    "bibField": "240",
    "authorityField": "100",
    "authoritySubfields": [
      "f",
      "g",
      "h",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "r",
      "s",
      "t"
    ],
    "subfieldModifications": [
      {
        "source": "t",
        "target": "a"
      }
    ],
    "validation": {
      "existence": [
        {
          "t": true
        }
      ]
    },
    "autoLinkingEnabled": true
  }
]
```

##### Change auto-linking flag in instance to authority linking rule:

`PATCH /linking-rules/instance-authority/1`

Request body:
```json
{
  "id": 1,
  "autoLinkingEnabled": false
}
```

### API instance-authority-links-statistics

The API provides statistics for instance to authority links statistics

| METHOD | URL                      | Required permissions                                           | DESCRIPTION                                                     |
|:-------|:-------------------------|:---------------------------------------------------------------|:----------------------------------------------------------------|
| GET    | `/links/stats/authority` | `instance-authority-links.authority-statistics.collection.get` | Get Instance to Authority links authority-statistics collection |
| GET    | `/links/stats/instance`  | `instance-authority-links.instance-statistics.collection.get`  | Get linked bib update statistics collection                     |

#### Examples

<a name="retrieve-instance-authority-links-statistics"></a>
<a name="retrieve-linked-bib-update-statistics"></a>

##### Retrieve instance to authority links statistics collection:

`GET /links/stats/authority`

###### Instance to Authority links statistics parameters

* `fromDate` - Start date to seek from
* `toDate` - End date to seek from
* `action` - Action to filter by
* `limit` - Max number of items in collection

Response:
```json
[
  {
    "next": "2000-01-23T04:56:07.000+00:00",
    "stats": [
      {
        "lbTotal": 0,
        "metadata": {
          "completedAt": "2000-01-23T04:56:07.000+00:00",
          "startedByUserFirstName": "startedByUserFirstName",
          "startedByUserId": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
          "startedAt": "2000-01-23T04:56:07.000+00:00",
          "startedByUserLastName": "startedByUserLastName"
        },
        "headingOld": "headingOld",
        "sourceFileNew": "sourceFileNew",
        "naturalIdOld": "naturalIdOld",
        "lbFailed": 1,
        "authorityId": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "headingTypeNew": "headingTypeNew",
        "lbUpdated": 6,
        "naturalIdNew": "naturalIdNew",
        "id": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "headingTypeOld": "headingTypeOld",
        "sourceFileOld": "sourceFileOld",
        "headingNew": "headingNew"
      },
      {
        "lbTotal": 0,
        "metadata": {
          "completedAt": "2000-01-23T04:56:07.000+00:00",
          "startedByUserFirstName": "startedByUserFirstName",
          "startedByUserId": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
          "startedAt": "2000-01-23T04:56:07.000+00:00",
          "startedByUserLastName": "startedByUserLastName"
        },
        "headingOld": "headingOld",
        "sourceFileNew": "sourceFileNew",
        "naturalIdOld": "naturalIdOld",
        "lbFailed": 1,
        "authorityId": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "headingTypeNew": "headingTypeNew",
        "lbUpdated": 6,
        "naturalIdNew": "naturalIdNew",
        "id": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "headingTypeOld": "headingTypeOld",
        "sourceFileOld": "sourceFileOld",
        "headingNew": "headingNew"
      }
    ]
  }
]
```

##### Retrieve linked bib updates statistics collection:

`GET /links/stats/instance`

###### Linked bib updates statistics parameters

* `fromDate` - Start date to seek from
* `toDate` - End date to seek to
* `status` - Link status to filter by
* `limit` - Max number of items in collection

Response:
```json
[
  {
    "next": "2000-01-23T04:56:07.000+00:00",
    "stats": [
      {
        "instanceId": "046b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "instanceTitle": "Some title",
        "bibRecordTag": "123",
        "authorityNaturalId": "nr123123",
        "updatedAt": "2000-01-23T04:56:07.000+00:00",
        "errorCause": "Error cause"
      },
      {
        "instanceId": "054b6c7f-0b8a-43b9-b35d-6489e6daee91",
        "instanceTitle": "Some title 1",
        "bibRecordTag": "321",
        "authorityNaturalId": "nr321321",
        "updatedAt": "2000-01-23T04:57:07.000+00:00",
        "errorCause": "Error cause 1"
      }
    ]
  }
]
```

### API instance-authority-links-suggestions

The API provides links suggestions for marc records

| METHOD | URL                       | Required permissions                        | DESCRIPTION                                 |
|:-------|:--------------------------|:--------------------------------------------|:--------------------------------------------|
| POST   | `/links-suggestions/marc` | `instance-authority-links.suggestions.post` | Retrieve links suggestions for marc records |

#### Examples

<a name="retrieve-links-suggestions-for-marc-records"></a>

##### Retrieve links suggestions for marc records:

`POST /links-suggestions/marc`

Request body:
```json
{
  "records": [
    {
      "fields": [
        {
          "001": "393893"
        },
        {
          "100": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Mozart, Wolfgang Amadeus"
              },
              {
                "d": "1756-1791."
              },
              {
                "0": "12345"
              },
              {
                "9": "b9a5f035-de63-4e2c-92c2-07240c88b817"
              }
            ],
            "linkDetails": {
              "authorityId": "b9a5f035-de63-4e2c-92c2-07240c88b817",
              "authorityNaturalId": "12345",
              "linkingRuleId": 1,
              "linkStatus": "ACTUAL"
            }
          }
        },
        {
          "110": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Mozart"
              }
            ]
          }
        },
        {
          "130": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Wolfgang Amadeus"
              }
            ]
          }
        }
      ],
      "leader": "01706ccm a2200361   4500"
    }
  ]
}
```

Response:
```json
{
  "records": [
    {
      "fields": [
        {
          "001": "393893"
        },
        {
          "100": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Mozart, Wolfgang Amadeus,"
              },
              {
                "d": "1756-1791."
              },
              {
                "0": "12345"
              },
              {
                "9": "b9a5f035-de63-4e2c-92c2-07240c88b817"
              }
            ],
            "linkDetails": {
              "authorityId": "b9a5f035-de63-4e2c-92c2-07240c88b817",
              "authorityNaturalId": "12345",
              "linkingRuleId": 1,
              "linkStatus": "ACTUAL"
            }
          }
        },
        {
          "110": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Mozart"
              },
              {
                "0": "12345"
              },
              {
                "9": "b9a5f035-de63-4e2c-92c2-07240c88b817"
              }
            ],
            "linkDetails": {
              "authorityId": "b9a5f035-de63-4e2c-92c2-07240c88b817",
              "authorityNaturalId": "12345",
              "linkingRuleId": 1,
              "linkStatus": "NEW"
            }
          }
        },
        {
          "130": {
            "ind1": "/",
            "ind2": "/",
            "subfields": [
              {
                "a": "Wolfgang Amadeus"
              }
            ],
            "linkDetails": {
              "linkStatus": "ERROR",
              "errorStatusCode": "101"
            }
          }
        }
      ],
      "leader": "01706ccm a2200361   4500"
    }
  ]
}
```
