package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIStreamException;

class OrderEdifactExporterTest {
  private final OrderEdifactExporter orderEdifactExporter = new OrderEdifactExporter();

  @Test
  void convertToEdiTest() throws EDIStreamException, FileNotFoundException
  {
    String edi = orderEdifactExporter.convertOrdersToEdi(mockCompPOs());
    assertFalse(edi.isEmpty());

    System.out.println(edi);
//    try (PrintWriter out = new PrintWriter("src/test/resources/edifact/orders.edi")) {
//      out.println(edi);
//    }
  }

  private List<CompositePurchaseOrder> mockCompPOs() {
    CompositePurchaseOrder compPO = new CompositePurchaseOrder();

    compPO.setPoNumber("12345");
    compPO.setDateOrdered(new Date());

    CompositePoLine poLine1 = new CompositePoLine();
    poLine1.setPoLineNumber("12345-1");
    poLine1.setTitleOrPackage("Economics of the public sector");
    poLine1.setPublisher("W W Norton");
    poLine1.setPublicationDate("c2000");

    CompositePoLine poLine2 = new CompositePoLine();
    poLine2.setPoLineNumber("12345-2");

    compPO.setCompositePoLines(List.of(poLine1, poLine2));

    return List.of(compPO);
  }
}
