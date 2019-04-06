package protocols.initiators.helpers;

import static protocols.Macros.TCPSERVER_PORT;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import service.Peer;

public class TCPServer implements Runnable {

  private ServerSocket serverSocket;
  private Peer parentPeer;
  private boolean run;

  public TCPServer(Peer parentPeer) {
    this.parentPeer = parentPeer;
    initializeTCPServer();
  }

  @Override
  public void run() {
    while (run) {
      handleTCPClient();
    }
  }

  public void closeTCPServer() {
    try {
      run = false;
      serverSocket.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't close TCPServer!");
    }
  }

  private void initializeTCPServer() {
    try {
      serverSocket = new ServerSocket(TCPSERVER_PORT + parentPeer.get_ID());
      utilitarios.Notificacoes_Terminal.printAviso("Started TCPServer!");
      run = true;
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't initialize TCPServer!");
    }

  }

  private void handleTCPClient() {
    try {
      Socket clientSocket = serverSocket.accept();
      utilitarios.Notificacoes_Terminal.printNotificao("Received a TCPClient");
      new Thread(new TCPClientHandler(parentPeer, clientSocket)).start();
    } catch (IOException e) {
      //e.printStackTrace();
    }
  }
}
