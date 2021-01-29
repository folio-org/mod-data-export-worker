package org.folio.model.repositories;

import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MinIOObjectStorageRepository {

    private MinioClient minioClient;

    public MinIOObjectStorageRepository(
            @Value("${minio.url}") String url,
            @Value("${minio.accessKey}") String accessKey,
            @Value("${minio.secretKey}") String secretKey) {
        // TODO validate input parameters here

        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    public ObjectWriteResponse uploadObject(String bucketName, String objectName, String filePath, String contentType)
            throws IOException, ServerException, InsufficientDataException, InternalException,
            InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
        // TODO validate input parameters here

        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .filename(filePath)
                .contentType(contentType)
                .build();

        ObjectWriteResponse objectWriteResponse = this.minioClient.uploadObject(uploadObjectArgs);
        return objectWriteResponse;
    }

    public ObjectWriteResponse composeObject(String bucketName, String destObjectName, List<String> sourceObjectNames)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException,
            NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        // TODO validate input parameters here

        List<ComposeSource> sourceObjects = new ArrayList<ComposeSource>();

        for (String name : sourceObjectNames) {
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket(bucketName)
                    .object(name)
                    .build();
            sourceObjects.add(composeSource);
        }

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucketName)
                .object(destObjectName)
                .sources(sourceObjects)
                .build();

        ObjectWriteResponse objectWriteResponse = minioClient.composeObject(composeObjectArgs);
        return objectWriteResponse;
    }
}
