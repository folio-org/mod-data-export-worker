package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDto;
import org.folio.dew.domain.dto.authority.control.Metadata;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.AuthUpdateHeadingExportFormat;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.FailedLinkedBibExportFormat;
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
  private static final String UNKNOWN_USER = "Unknown User";

  public AuthUpdateHeadingExportFormat convertToAuthUpdateHeadingsExportFormat(AuthorityDataStatDto dto) {
    var exportFormat = new AuthUpdateHeadingExportFormat();
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

  public FailedLinkedBibExportFormat convertToFailedLinkedBibExportFormat(InstanceDataStatDto dto) {
    var exportFormat = new FailedLinkedBibExportFormat();

    exportFormat.setFailed(dateToString(dto.getUpdatedAt()));
    exportFormat.setBibliographicTitle(dto.getInstanceTitle());
    exportFormat.setBibliographicUUID(dto.getInstanceId().toString());
    exportFormat.setFailedBibFieldUpdate(dto.getBibRecordTag());
    exportFormat.setLinkedAuthorityIdentifier(dto.getAuthorityNaturalId());
    exportFormat.setReasonForError(dto.getErrorCause());

    return exportFormat;
  }

  private String convertUserName(Metadata metadata) {
    if (isBlank(metadata.getStartedByUserLastName())) {
      return UNKNOWN_USER;
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
