# Development notes
## Local testing
### Vagrant box setup
It's not possible to fully use mod-data-export-worker with vagrant box for now because there's no AWS configuration for it.
In order to set it up you need to perform multiple steps:
- bring up folio Vagrant box.
- enter Vagrant box with `vagrant ssh`
- run `docker run --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack -e LOCALSTACK_SERVICES=s3` command to bring up localstack with aws s3 mock
- run `docker exec -it {localstackContainerId} bash` to enter localstack container
- run `awslocal s3api create-bucket --bucket sample-bucket` to create bucket
- then you need to replace `application.minio` properties of `application.yml` configuration file in mod-data-export worker with following (Note: ip address may be changed by should work as is):
```yaml
minio:
    endpoint: ${AWS_URL:http://172.17.0.71:4566}
    region: ${AWS_REGION:us-east-1}
    bucket: ${AWS_BUCKET:sample-bucket}
    accessKey: ${AWS_ACCESS_KEY_ID:test}
    secretKey: ${AWS_SECRET_ACCESS_KEY:test}
```
- to finish, build module and replace it in Vagrant box
- now you should be able to use export in local Vagrant box
