package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkEditParseServiceTest extends BaseBatchTest {
  @Autowired
  private BulkEditParseService bulkEditParseService;

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldIgnoreBlankBarcodeAndExternalSystemId(String val) {
    var userFormat = UserFormat.builder()
      .patronGroup("PatronGroup")
      .externalSystemId(val)
      .barcode(val)
      .active("true")
      .departments("")
      .proxyFor("")
      .addresses("")
      .build();

    assertThat(bulkEditParseService.mapUserFormatToUser(userFormat).getExternalSystemId()).isNull();
    assertThat(bulkEditParseService.mapUserFormatToUser(userFormat).getBarcode()).isNull();
  }

  @Test
  void shouldReturnInitialIdsForWrongReferenceIdsWhenMappingToItem() {
    var itemFormat = ItemFormat.builder()
      .id("f042b881-c245-44c3-abdc-0094113793b4")
      .itemLevelCallNumberType("4e4c7814-a3e8-45ca-9482-4b95b5b98f9c")
      .itemDamagedStatus("a6236035-b88f-4d0c-a1ca-48af9bdde3c8")
      .notes("43286219-6c16-4884-b967-65b419e48f8d;note;false")
      .statisticalCodes("bdc3d37d-6cd9-4429-8fb8-e40c9c239278")
      .lastCheckIn("bf3dd85b-af88-4adf-88d0-51d7ec7bf656;af50a162-42a8-4ca2-8768-8c53630cc4ae;2022-12-02T06:52:19.743Z")
      .build();

    var expectedItem = new Item()
      .id("f042b881-c245-44c3-abdc-0094113793b4")
      .itemLevelCallNumberTypeId("4e4c7814-a3e8-45ca-9482-4b95b5b98f9c")
      .itemDamagedStatusId("a6236035-b88f-4d0c-a1ca-48af9bdde3c8")
      .notes(Collections.singletonList(new ItemNote()
        .itemNoteTypeId("43286219-6c16-4884-b967-65b419e48f8d")
        .note("note")
        .staffOnly(false)))
      .statisticalCodeIds(Collections.singletonList("bdc3d37d-6cd9-4429-8fb8-e40c9c239278"))
      .lastCheckIn(new LastCheckIn()
        .servicePointId("bf3dd85b-af88-4adf-88d0-51d7ec7bf656")
        .staffMemberId("af50a162-42a8-4ca2-8768-8c53630cc4ae")
        .dateTime("2022-12-02T06:52:19.743Z"));

    var actualItem = bulkEditParseService.mapItemFormatToItem(itemFormat);

    assertEquals(expectedItem.getItemLevelCallNumberTypeId(), actualItem.getItemLevelCallNumberTypeId());
    assertEquals(expectedItem.getItemDamagedStatusId(), actualItem.getItemDamagedStatusId());
    assertEquals(expectedItem.getNotes().get(0).getItemNoteTypeId(), actualItem.getNotes().get(0).getItemNoteTypeId());
    assertEquals(expectedItem.getStatisticalCodeIds(), actualItem.getStatisticalCodeIds());
    assertEquals(expectedItem.getLastCheckIn().getServicePointId(), actualItem.getLastCheckIn().getServicePointId());
    assertEquals(expectedItem.getLastCheckIn().getStaffMemberId(), actualItem.getLastCheckIn().getStaffMemberId());
  }

  @Test
  void shouldReturnInitialIdsForWrongReferenceIdsWhenMappingToUser() {
    var userFormat = UserFormat.builder()
      .active("true")
      .patronGroup("staff")
      .addresses(";;;;;;;false;db541cda-fcc7-403b-8077-3613f3244901")
      .preferredContactTypeId("002")
      .departments("103aee0f-c5f6-44de-94aa-74093f0e45d9")
      .build();

    var expectedUser = new User()
      .personal(new Personal().addresses(Collections.singletonList(new Address().addressTypeId("db541cda-fcc7-403b-8077-3613f3244901"))))
      .patronGroup("3684a786-6671-4268-8ed0-9db82ebca60b")
      .departments(Set.of(UUID.fromString("103aee0f-c5f6-44de-94aa-74093f0e45d9")));

    var actualUser = bulkEditParseService.mapUserFormatToUser(userFormat);

    assertEquals(expectedUser.getPersonal().getAddresses().get(0).getAddressTypeId(), actualUser.getPersonal().getAddresses().get(0).getAddressTypeId());
    assertEquals(expectedUser.getDepartments(), actualUser.getDepartments());
  }
}
