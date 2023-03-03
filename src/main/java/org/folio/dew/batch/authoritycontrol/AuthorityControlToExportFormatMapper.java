package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.Metadata;
import org.folio.dew.domain.dto.authoritycontrol.AuthorityUpdateHeadingExportFormat;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

@Component
public class AuthorityControlToExportFormatMapper {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

  public AuthorityUpdateHeadingExportFormat convertToExportFormat(AuthorityDataStatDto dto) {
    var exportFormat = new AuthorityUpdateHeadingExportFormat();
    var metadata = dto.getMetadata();

    exportFormat.setUpdater(convertUserName(metadata));
    exportFormat.setLastUpdated(dateToString(metadata.getStartedAt()));
    exportFormat.setNewHeading(dto.getHeadingNew());
    exportFormat.setOriginalHeading(dto.getHeadingOld());
    exportFormat.setNewHeadingType(dto.getHeadingTypeNew());
    exportFormat.setOriginalHeadingType(dto.getHeadingTypeOld());
    exportFormat.setIdentifier(dto.getNaturalIdNew());
    exportFormat.setAuthoritySourceFileName(dto.getSourceFileNew());
    exportFormat.setNumberOfBibliographicRecordsLinked(dto.getLbTotal().toString());

    return exportFormat;
  }

  private String convertUserName(Metadata metadata) {
    if ((metadata.getStartedByUserFirstName() == null && metadata.getStartedByUserLastName()==null)
        ||(metadata.getStartedByUserFirstName().isBlank() && metadata.getStartedByUserLastName().isBlank())) {
      return "Unknown User";
    }

    if (isBlank(metadata.getStartedByUserFirstName())) {
      return metadata.getStartedByUserLastName();
    }
    return metadata.getStartedByUserLastName() + ", " + metadata.getStartedByUserFirstName();
  }

  private String dateToString(OffsetDateTime date) {
    return nonNull(date) ? date.format(DATE_FORMAT) : EMPTY;
  }
}
