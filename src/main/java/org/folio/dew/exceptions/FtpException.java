package org.folio.dew.exceptions;

public class FtpException extends Exception {

  private static final long serialVersionUID = 1951421211166849200L;
  private final String replyMessage;
  private final Integer replyCode;

  public FtpException(Integer replyCode, String replyMessage) {
    super(replyMessage);
    this.replyMessage = replyMessage;
    this.replyCode = replyCode;
  }

  public String getReplyMessage() {
    return replyMessage;
  }

  public Integer getReplyCode() {
    return replyCode;
  }
}
