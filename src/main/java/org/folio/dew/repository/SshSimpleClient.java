package org.folio.dew.repository;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;

@Log4j2
@Getter
public class SshSimpleClient {

  private final SshClient sshClient;
  private final String username;
  private final String host;
  private final String password;
  private final int port;

  public SshSimpleClient(String username, String password, String host, int port) {
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;

    sshClient = SshClient.setUpDefaultClient();
    sshClient.addPasswordIdentity(password);
  }

  public ClientSession connect() throws IOException {
    ConnectFuture connectFuture = sshClient.connect(username, host, port).verify();
    log.debug("SSHClient is connected: {}", connectFuture.isConnected());
    return connectFuture.getSession();
  }

  public void startClient() {
    sshClient.start();
    log.debug("SSHClient is started...");
  }

  public void stopClient() {
    sshClient.stop();
    log.debug("SSHClient is stopped...");
  }
}
