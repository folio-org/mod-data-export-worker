package org.folio.dew.domain.dto.bursarfeesfines;

import java.util.List;
import lombok.Data;

@Data
public class TransferRequest {
  private List<String> accountIds;
  private double amount;
  private String servicePointId;
  private String userName;
  private String paymentMethod;
  private boolean notifyPatron;
}
