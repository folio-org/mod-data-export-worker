package org.folio.dew.batch.acquisitions.edifact.jobs.decider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ExportStepDecision {

  PROCESS("PROCESS"),
  SKIP("SKIP");

  private final String status;

}
