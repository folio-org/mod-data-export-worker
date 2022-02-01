package org.folio.dew.batch;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
public class ExportJobManagerCirculationLog extends ExportJobManager {
  public ExportJobManagerCirculationLog(JobLauncher jobLauncher, JobExplorer jobExplorer) {
    super(jobLauncher, jobExplorer);
  }
}
