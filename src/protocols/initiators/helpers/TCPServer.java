package protocols.initiators.helpers;

import static utilitarios.Utils.TCPSERVER_PORT;

import service.Peer;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * classe TCPServer
 */
public class TCPServer implements Runnable {

  private ServerSocket server_socket;
  private Peer parent_peer;
  private boolean run;

  /**
   * construtor do servidor TCP
   *
   * @param parent_peer peer que iniciou o serviço
   */
  public TCPServer(Peer parent_peer) {
    this.parent_peer = parent_peer;
    initialize_TCP_server();
  }

  /**
   * lança o servidor TCP
   */
  @Override
  public void run() {
    while (run) {
      handleTCPClient();
    }
  }

  /**
   * Fecha o servidor TCP
   */
  public void close_TCP_server() {
    try {
      run = false;
      server_socket.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível fecharar o servidor TCP");
    }
  }

  /**
   * Inicializa o servidor TCP
   */
  private void initialize_TCP_server() {
    try {
      server_socket = new ServerSocket(TCPSERVER_PORT + parent_peer.get_ID());
      utilitarios.Notificacoes_Terminal.printAviso("servidor TCP inicializado");
      run = true;
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível iniciar o servidor TCP");
    }

  }

  /**
   * cria thread auxiliar para o cliente TCP
   */
  private void handleTCPClient() {
    try {
      Socket client_socket = server_socket.accept();
      utilitarios.Notificacoes_Terminal.printNotificao("cliente TCP");
      new Thread(new TCPClientHandler(parent_peer, client_socket)).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
