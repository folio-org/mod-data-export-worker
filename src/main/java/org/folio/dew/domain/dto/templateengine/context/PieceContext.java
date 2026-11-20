package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

/**
 * Vendor-facing piece data of a claimed piece. Internal fields ({@code internalNote}, {@code comment})
 * are deliberately not part of it.
 */
@Data
@Builder
public class PieceContext {
  private String title;
  private String displaySummary;
  private String enumeration;
  private String chronology;
  private String format;
  private Boolean supplement;
  private String receiptDate;       // expected receipt date, yyyy-MM-dd
  private String externalNote;
}
