# mod-data-export-worker

Copyright (C) 2021-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file [LICENSE](LICENSE) for more information.

## Introduction
API for Data Export Worker module.

## Additional information
More detail can be found on Data Export Worker wiki-page: [WIKI Data Export Worker](https://wiki.folio.org/pages/viewpage.action?pageId=52134948).

### Issue tracker
See project [MODEXPW](https://issues.folio.org/browse/MODEXPW)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation
Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/)

### Memory configuration
To stable module operating the following mod-data-export-worker configuration is required: Java args -XX:MetaspaceSize=384m -XX:MaxMetaspaceSize=512m -Xmx2048m,
AWS container: memory - 3072, memory (soft limit) - 2600, cpu - 1024.

### Environment variables
Any S3-compatible storage (AWS S3, Minio Server) supported by the Minio Client can be used as such storage. Thus, in addition to the 
AWS configuration (AWS_URL, AWS_REGION, AWS_BUCKET, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) of the permanent storage, 
one need to configure the environment settings for s3 subpathes (S3_SUB_PATH, S3_LOCAL_SUB_PATH). 
Typically, these options must specify a separate pathes.
It is also necessary to specify variable S3_IS_AWS to determine if AWS S3 is used as files storage. By default this variable is `false` and means that MinIO server is used as files storage.
This value should be `true` if AWS S3 is used as storage.

| Name                                              | Default value                 | Description                                                                                                                                                                                           |
|:--------------------------------------------------|:------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KAFKA_HOST                                        | localhost                     | Kafka broker hostname                                                                                                                                                                                 |
| KAFKA_PORT                                        | 9092                          | Kafka broker port                                                                                                                                                                                     |
| KAFKA_CONSUMER_POLL_INTERVAL                      | 3600000                       | Max interval before next poll. If long record processing is in place and interval exceeded then consumer will be kicked out of the group and another consumer will start processing the same message. |
| OKAPI_URL                                         | http://okapi:9130             | Okapi url                                                                                                                                                                                             |
| ENV                                               | folio                         | Environment name                                                                                                                                                                                      |
| S3_URL                                            | http://127.0.0.1:9000/        | AWS url                                                                                                                                                                                               |
| S3_REGION                                         | -                             | AWS region                                                                                                                                                                                            |
| S3_BUCKET                                         | -                             | AWS bucket                                                                                                                                                                                            |
| S3_ACCESS_KEY_ID                                  | -                             | AWS access key                                                                                                                                                                                        |
| S3_SECRET_ACCESS_KEY                              | -                             | AWS secret key                                                                                                                                                                                        |
| S3_SUB_PATH                                       | mod-data-export-worker/remote | S3 subpath for files storage                                                                                                                                                                          |
| S3_LOCAL_SUB_PATH                                 | mod-data-export-worker/local  | S3 subpath for local files storage                                                                                                                                                                    |
| S3_IS_AWS                                         | false                         | Specify if AWS S3 is used as files storage                                                                                                                                                            |
| URL_EXPIRATION_TIME                               | 604800                        | Presigned url expiration time (in seconds)                                                                                                                                                            |
| DATA_EXPORT_JOB_UPDATE_TOPIC_PARTITIONS           | 50                            | Number of partitions for topic                                                                                                                                                                        |
| KAFKA_CONCURRENCY_LEVEL                           | 30                            | Concurrency level of kafka listener                                                                                                                                                                   |
| E_HOLDINGS_BATCH_JOB_CHUNK_SIZE                   | 100                           | Specify chunk size for eHoldings export job which will be used to query data from kb-ebsco, write to database, read from database and write to file                                                   |
| E_HOLDINGS_BATCH_KB_EBSCO_CHUNK_SIZE              | 100                           | Amount to retrieve per request to mod-kb-ebsco-java (100 is max acceptable value)                                                                                                                     |
| E_HOLDINGS_BATCH_ENTITLEMENTS_PER_PAGE            | 100                           | Page size for bulk entitlements lookup                                                                                                                                                                |
| AUTHORITY_CONTROL_BATCH_JOB_CHUNK_SIZE            | 100                           | Specify chunk size for authority control export job which will be used to query data from entities-links, and write to file                                                                           |
| AUTHORITY_CONTROL_BATCH_ENTITIES_LINKS_CHUNK_SIZE | 100                           | Amount to retrieve per request to mod-entities-links                                                                                                                                                  |
| MAX_UPLOADED_FILE_SIZE                            | 40MB                          | Specifies multipart upload file size                                                                                                                                                                  |
| PLATFORM                                          | okapi                         | Specifies if okapi or eureka platform                                                                                                                                                                 |
| CHUNKS                                            | 100                           | Number of items being passed to write at once                                                                                                                                                         |
| CORE_POOL_SIZE                                    | 10                            | Maximum number of threads being created for each task before the queue is utilized                                                                                                                    |
| MAX_POOL_SIZE                                     | 10                            | Maximum number of threads that can be created after the queue is full and before rejecting the new tasks                                                                                              |
| BUCKET_SIZE                                       | 50                            | Size of the bucket used in partitioning parameters                                                                                                                                                   |

### Order Email — Template Context Payload

This is the `context` payload sent to **mod-template-engine** when an `EDIFACT_ORDERS_EXPORT` job is transmitted by **Email**.

#### Structure overview

```
OrderEmailContext
├── createdAt                                # context build time, ISO-8601 UTC with millis (e.g. 2026-03-30T16:22:13.284Z)
├── organization
│   ├── name
│   └── primaryAddress                       # address flagged isPrimary
│       ├── addressLine1
│       ├── city
│       ├── zipCode
│       └── country
└── orders[]                                 # multiple entries
    ├── order
    │   ├── poNumber
    │   ├── orderType
    │   ├── metadata
    │   │   └── createdByUser                # resolved from metadata.createdByUserId via mod-users
    │   │       ├── id
    │   │       ├── firstName
    │   │       ├── lastName
    │   │       └── fullName
    │   ├── shipTo                           # resolved from shipTo UUID via tenant-addresses
    │   │   ├── id
    │   │   └── address
    │   └── billTo                           # resolved from billTo UUID via tenant-addresses
    │       ├── id
    │       └── address
    └── orderLines[]                         # multiple entries
        └── orderLine
            ├── poLineNumber
            ├── titleOrPackage
            ├── publisher
            ├── publicationDate
            ├── edition
            ├── rush
            ├── contributors[]               # multiple entries
            │   ├── contributor
            │   └── contributorNameType      # resolved type name (e.g. Personal name, Corporate name)
            │       ├── id
            │       └── name                 
            ├── details
            │   └── productIds[]             # multiple entries
            │       ├── productId
            │       ├── qualifier
            │       └── productIdType        # resolved type name (e.g. ISBN, ASIN)
            │           ├── id
            │           └── name
            ├── cost
            │   ├── listUnitPrice
            │   ├── listUnitPriceElectronic
            │   ├── quantityPhysical
            │   ├── quantityElectronic
            │   ├── poLineEstimatedPrice
            │   └── currency
            ├── fundDistribution[]           # multiple entries
            │   └── code                     # code taken as-is from the PO line; fundId is not resolved
            └── vendorDetail 
                └── instructions             # vendor instructions
```
> **Null/empty policy:** missing values are rendered as safe defaults rather than
> `null`, so templates can reference any field without null checks.

#### Example payload

```json
{
  "createdAt": "2026-03-30T16:22:13.284Z",
  "organization": {
    "name": "GOBI Library Solutions",
    "primaryAddress": {
      "addressLine1": "1 Innovation Way",
      "city": "Contoocook",
      "zipCode": "03229",
      "country": "USA"
    }
  },
  "orders": [
    {
      "order": {
        "poNumber": "10000",
        "orderType": "One-Time",
        "metadata": {
          "createdByUser": {
            "id": "7a626480-284e-5b55-9cf2-db32f93956cf",
            "firstName": "John",
            "lastName": "Doe",
            "fullName": "John Doe"
          }
        },
        "shipTo": {
          "id": "11111111-1111-1111-1111-111111111111",
          "address": "SLUB Dresden, Zellescher Weg 18, 01069 Dresden"
        },
        "billTo": {
          "id": "22222222-2222-2222-2222-222222222222",
          "address": "Accounts Payable, PO Box 42, Springfield IL"
        }
      },
      "orderLines": [
        {
          "orderLine": {
            "poLineNumber": "10000-1",
            "titleOrPackage": "Introduction to FOLIO",
            "publisher": "FOLIO Press",
            "publicationDate": "2020",
            "edition": "2nd",
            "rush": false,
            "contributors": [
              {
                "contributor": "Jane Author",
                "contributorNameType": {
                  "id": "2b94c631-fca9-4892-a730-03ee529ffe2a",
                  "name": "Personal name"
                }
              }
            ],
            "details": {
              "productIds": [
                {
                  "productId": "978-3-16-148410-0",
                  "qualifier": "",
                  "productIdType": {
                    "id": "8261054f-be78-422d-bd51-4ed9f33c3422",
                    "name": "ISBN"
                  }
                }
              ]
            },
            "cost": {
              "listUnitPrice": "49.99",
              "listUnitPriceElectronic": "0.00",
              "quantityPhysical": 2,
              "quantityElectronic": 0,
              "poLineEstimatedPrice": "99.98",
              "currency": "USD"
            },
            "fundDistribution": [
              {
                "code": "USHIST"
              }
            ],
            "vendorDetail": {
              "instructions": "Deliver to loading dock, ring bell on arrival"
            }
          }
        }
      ]
    }
  ]
}
```       
