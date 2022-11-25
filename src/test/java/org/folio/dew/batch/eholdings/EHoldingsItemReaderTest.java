package org.folio.dew.batch.eholdings;

import static java.lang.Math.ceil;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.folio.dew.batch.eholdings.EHoldingsItemReader.MAX_RETRIEVABLE_RESULTS;
import static org.folio.dew.client.KbEbscoClient.COUNT_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Log4j2
@ExtendWith(MockitoExtension.class)
class EHoldingsItemReaderTest {

  private static final int MIN_PAGE = 1;
  private static final int EXCEEDING_RESULT_NUMBER = 10_000;
  private static final int MIN_LIMIT = 1;
  private static final int MAX_LIMIT = 100;

  private final EHoldingsJobProperties jobProperties = new EHoldingsJobProperties();
  private final EHoldingsExportConfig exportConfig = new EHoldingsExportConfig()
    .recordType(EHoldingsExportConfig.RecordTypeEnum.PACKAGE)
    .titleFields(singletonList("test"));
  private final KbEbscoClient kbEbscoClient = mock(KbEbscoClient.class);

  private EHoldingsItemReader itemReader;

  @Captor
  private ArgumentCaptor<Integer> limitCaptor;

  @ParameterizedTest
  @MethodSource("getPagesLimits")
  void mustNotExceedKbEbscoPagingBound(int page, int limit) {
    jobProperties.setKbEbscoChunkSize(MAX_LIMIT);
    itemReader = spy(new EHoldingsItemReader(kbEbscoClient, exportConfig, jobProperties));
    when(kbEbscoClient.getResourcesByPackageId(any(), any()))
      .thenReturn(new EResources().data(emptyList()));

    itemReader.getItems(page, limit);

    verify(kbEbscoClient).constructParams(anyInt(), limitCaptor.capture(), anyString(), anyString());
    var actualLimit = limitCaptor.getValue();
    var actualRetrieveNumber = page * limit - limit + actualLimit;

    assertTrue(page * limit >= EXCEEDING_RESULT_NUMBER);
    assertTrue(page * actualLimit < EXCEEDING_RESULT_NUMBER);
    assertEquals(MAX_RETRIEVABLE_RESULTS, actualRetrieveNumber);
  }

  /**
   * @return Stream of arguments where 1'st is a page number and 2'nd is a limit.
   * Method is designed in way to provide test data which exceeds max possible number of results to retrieve
   * from HoldingsIQ
   * */
  private static Stream<Arguments> getPagesLimits() {
    var limits = Stream.iterate(MIN_LIMIT, i -> i <= MAX_LIMIT, i -> ++i)
      .collect(Collectors.toList());
    return Stream.iterate(MIN_PAGE, i -> i < EXCEEDING_RESULT_NUMBER, i -> ++i)
      .flatMap(page -> limits.stream()
        .filter(limit -> page * limit >= EXCEEDING_RESULT_NUMBER
          && page * limit < MAX_RETRIEVABLE_RESULTS + limit)
        .map(limit -> Arguments.of(page, limit))
      );
  }

  @SuppressWarnings("unchecked")
  @ParameterizedTest
  @ValueSource(ints = {35, 64, 50, 75, 100})
  void shouldReadAllPages(int limit) throws Exception {
    jobProperties.setKbEbscoChunkSize(limit);
    itemReader = spy(new EHoldingsItemReader(kbEbscoClient, exportConfig, jobProperties));

    doCallRealMethod().when(kbEbscoClient).constructParams(anyInt(), anyInt(), anyString(), any());
    var resourcesQueried = new AtomicLong();
    when(kbEbscoClient.getResourcesByPackageId(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var actualCount = Long.parseLong(
          ((Map<String, String>) invocationOnMock.getArgument(1)).get(COUNT_PARAM));
        resourcesQueried.getAndAdd(actualCount);
        var data = Stream.generate(ResourcesData::new)
          .limit(actualCount)
          .collect(Collectors.toList());
        log.info("Count: {}", actualCount);
        return new EResources().data(data).meta(new MetaTotalResults().totalResults(EXCEEDING_RESULT_NUMBER));
      });

    itemReader.doOpen();

    var item = (EHoldingsResourceDTO) null;
    do {
      item = itemReader.read();
    } while (item != null);

    // One is added because we first call api to get total number of resources and then - paginate
    verify(kbEbscoClient, times((int) ceil((double) MAX_RETRIEVABLE_RESULTS / limit) + 1))
      .getResourcesByPackageId(any(), any());
    // One is subtracted because we first call api to get total number of resources (with limit=1) and then - paginate
    assertEquals(MAX_RETRIEVABLE_RESULTS, resourcesQueried.get() - 1);
  }

  /**
   * Limit 1 is separated and disabled because it runs for too long (~3min) and should not be run on every build
   * But it'd be good to run it locally in case of {@link EHoldingsItemReader} changes
   */
  @Test
  @Disabled("Runs too long, should not be run for every build")
  void shouldReadAllPagesWithLimit1() throws Exception {
    shouldReadAllPages(1);
  }

}
