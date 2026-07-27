package org.folio.dew.batch.eholdings;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.ResourcesAttributes;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.junit.jupiter.api.Test;

class EHoldingsAgreementItemProcessorTest {

  @Test
  @SneakyThrows
  void shouldReadAgreementsFromCache() {
    var cache = new AgreementCache();
    var agreement = new AgreementClient.Agreement();
    agreement.setName("Test");
    cache.put("1-22-333", agreement);

    var processor = new EHoldingsAgreementItemProcessor(cache);
    var dto = buildResourceDto(333);

    var result = processor.process(dto);

    assert result != null;
    assertThat(result.getAgreements()).hasSize(1);
    assertThat(result.getAgreements().getFirst().getName()).isEqualTo("Test");
  }

  @Test
  @SneakyThrows
  void shouldLeaveAgreementsEmptyWhenCacheHasNoMatch() {
    var cache = new AgreementCache();

    var processor = new EHoldingsAgreementItemProcessor(cache);
    var dto = buildResourceDto(999);
    var result = processor.process(dto);

    assert result != null;
    assertThat(result.getAgreements()).isEmpty();
  }

  private EHoldingsResourceDTO buildResourceDto(int titleId) {
    var attrs = new ResourcesAttributes();
    attrs.setPackageId("1-22");
    attrs.setTitleId(titleId);
    var data = new ResourcesData();
    data.setAttributes(attrs);
    return EHoldingsResourceDTO.builder().resourcesData(data).build();
  }
}

