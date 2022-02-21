package org.folio.dew.exceptions;

import java.io.IOException;

public class FtpException extends IOException {

  private static final long serialVersionUID = 1951421211166849200L;
  private final String replyMessage;
  private final Integer replyCode;
  private static final String FILE_UPLOAD_FAILED = "File upload failed. ";

  public FtpException(Integer replyCode, String replyMessage) {
    super(replyMessage);
    this.replyMessage = getReplyMessage(replyCode, replyMessage);
    this.replyCode = replyCode;
  }

  public String getReplyMessage() {
    return replyMessage;
  }

  public Integer getReplyCode() {
    return replyCode;
  }

  public String getReplyMessage(Integer replyCode, String replyMessage) {
    switch (replyCode) {
    case 550:
      return FILE_UPLOAD_FAILED + "Please check if user has write permissions";
    default:
      return FILE_UPLOAD_FAILED + replyMessage;
    }
  }
}
