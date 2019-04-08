package protocols.initiators.helpers;

import service.Peer;
import protocols.Peer_Info;
import filesystem.ChunkData;
import network.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * classe TCPClientHandler
 */
public class TCPClientHandler implements Runnable {

  private Peer parent_peer;
  private Socket client_socket;

  /**
   * construtor de TCPClientHandler
   *  @param parent_peer
   * @param client_socket
   */
  public TCPClientHandler(Peer parent_peer, Socket client_socket) {
    this.parent_peer = parent_peer;
    this.client_socket = client_socket;
  }

  /**
   * lança o handler de cliente TCP
   */
  @Override
  public void run() {

    Message msg = read_from_TCP_socket();

    utilitarios.Notificacoes_Terminal.printAviso("Recetor TCP: " + msg.toString());

    if (msg == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError("TCP: Recebido um chunk inválido. A abortar");
      return;
    }

    Peer_Info peer_data = get_peer_info(msg);
    if (peer_data == null) {
      return;
    }

    peer_data.get_restored_chunk_data(new ChunkData(msg.get_file_ID(), msg.get_Chunk_Numero(), msg.get_Corpo_Mensagem()));
  }

  /**
   * Recebe o CHUNK do socket TCP
   *
   * @return
   */
  private Message read_from_TCP_socket() {
    Message msg = null;
    try {
      ObjectInputStream obj_in_stream = new ObjectInputStream(client_socket.getInputStream());
      msg = (Message) obj_in_stream.readObject();
      obj_in_stream.close();
      client_socket.close();
    } catch (IOException | ClassNotFoundException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("TCP: Não foi possível obter dados do servidor");
    }
    return msg;
  }

  /**
   * Acede aos dados peer
   *
   * @param msg chunk recebido
   * @return dados do peer
   */
  private Peer_Info get_peer_info(Message msg) {

    Peer_Info peer_data = parent_peer.get_peer_data();

    if (!peer_data.get_restored_flag(msg.get_file_ID())) {
      utilitarios.Notificacoes_Terminal.printNotificao("Os dados do Chunk foram eliminados");
      return null;
    }
    return peer_data;
  }

}
