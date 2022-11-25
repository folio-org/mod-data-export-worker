package org.folio.dew.batch.eholdings;

import static java.util.Collections.singletonList;
import static org.folio.dew.batch.eholdings.EHoldingsItemReader.MAX_RETRIEVABLE_RESULTS;
import static org.folio.dew.client.KbEbscoClient.COUNT_PARAM;
import static org.folio.dew.client.KbEbscoClient.PAGE_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.MetaTotalResults;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@Log4j2
@ExtendWith(MockitoExtension.class)
class EHoldingsItemReaderTest {

  private static final int EXCEEDING_RESULT_NUMBER = 10_000;
  private static final int MIN_LIMIT = 1;
  private static final int MAX_LIMIT = 100;

  private final EHoldingsJobProperties jobProperties = new EHoldingsJobProperties();
  private final EHoldingsExportConfig exportConfig = new EHoldingsExportConfig()
    .recordType(EHoldingsExportConfig.RecordTypeEnum.PACKAGE)
    .titleFields(singletonList("test"));
  private final KbEbscoClient kbEbscoClient = mock(KbEbscoClient.class);

  /**
   * Verifies that pagination goes the right way, not exceeding upper bound of api
   * */
  @SuppressWarnings("unchecked")
  @ParameterizedTest
  @MethodSource("getLimits")
  void shouldReadAllPages(int limit) throws Exception {
    jobProperties.setKbEbscoChunkSize(limit);
    var itemReader = spy(new EHoldingsItemReader(kbEbscoClient, exportConfig, jobProperties));

    doCallRealMethod().when(kbEbscoClient).constructParams(anyInt(), anyInt(), anyString(), any());

    var resourcesQueried = new AtomicLong();
    var previousPage = new AtomicLong();
    var previousLimit = new AtomicLong();
    when(kbEbscoClient.getResourcesByPackageId(any(), any()))
      .thenReturn(new EResources().meta(new MetaTotalResults().totalResults(EXCEEDING_RESULT_NUMBER)))
      .thenAnswer(invocationOnMock -> {
        var actualPage = Long.parseLong(
          ((Map<String, String>) invocationOnMock.getArgument(1)).get(PAGE_PARAM));
        var actualLimit = Long.parseLong(
          ((Map<String, String>) invocationOnMock.getArgument(1)).get(COUNT_PARAM));

        if (previousPage.get() != 0) {
          assertEquals(previousPage.get() * previousLimit.get() + actualLimit, actualPage * actualLimit);
        }
        previousPage.set(actualPage);
        previousLimit.set(actualLimit);
        resourcesQueried.getAndAdd(actualLimit);
        var data = Stream.generate(ResourcesData::new)
          .limit(actualLimit)
          .collect(Collectors.toList());
        return new EResources().data(data).meta(new MetaTotalResults().totalResults(EXCEEDING_RESULT_NUMBER));
      });

    itemReader.doOpen();

    var item = (EHoldingsResourceDTO) null;
    do {
      item = itemReader.read();
    } while (item != null);

    assertEquals(MAX_RETRIEVABLE_RESULTS, resourcesQueried.get());
  }

  /**
   * Get possible limits. Omit those which will not trigger pagination values customization
   * */
  private static Stream<Integer> getLimits() {
    return Stream.iterate(MIN_LIMIT, i -> i <= MAX_LIMIT, i -> ++i)
      .filter(num -> MAX_RETRIEVABLE_RESULTS % num != 0);
  }

}
