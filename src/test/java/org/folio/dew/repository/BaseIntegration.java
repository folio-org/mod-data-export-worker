package org.folio.dew.repository;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Log4j2
public abstract class BaseIntegration {

  @Container
  public static final LocalStackContainer localstack
      = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.10.0"))
      .withServices(S3)
      .waitingFor(
          Wait.forHttp("/_localstack/health")
              .forPort(4566)
              .forStatusCode(200)
              .forResponsePredicate(s -> s.contains("\"s3\": \"running\""))
              .withStartupTimeout(Duration.ofMinutes(1))
      )
      .withEnv("EAGER_SERVICE_LOADING", "1");


  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    String endpoint = localstack.getEndpointOverride(S3).toString();
    String region   = localstack.getRegion();
    String access   = localstack.getAccessKey();
    String secret   = localstack.getSecretKey();

    r.add("application.minio-local.endpoint", () -> endpoint);
    r.add("application.minio-local.region",   () -> region);
    r.add("application.minio-local.accessKey",() -> access);
    r.add("application.minio-local.secretKey",() -> secret);

    r.add("application.minio-remote.endpoint", () -> endpoint);
    r.add("application.minio-remote.region",   () -> region);
    r.add("application.minio-remote.accessKey",() -> access);
    r.add("application.minio-remote.secretKey",() -> secret);
  }
}
