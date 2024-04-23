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

### Bulk edit
In case of no matched records found when uploading CSV file with items or users, link to download matched records is not available for user.
The maximum value of size for uploading file is 15MB. It could be changed with spring.servlet.multipart.max-file-size application argument.

### Memory configuration
To stable module operating the following mod-data-export-worker configuration is required: Java args -XX:MetaspaceSize=384m -XX:MaxMetaspaceSize=512m -Xmx2048m,
AWS container: memory - 3072, memory (soft limit) - 2600, cpu - 1024.

### Environment variables
This module uses separate storage of temporary (local) files for its work. These files are necessary for processing bulk-edit business flows. 
Any S3-compatible storage (AWS S3, Minio Server) supported by the Minio Client can be used as such storage. Thus, in addition to the 
AWS configuration (AWS_URL, AWS_REGION, AWS_BUCKET, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) of the permanent storage, 
one need to configure the environment settings for temporary files storage (LOCAL_FS_URL, LOCAL_FS_REGION, LOCAL_FS_BUCKET, LOCAL_FS_ACCESS_KEY_ID, LOCAL_FS_SECRET_ACCESS_KEY). 
Typically, these options must specify a separate storage. It should be noted that a single storage can also be used for the results of processing and storing temporary files, 
but in this case it is necessary to use different buckets.
It is also necessary to specify variable LOCAL_FS_COMPOSE_WITH_AWS_SDK to determine if AWS S3 is used as files storage. By default this variable is `false` and means that MinIO server is used as files storage.
This value should be `true` if AWS S3 is used as storage.

| Name                                              | Default value          | Description                                                                                                                                                                                           |
|:--------------------------------------------------|:-----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KAFKA_HOST                                        | localhost              | Kafka broker hostname                                                                                                                                                                                 |
| KAFKA_PORT                                        | 9092                   | Kafka broker port                                                                                                                                                                                     |
| KAFKA_CONSUMER_POLL_INTERVAL                      | 3600000                | Max interval before next poll. If long record processing is in place and interval exceeded then consumer will be kicked out of the group and another consumer will start processing the same message. |
| ENV                                               | folio                  | Environment name                                                                                                                                                                                      |
| AWS_URL                                           | http://127.0.0.1:9000/ | AWS url                                                                                                                                                                                               |
| AWS_REGION                                        | -                      | AWS region                                                                                                                                                                                            |
| AWS_BUCKET                                        | -                      | AWS bucket                                                                                                                                                                                            |
| AWS_ACCESS_KEY_ID                                 | -                      | AWS access key                                                                                                                                                                                        |
| AWS_SECRET_ACCESS_KEY                             | -                      | AWS secret key                                                                                                                                                                                        |
| LOCAL_FS_URL                                      | http://127.0.0.1:9000/ | S3-compatible local files storage url                                                                                                                                                                 |
| LOCAL_FS_REGION                                   | -                      | S3-compatible local files storage region                                                                                                                                                              |
| LOCAL_FS_BUCKET                                   | -                      | S3-compatible local files storage bucket                                                                                                                                                              |
| LOCAL_FS_ACCESS_KEY_ID                            | -                      | S3-compatible local files storage access key                                                                                                                                                          |
| LOCAL_FS_SECRET_ACCESS_KEY                        | -                      | S3-compatible local files storage secret key                                                                                                                                                          |
| URL_EXPIRATION_TIME                               | 604800                 | Presigned url expiration time (in seconds)                                                                                                                                                            |
| DATA_EXPORT_JOB_UPDATE_TOPIC_PARTITIONS           | 50                     | Number of partitions for topic                                                                                                                                                                        |
| KAFKA_CONCURRENCY_LEVEL                           | 30                     | Concurrency level of kafka listener                                                                                                                                                                   |
| LOCAL_FS_COMPOSE_WITH_AWS_SDK                     | false                  | Specify if AWS S3 is used as local files storage                                                                                                                                                      |
| E_HOLDINGS_BATCH_JOB_CHUNK_SIZE                   | 100                    | Specify chunk size for eHoldings export job which will be used to query data from kb-ebsco, write to database, read from database and write to file                                                   |
| E_HOLDINGS_BATCH_KB_EBSCO_CHUNK_SIZE              | 100                    | Amount to retrieve per request to mod-kb-ebsco-java (100 is max acceptable value)                                                                                                                     |
| AUTHORITY_CONTROL_BATCH_JOB_CHUNK_SIZE            | 100                    | Specify chunk size for authority control export job which will be used to query data from entities-links, and write to file                                                                           |
| AUTHORITY_CONTROL_BATCH_ENTITIES_LINKS_CHUNK_SIZE | 100                    | Amount to retrieve per request to mod-entities-links                                                                                                                                                  |
| MAX_UPLOADED_FILE_SIZE                            | 40MB                   | Specifies multipart upload file size                                                                                                                                                                  |
