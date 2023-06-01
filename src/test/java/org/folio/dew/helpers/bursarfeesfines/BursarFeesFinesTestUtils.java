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
import org.folio.dew.domain.dto.BursarExportFilterAggregate;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportHeaderFooter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTokenAggregate;
import org.folio.dew.domain.dto.BursarExportTokenConditional;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
import org.folio.dew.domain.dto.BursarExportTokenCurrentDate;
import org.folio.dew.domain.dto.BursarExportTokenDateType;
import org.folio.dew.domain.dto.BursarExportTokenFeeAmount;
import org.folio.dew.domain.dto.BursarExportTokenFeeDate;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTokenItemData;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;
import org.folio.dew.domain.dto.BursarExportTokenUserData;
import org.folio.dew.domain.dto.BursarExportTokenUserDataOptional;
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

  public static JobParameters prepareNoFeeFineMatchingJobParameters(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterNegation filterNegation = new BursarExportFilterNegation();
    filterNegation.setCriteria(new BursarExportFilterPass());
    job.setFilter(filterNegation);

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
    dataTokens.add(tokenFeeMetadata);

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

    String jobId = "00000000-0000-1000-2000-000000000000";
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

  public static JobParameters prepareMultipleFeeFinesMatchingJobParameters(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterAmount filterAmount = new BursarExportFilterAmount();
    filterAmount.setAmount(10000);
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN
    );
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

    String jobId = "00000000-0000-1000-1000-000000000000";
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

  public static JobParameters prepareUseMostOutputTokensTest(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();
    job.setFilter(filterPass);

    List<BursarExportHeaderFooter> headerTokens = new ArrayList<>();
    BursarExportTokenAggregate headerAggregateAmount = new BursarExportTokenAggregate();
    headerAggregateAmount.setValue(
      BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT
    );
    headerAggregateAmount.setDecimal(true);
    headerTokens.add(headerAggregateAmount);
    BursarExportTokenAggregate headerAggregateNumRows = new BursarExportTokenAggregate();
    headerAggregateNumRows.setValue(
      BursarExportTokenAggregate.ValueEnum.NUM_ROWS
    );
    headerAggregateNumRows.setDecimal(true);
    headerTokens.add(headerAggregateNumRows);
    BursarExportTokenConstant newLineToken = new BursarExportTokenConstant();
    newLineToken.setValue("\n");
    headerTokens.add(newLineToken);

    job.setHeader(headerTokens);

    List<BursarExportHeaderFooter> footerTokens = new ArrayList<>();
    footerTokens.add(headerAggregateAmount);
    footerTokens.add(headerAggregateNumRows);

    job.setFooter(footerTokens);

    List<BursarExportDataToken> dataTokens = new ArrayList<>();

    BursarExportTokenFeeMetadata tokenFeeMetadata = new BursarExportTokenFeeMetadata();
    tokenFeeMetadata.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);

    BursarExportTokenItemData tokenItemData = new BursarExportTokenItemData();
    tokenItemData.setValue(BursarExportTokenItemData.ValueEnum.NAME);

    BursarExportTokenUserData tokenUserData = new BursarExportTokenUserData();
    tokenUserData.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);

    BursarExportTokenFeeAmount tokenFeeAmount = new BursarExportTokenFeeAmount();
    tokenFeeAmount.setDecimal(true);

    BursarExportTokenFeeDate tokenFeeDate = new BursarExportTokenFeeDate();
    tokenFeeDate.setValue(BursarExportTokenDateType.YEAR_LONG);
    tokenFeeDate.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    tokenFeeDate.setTimezone("UTC");
    tokenFeeDate.setPlaceholder(" ");
    tokenFeeDate.setLengthControl(null);

    BursarExportTokenUserDataOptional tokenUserDataOptional = new BursarExportTokenUserDataOptional();
    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.LAST_NAME
    );

    BursarExportTokenCurrentDate tokenCurrentDate = new BursarExportTokenCurrentDate();
    tokenCurrentDate.setValue(BursarExportTokenDateType.YEAR_LONG);
    tokenCurrentDate.setTimezone("UTC");
    BursarExportTokenConditional tokenConditional = new BursarExportTokenConditional();
    tokenConditional.setElse(tokenCurrentDate);

    dataTokens.add(tokenFeeMetadata);
    dataTokens.add(tokenItemData);
    dataTokens.add(tokenUserData);
    dataTokens.add(tokenFeeAmount);
    dataTokens.add(tokenFeeDate);
    dataTokens.add(tokenUserDataOptional);
    dataTokens.add(tokenConditional);
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

  public static JobParameters prepareNoFeeFineMatchingAggregateCriteriaAggregateTest(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();
    job.setFilter(filterPass);

    BursarExportFilterAggregate filterAggregate = new BursarExportFilterAggregate();
    filterAggregate.setProperty(
      BursarExportFilterAggregate.PropertyEnum.TOTAL_AMOUNT
    );
    filterAggregate.setAmount(100000);
    filterAggregate.setCondition(
      BursarExportFilterAggregate.ConditionEnum.GREATER_THAN
    );
    job.setGroupByPatronFilter(filterAggregate);

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

    BursarExportTokenUserData tokenUserData = new BursarExportTokenUserData();
    tokenUserData.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);

    BursarExportTokenAggregate tokenAggregate = new BursarExportTokenAggregate();
    tokenAggregate.setValue(BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT);
    tokenAggregate.setDecimal(true);

    dataTokens.add(tokenUserData);
    dataTokens.add(tokenAggregate);
    job.setData(dataTokens);

    job.setGroupByPatron(true);

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

    String jobId = "00000000-0000-3000-6000-000000000000";
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

  public static JobParameters prepareOneFeeFineOnOneAccountAggregateTest(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();
    BursarExportFilterPass filterPass = new BursarExportFilterPass();
    job.setFilter(filterPass);
    job.setGroupByPatronFilter(null);

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

    BursarExportTokenUserData tokenUser = new BursarExportTokenUserData();
    tokenUser.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);

    BursarExportTokenAggregate tokenAggregate = new BursarExportTokenAggregate();
    tokenAggregate.setDecimal(true);
    tokenAggregate.setValue(BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT);

    dataTokens.add(tokenUser);
    dataTokens.add(tokenAggregate);
    dataTokens.add(newLineToken);

    job.setData(dataTokens);

    job.setGroupByPatron(true);

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

    String jobId = "00000000-0000-3000-6000-000000000000";
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

  public static JobParameters prepareMultipleFeeFinesAcrossPatronsAggregateTest(
    String springApplicationName,
    ObjectMapper objectMapper
  ) throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();
    job.setFilter(filterPass);
    job.setGroupByPatronFilter(null);

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

    BursarExportTokenUserData tokenUserData = new BursarExportTokenUserData();
    tokenUserData.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);

    BursarExportTokenAggregate tokenAggregateAmount = new BursarExportTokenAggregate();
    tokenAggregateAmount.setValue(
      BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT
    );
    tokenAggregateAmount.setDecimal(true);
    BursarExportTokenAggregate tokenAggregateNumRows = new BursarExportTokenAggregate();
    tokenAggregateNumRows.setValue(
      BursarExportTokenAggregate.ValueEnum.NUM_ROWS
    );
    tokenAggregateNumRows.setDecimal(false);

    dataTokens.add(tokenUserData);
    dataTokens.add(tokenAggregateAmount);
    dataTokens.add(tokenAggregateNumRows);
    dataTokens.add(newLineToken);
    job.setData(dataTokens);

    job.setGroupByPatron(true);

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

    String jobId = "00000000-0000-3000-6000-000000000000";
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
