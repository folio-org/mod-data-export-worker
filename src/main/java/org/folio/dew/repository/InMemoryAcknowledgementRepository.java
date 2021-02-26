package org.folio.dew.repository;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Repository;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Repository
public class InMemoryAcknowledgementRepository implements IAcknowledgementRepository {

  private final ConcurrentHashMap<String, Acknowledgment> acknowledgmentStorage =
      new ConcurrentHashMap<>();

  public void addAcknowledgement(String id, Acknowledgment acknowledgment) {
    this.acknowledgmentStorage.putIfAbsent(id, acknowledgment);
  }

  public Acknowledgment getAcknowledgement(String id) {
    return this.acknowledgmentStorage.get(id);
  }

  public Acknowledgment deleteAcknowledgement(String id) {
    return this.acknowledgmentStorage.remove(id);
  }
}
