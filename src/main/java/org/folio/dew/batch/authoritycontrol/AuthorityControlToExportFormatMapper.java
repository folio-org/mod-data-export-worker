package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.Metadata;
import org.folio.dew.domain.dto.authoritycontrol.AuthorityControlExportFormat;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

@Component
public class AuthorityControlToExportFormatMapper {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

  public AuthorityControlExportFormat convertToExportFormat(AuthorityDataStatDto dto) {
    var exportFormat = new AuthorityControlExportFormat();
    var metadata = dto.getMetadata();

    exportFormat.setLastUpdated(dateToString(metadata.getCompletedAt()));
    exportFormat.setUpdater(convertUserName(metadata));
    exportFormat.setOriginalHeading(dto.getHeadingOld());
    exportFormat.setNewHeading(dto.getHeadingNew());
    exportFormat.setOriginal1XX(dto.getHeadingTypeOld());
    exportFormat.setNew1XXX(dto.getHeadingTypeNew());
    exportFormat.setNumberOfBibliographicRecordsLinked(dto.getLbTotal().toString());

/*
TODO: Here should be integration with mod-inventory-storage to receive source file data by id
    exportFormat.setIdentifier("");
    exportFormat.setAuthoritySourceFileName("");
*/

    return exportFormat;
  }

  private String convertUserName(Metadata metadata) {
    return metadata.getStartedByUserLastName() + ", " + metadata.getStartedByUserFirstName();
  }

  private String dateToString(Date date) {
    return nonNull(date) ? OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).format(DATE_FORMAT) : EMPTY;
  }
}
