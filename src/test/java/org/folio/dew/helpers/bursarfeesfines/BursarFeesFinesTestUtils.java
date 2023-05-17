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
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTransferCriteria;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

@UtilityClass
public class BursarFeesFinesTestUtils {

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
}
