package org.folio.dew.repository;

import org.springframework.kafka.support.Acknowledgment;

public interface IAcknowledgementRepository {

  void addAcknowledgement(String id, Acknowledgment acknowledgment);

  Acknowledgment getAcknowledgement(String id);

  Acknowledgment deleteAcknowledgement(String id);
}
