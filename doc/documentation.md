# Entities-Links Documentation

## Table of contents

<!-- TOC -->
* [Entities-Links Documentation](#entities-links-documentation)
  * [Table of contents](#table-of-contents)
  * [Deploying the module](#deploying-the-module)
    * [Environment variables](#environment-variables)
  * [Integration](#integration)
    * [Folio modules communication](#folio-modules-communication)
    * [Consuming Kafka messages](#consuming-kafka-messages)
  * [API](#api)
    * [instance-links API](#instance-links-api)
    * [linking-rules API](#linking-rules-api)
  * [Linking rules](#linking-rules)
    * [Instance to Authority linking rule parameters](#instance-to-authority-linking-rule-parameters)
<!-- TOC -->

## Deploying the module
### Environment variables

| Name                                              | Default value      | Description                                                                                                                                                                           |
|:--------------------------------------------------|:-------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ENV                                               | folio              | The logical name of the deployment, must be unique across all environments using the same shared Kafka/Elasticsearch clusters, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |
| DB_HOST                                           | localhost          | Postgres hostname                                                                                                                                                                     |
| DB_PORT                                           | 5432               | Postgres port                                                                                                                                                                         |
| DB_USERNAME                                       | folio_admin        | Postgres username                                                                                                                                                                     |
| DB_PASSWORD                                       | folio_admin        | Postgres username password                                                                                                                                                            |
| DB_DATABASE                                       | okapi_modules      | Postgres database name                                                                                                                                                                |
| OKAPI_URL                                         | -                  | Okapi URL                                                                                                                                                                             |
| SYSTEM_USER_PASSWORD                              | mod-entities-links | Password for system user                                                                                                                                                              |
| KAFKA_HOST                                        | kafka              | Kafka broker hostname                                                                                                                                                                 |
| KAFKA_PORT                                        | 9092               | Kafka broker port                                                                                                                                                                     |
| KAFKA_SECURITY_PROTOCOL                           | PLAINTEXT          | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                                           |
| KAFKA_SSL_KEYSTORE_LOCATION                       | -                  | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                                          |
| KAFKA_SSL_KEYSTORE_PASSWORD                       | -                  | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                                                |
| KAFKA_SSL_TRUSTSTORE_LOCATION                     | -                  | The location of the Kafka trust store file.                                                                                                                                           |
| KAFKA_SSL_TRUSTSTORE_PASSWORD                     | -                  | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.                            |
| KAFKA_CONSUMER_MAX_POLL_RECORDS                   | 200                | Maximum number of records returned in a single call to poll().                                                                                                                        |
| KAFKA_INSTANCE_AUTHORITY_TOPIC_PARTITIONS         | 10                 | Amount of partitions for `links.instance-authority` topic.                                                                                                                            |
| KAFKA_INSTANCE_AUTHORITY_TOPIC_REPLICATION_FACTOR | -                  | Replication factor for `links.instance-authority` topic.                                                                                                                              |
| KAFKA_AUTHORITIES_CONSUMER_CONCURRENCY            | -                  | Number of kafka concurrent threads for `inventory.authority` message consuming                                                                                                        |
| KAFKA_AUTHORITIES_CONSUMER_CONCURRENCY            | -                  | Subscription pattern for `inventory.authority` message consumers.                                                                                                                     |

## Integration

### Folio modules communication

| Module name     | Interface   | Notes                                       |
|-----------------|-------------|---------------------------------------------|
| mod-login       | login       | For system user creation and authentication |
| mod-permissions | permissions | For system user creation                    |
| mod-users       | users       | For system user creation                    |


### Consuming Kafka messages

| Topic name                         | Group ID                                   | Notes                                                     |
|------------------------------------|--------------------------------------------|-----------------------------------------------------------|
| {ENV}.[tenant].inventory.authority | {ENV}-mod-entities-links-authorities-group | Filtrating messages that have type UPDATE and DELETE only |


## API

### instance-links API
The API enables possibility to link a MARC bib field(s) to MARC authority heading(s)/reference(s) because authority records are seen as the source of truth about a person/place/corporation/conference/subject/genre.
This allows to indicate on any MARC bib records' 1XX/6XX/7XX/8XX fields that they are controlled by the linked/matched MARC authority field 1XX/4XX.
Which means that the fields are not editable on bib side, instead manual edits to a MARC authority field 1XX/4XX are reflected on them.

| METHOD | URL                                   | Required permissions                      | DESCRIPTION                                 |
|:-------|:--------------------------------------|:------------------------------------------|:--------------------------------------------|
| GET    | `/links/instances/{instanceId}`       | `entities-links.instances.collection.get` | Get links collection related to Instance    |
| PUT    | `/links/instances/{instanceId}`       | `entities-links.instances.collection.put` | Update links collection related to Instance |

#### Examples:

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
      "bibRecordTag": "100",
      "bibRecordSubfields": [
        "a",
        "b"
      ]
    }
  ],
  "totalRecords": 1
}
```
<a name='modify-instance-links'></a>
##### Modify links by the given instance id:
`PUT /links/instances/b2658a84-912b-4ed9-83d7-e8201f4d27ec`

```json
{
  "links": [
    {
      "authorityId": "875e86cf-599d-4293-b966-b34ac6a6d19e",
      "authorityNaturalId": "no50047988",
      "instanceId": "b2658a84-912b-4ed9-83d7-e8201f4d27ec",
      "bibRecordTag": "700",
      "bibRecordSubfields": [
        "a",
        "b"
      ]
    },
    {
      "authorityId": "77398964-ee2d-4b28-ba2b-e30851d5d963",
      "authorityNaturalId": "mp96352145",
      "instanceId": "b2658a84-912b-4ed9-83d7-e8201f4d27ec",
      "bibRecordTag": "710",
      "bibRecordSubfields": [
        "a",
        "b"
      ]
    }
  ]
}
```

### linking-rules API
The API enables possibility to retrieve default linking rules.
For more information, see [Linking rules](#linking-rules) topic

| METHOD | URL                                    | Required permissions                              | DESCRIPTION                                        |
|:-------|:---------------------------------------|:--------------------------------------------------|:---------------------------------------------------|
| GET    | `/linking-rules/instance-authority`    | `instance-authority.linking-rules.collection.get` | Get Instance to Authority linking rules collection |

#### Examples:

<a name="retrieve-instance-authority-linking-rules"></a>
##### Retrieve instance to authority linking rules collection:
`GET /linking-rules/instance-authority`

Response:

```json
[
    {
      "bibField": "240",
      "authorityField": "100",
      "authoritySubfields": ["f", "g", "h", "k", "l", "m", "n", "o", "p", "r", "s", "t"],
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
      }
    }
]
```

## Linking rules
### Instance to Authority linking rule parameters

* `bibField` - Instance field which would be controlled by authority field
* `authorityField` - Authority field which would be linked to instance field
* `authoritySubfields` - Array of authority subfields that can be linked to instance subfields. Should match instance subfields (in exceptions use `subfieldModifications`)
* `subfieldModifications` - Array of subfield modifications
  * `source` - Authority subfield, which would be linked to `target`
  * `target` - Instance subfield, which would be controlled by `source`
* `validation` - Linking rule validations that should be verified before linking
  * `existence` - Map <char, boolean>. Validate if subfield have to exist or not
