package org.folio.dew.batch.eholdings;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class EHoldingsJobConstants {
  //export config constants
  public static final String LOAD_FIELD_PACKAGE_NOTES = "packageNotes";
  public static final String LOAD_FIELD_TITLE_NOTES = "titleNotes";
  public static final String LOAD_FIELD_PACKAGE_AGREEMENTS = "packageAgreements";
  public static final String LOAD_FIELD_TITLE_AGREEMENTS = "titleAgreements";

  //execution context constants
  public static final String CONTEXT_MAX_PACKAGE_NOTES_COUNT = "packageMaxNotesCount";
  public static final String CONTEXT_MAX_TITLE_NOTES_COUNT = "titleMaxNotesCount";
  public static final String CONTEXT_PACKAGE = "package";
  public static final String CONTEXT_RESOURCES = "resources";
}
