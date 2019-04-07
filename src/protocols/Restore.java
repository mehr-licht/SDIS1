package protocols;

import static utilitarios.Utils.enhancements_compatible;
import static utilitarios.Utils.RESTORE_ENH;

import java.io.IOException;
import java.io.ObjectOutputStream;
import filesystem.Database;
import java.net.Socket;
import canais.Canal;
import network.Message;
import service.Peer;
import utilitarios.Utils;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Restore implements Runnable, Peer_Info.MessageObserver {
  private Peer parent_peer;
  private Message request;
  private Database database;
  private Random random;
  private Future handler = null;

  /**
   * classe Restore
   */
  public Restore(Peer parentPeer, Message request) {
    this.parent_peer = parentPeer;
    this.request = request;
    this.database = parentPeer.get_database();
    this.random = new Random();

    utilitarios.Notificacoes_Terminal.printAviso("Início do RESTORE");
  }

  /**
   * Lançamento do restore
   */
  @Override
  public void run() {
    if (is_owned()) {
      utilitarios.Notificacoes_Terminal.printAviso("Não se liga a chunks de ficheiros nossos");
      return;
    }

    String file_ID = request.get_file_ID();
    int chunk_No = request.get_Chunk_Numero();

    if (!chunk_found(chunk_No, file_ID)) {
      return;
    }

    byte[] chunk_data = parent_peer.load_chunk(file_ID, chunk_No);

    send_message_by_channels(chunk_data);

    utilitarios.Notificacoes_Terminal.printAviso("RESTORE terminado");
  }

  /**
   * Envia o datagrama
   *
   * @param chunkData dados do chunk
   */
  private void send_message_by_channels(byte[] chunkData) {
    if (enhancements_compatible(parent_peer, request, RESTORE_ENH)) {
      send_message_by_TCP(request, chunkData);
      send_message_by_MDR(request, null);
    } else {
      send_message_by_MDR(request, chunkData);
    }
  }

  /**
   * Verifica se o chunk existe
   *
   * @param chunk_No numero do chunk
   * @param file_ID identificação do ficheiro
   * @return verdadeiro ou falso
   */
  private boolean chunk_found(int chunk_No, String file_ID) {

    if (!database.hasChunk(file_ID, chunk_No)) {
      utilitarios.Notificacoes_Terminal.printMensagemError("chunk " + chunk_No
          + " do ficheiro " + file_ID +" não encontrado");
      return false;
    }
    return true;
  }

  /**
   * Verifica se pedido é do próprio.
   * @return verdadeiro ou falso
   */
  private boolean is_owned() {
    if (request.get_Sender_ID() == parent_peer.get_ID()) {
      return true;
    }
    return false;
  }

  /**
   * Cria datagrama
   *
   * @param request pedido de serviço
   * @param chunk_data dados do datagrama
   * @return datagrama criado
   */
  private Message create_message(Message request, byte[] chunk_data) {
    String[] args = {
        parent_peer.get_version(),
        Integer.toString(parent_peer.get_ID()),
        request.get_file_ID(),
        Integer.toString(request.get_Chunk_Numero())
    };

    return new Message(Message.Categoria_Mensagem.CHUNK, args, chunk_data);
  }

  /**
   * Envia datagrama por TCP
   *  @param request serviço pedido
   * @param chunk_data dados a enviar
   */
  private void send_message_by_TCP(Message request, byte[] chunk_data) {
    Message msg_to_send = create_message(request, chunk_data);

    String host_name = request.get_TCP_hostname();
    int port_number = request.get_TCP_porta();

    create_socket(host_name, port_number, msg_to_send);

    utilitarios.Notificacoes_Terminal.printAviso("Emissor (TCP): " + request.toString());
  }

  /**
   * Cria o socket TCP
   *  @param host_name
   * @param port_number
   * @param msg_to_send
   */
  private void create_socket(String host_name, int port_number, Message msg_to_send) {
    try {
      Socket server_socket;
      server_socket = new Socket(host_name, port_number);
      tcp_socket_send(msg_to_send, server_socket);
      server_socket.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar chunk por TCP");
    }
  }

  /**
   * Envia o datagrama pelo socket TCP
   *
   * @param msg_to_send datagrama a enviar
   * @param server_socket
   * @throws IOException Exceção In/Out a ser lançado se acontecer um erro
   */
  private void tcp_socket_send(Message msg_to_send, Socket server_socket) throws IOException {
    utilitarios.Notificacoes_Terminal.printNotificao("Ligado ao servidor TCP");
    ObjectOutputStream obj_out_stream = new ObjectOutputStream(server_socket.getOutputStream());
    obj_out_stream.writeObject(msg_to_send);
    obj_out_stream.close();
  }

  /**
   * Envia datagrama pelo canal MDR
   *  @param request serviço pedido
   * @param chunk_data dados a enviar
   */
  private void send_message_by_MDR(Message request, byte[] chunk_data) {
    Message msg_to_send = create_message(request, chunk_data);

    parent_peer.get_peer_data().add_chunk_observer(this);
    this.handler = parent_peer.send_delayed_message(
        msg_to_send, Canal.ChannelType.MDR,
        random.nextInt(Utils.MAX_DELAY),
        TimeUnit.MILLISECONDS
    );

    remove_chunk_observer();
  }

  /**
   * Remove da lista de observadores do chunk após espera
   */
  private void remove_chunk_observer() {
    try {
      this.handler.wait();
      parent_peer.get_peer_data().remove_chunk_observer(this);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Actualização do estado da transmissão e cancelamento se necessário
   *
   * @param msg datagrama
   */
  @Override
  public void update(Message msg) {
    if (this.handler == null) {
      return;
    }
    if (msg.get_file_ID().equals(request.get_file_ID()) && msg.get_Chunk_Numero() == request.get_Chunk_Numero()) {
      this.handler.cancel(true);
      utilitarios.Notificacoes_Terminal.printNotificao("datagrama cancelado para não assoberbar o host");
    }
  }
}
