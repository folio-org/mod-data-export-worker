package org.folio.dew.helpers.bursarfeesfines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportHeaderFooter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTokenAggregate;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTokenItemData;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;
import org.folio.dew.domain.dto.BursarExportTokenUserData;
import org.folio.dew.domain.dto.BursarExportTransferCriteria;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

@UtilityClass
public class BursarFeesFinesTestUtils {

  public static final String USERS_ENDPOINT_PATH = "/users";
  public static final String ITEMS_ENDPOINT_PATH = "/inventory/items";

  public static final String ALL_OPEN_ACCOUNTS_GET_REQUEST =
    "/accounts?query=remaining%20%3E%200.0&limit=10000";
  public static final String TRANSFERS_ENDPOINT_PATH = "/transfers";

  public static final String SERVICE_POINTS_GET_REQUEST =
    "/service-points?query=code%3D%3Dsystem&limit=2";

  public static final String TRANSFER_ACCOUNTS_ENDPOINT =
    "/accounts-bulk/transfer";

  public static JobParameters prepareNoFeesFinesJobParameters(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();
    List<BursarExportDataToken> dataTokens = new ArrayList<>();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();

    BursarExportTokenFeeMetadata tokenFeeMetadata = new BursarExportTokenFeeMetadata();
    tokenFeeMetadata.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);
    dataTokens.add(tokenFeeMetadata);

    job.setData(dataTokens);
    job.setFilter(filterPass);
    job.setGroupByPatron(false);

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();

    List<BursarExportTransferCriteriaConditionsInner> transferConditions = new ArrayList<>();

    BursarExportTransferCriteriaElse transferInfo = new BursarExportTransferCriteriaElse();
    transferInfo.setAccount(
      UUID.fromString("998ecb15-9f5d-4674-b288-faad24e44c0b")
    );

    transferCriteria.setConditions(transferConditions);
    transferCriteria.setElse(transferInfo);

    job.setTransferInfo(transferCriteria);

    var parametersBuilder = new JobParametersBuilder();
    parametersBuilder.addString(
      "bursarFeeFines",
      objectMapper.writeValueAsString(job)
    );

    String jobId = "00000000-0000-4000-8000-000000000000";
    parametersBuilder.addString(JobParameterNames.JOB_ID, jobId);

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir") +
      File.separator +
      springApplicationName +
      File.separator;
    final String outputFile = String.format(
      "%s%s_%tF_%tH%tM%tS_%s",
      workDir,
      ExportType.BURSAR_FEES_FINES,
      now,
      now,
      now,
      now,
      jobId
    );
    parametersBuilder.addString(
      JobParameterNames.TEMP_OUTPUT_FILE_PATH,
      outputFile
    );

    return parametersBuilder.toJobParameters();
  }

  public static JobParameters prepareOneFeeFineMatchingJobParameters(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterAmount filterAmount = new BursarExportFilterAmount();
    filterAmount.setCondition(BursarExportFilterAmount.ConditionEnum.LESS_THAN);
    filterAmount.setAmount(20000);
    job.setFilter(filterAmount);

    BursarExportTokenLengthControl lengthControl = new BursarExportTokenLengthControl();
    lengthControl.setLength(30);
    lengthControl.setCharacter(" ");
    lengthControl.setDirection(
      BursarExportTokenLengthControl.DirectionEnum.BACK
    );
    lengthControl.setTruncate(true);

    List<BursarExportHeaderFooter> headerTokens = new ArrayList<>();
    BursarExportTokenAggregate headerAggregate = new BursarExportTokenAggregate();
    headerAggregate.setValue(BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT);
    headerAggregate.setDecimal(true);
    headerTokens.add(headerAggregate);

    BursarExportTokenConstant newLineToken = new BursarExportTokenConstant();
    newLineToken.setValue("\n");
    headerTokens.add(newLineToken);

    job.setHeader(headerTokens);

    List<BursarExportHeaderFooter> footerTokens = new ArrayList<>();
    BursarExportTokenAggregate footerAggregate = new BursarExportTokenAggregate();
    footerAggregate.setValue(BursarExportTokenAggregate.ValueEnum.NUM_ROWS);
    footerAggregate.setDecimal(false);
    footerTokens.add(footerAggregate);

    job.setFooter(footerTokens);

    List<BursarExportDataToken> dataTokens = new ArrayList<>();

    BursarExportTokenFeeMetadata tokenFeeMetadata = new BursarExportTokenFeeMetadata();
    tokenFeeMetadata.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);

    BursarExportTokenItemData tokenItemData = new BursarExportTokenItemData();
    tokenItemData.setValue(BursarExportTokenItemData.ValueEnum.NAME);
    tokenItemData.setLengthControl(lengthControl);

    BursarExportTokenUserData tokenUserData = new BursarExportTokenUserData();
    tokenUserData.setLengthControl(lengthControl);
    tokenUserData.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);

    dataTokens.add(tokenFeeMetadata);
    dataTokens.add(tokenItemData);
    dataTokens.add(tokenUserData);
    dataTokens.add(newLineToken);

    job.setData(dataTokens);
    job.setGroupByPatron(false);

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();

    List<BursarExportTransferCriteriaConditionsInner> transferConditions = new ArrayList<>();

    BursarExportTransferCriteriaElse transferInfo = new BursarExportTransferCriteriaElse();
    transferInfo.setAccount(
      UUID.fromString("998ecb15-9f5d-4674-b288-faad24e44c0b")
    );

    transferCriteria.setConditions(transferConditions);
    transferCriteria.setElse(transferInfo);

    job.setTransferInfo(transferCriteria);

    var parametersBuilder = new JobParametersBuilder();
    parametersBuilder.addString(
      "bursarFeeFines",
      objectMapper.writeValueAsString(job)
    );

    String jobId = "00000000-0000-4000-8000-000000000000";
    parametersBuilder.addString(JobParameterNames.JOB_ID, jobId);

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir") +
      File.separator +
      springApplicationName +
      File.separator;
    final String outputFile = String.format(
      "%s%s_%tF_%tH%tM%tS_%s",
      workDir,
      ExportType.BURSAR_FEES_FINES,
      now,
      now,
      now,
      now,
      jobId
    );
    parametersBuilder.addString(
      JobParameterNames.TEMP_OUTPUT_FILE_PATH,
      outputFile
    );

    return parametersBuilder.toJobParameters();
  }
}
