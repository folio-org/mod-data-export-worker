package org.folio.model.repositories;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Repository
public class InMemoryAcknowledgementRepository implements IAcknowledgementRepository {

    private ConcurrentHashMap<String, Acknowledgment> acknowledgmentStorage = new ConcurrentHashMap<>();

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
