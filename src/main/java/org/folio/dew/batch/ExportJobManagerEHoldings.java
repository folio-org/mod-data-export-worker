package org.folio.dew.batch;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
public class ExportJobManagerEHoldings extends ExportJobManager {
  public ExportJobManagerEHoldings(JobLauncher jobLauncher, JobExplorer jobExplorer) {
    super(jobLauncher, jobExplorer);
  }
}
