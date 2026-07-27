package org.folio.dew.batch.eholdings;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.dew.client.AgreementClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgreementCacheTest {

  private AgreementCache cache;

  @BeforeEach
  void setUp() {
    cache = new AgreementCache();
  }

  @Test
  void shouldBeEmptyWhenNoDataAdded() {
    assertThat(cache.isEmpty()).isTrue();
  }

  @Test
  void shouldNotBeEmptyAfterAddingData() {
    cache.put("1-22-333", createAgreement("Test Agreement"));

    assertThat(cache.isEmpty()).isFalse();
  }

  @Test
  void shouldReturnEmptyListWhenKeyNotFound() {
    cache.put("1-22-333", createAgreement("Test"));

    assertThat(cache.get("1-22-999")).isEmpty();
  }

  @Test
  void shouldReturnEmptyListWhenCacheIsEmpty() {
    assertThat(cache.get("1-22-333")).isEmpty();
  }

  @Test
  void shouldReturnAgreementsForMatchingKey() {
    cache.put("1-22-333", createAgreement("Agreement A"));
    cache.put("1-22-333", createAgreement("Agreement B"));

    var result = cache.get("1-22-333");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(AgreementClient.Agreement::getName)
      .containsExactly("Agreement A", "Agreement B");
  }

  @Test
  void shouldStoreMultipleResources() {
    cache.put("1-22-100", createAgreement("Agreement 1"));
    cache.put("1-22-200", createAgreement("Agreement 2"));
    cache.put("1-22-300", createAgreement("Agreement 3"));
    cache.put("1-22-300", createAgreement("Agreement 4"));
    cache.put("1-22-300", createAgreement("Agreement 5"));

    assertThat(cache.get("1-22-100")).hasSize(1);
    assertThat(cache.get("1-22-200")).hasSize(1);
    assertThat(cache.get("1-22-300")).hasSize(3);
  }

  private AgreementClient.Agreement createAgreement(String name) {
    var agreement = new AgreementClient.Agreement();
    agreement.setName(name);
    agreement.setStatus("Active");
    agreement.setStartDate("2024-01-01");
    return agreement;
  }
}

