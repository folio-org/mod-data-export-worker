# mod-data-export-worker

Copyright (C) 2021 The Open Library Foundation

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

### Environment variables
| Name                         | Default value          | Description                                                                                                                                                                                           |
|:-----------------------------|:-----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KAFKA_HOST                   | localhost              | Kafka broker hostname                                                                                                                                                                                 |
| KAFKA_PORT                   | 9092                   | Kafka broker port                                                                                                                                                                                     |
| KAFKA_CONSUMER_POLL_INTERVAL | 3600000                | Max interval before next poll. If long record processing is in place and interval exceeded then consumer will be kicked out of the group and another consumer will start processing the same message. |
| ENV                          | folio                  | Environment name                                                                                                                                                                                      |
| AWS_URL                      | http://127.0.0.1:9000/ | AWS url                                                                                                                                                                                               |
| AWS_REGION                   | -                      | AWS region                                                                                                                                                                                            |
| AWS_BUCKET                   | -                      | AWS bucket                                                                                                                                                                                            |
| AWS_ACCESS_KEY_ID            | -                      | AWS access key                                                                                                                                                                                        |
| AWS_SECRET_ACCESS_KEY        | -                      | AWS secret key                                                                                                                                                                                        |
