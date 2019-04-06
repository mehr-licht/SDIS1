package protocols;

import static protocols.Macros.isCompatibleWithEnhancement;
import static protocols.Macros.ENHANCEMENT_RESTORE;

import java.io.IOException;
import java.io.ObjectOutputStream;
import filesystem.Database;
import java.net.Socket;
import channels.Channel;
import network.Message;
import service.Peer;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Restore implements Runnable, PeerData.MessageObserver {
  private Peer parentPeer;
  private Message request;
  private Database database;
  private Random random;
  private Future handler = null;

  public Restore(Peer parentPeer, Message request) {
    this.parentPeer = parentPeer;
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

    String fileID = request.getFileID();
    int chunkNo = request.getChunkNo();

    if (!chunk_found(chunkNo, fileID)) {
      return;
    }

    byte[] chunkData = parentPeer.load_chunk(fileID, chunkNo);

    send_message_by_channels(chunkData);

    utilitarios.Notificacoes_Terminal.printAviso("RESTORE terminado");
  }

  /**
   * Envia o datagrama
   *
   * @param chunkData dados do chunk
   */
  private void send_message_by_channels(byte[] chunkData) {
    if (isCompatibleWithEnhancement(ENHANCEMENT_RESTORE, request, parentPeer)) {
      send_message_by_TCP(request, chunkData);
      send_message_by_MDR(request, null);
    } else {
      send_message_by_MDR(request, chunkData);
    }
  }

  /**
   * Verifica se o chunk existe
   *
   * @param chunkNo numero do chunk
   * @param fileID identificação do ficheiro
   * @return verdadeiro ou falso
   */
  private boolean chunk_found(int chunkNo, String fileID) {
    //Access database to get the ChunkData
    if (!database.hasChunk(fileID, chunkNo)) {
      utilitarios.Notificacoes_Terminal.printMensagemError("chunk " + chunkNo + " do ficheiro " + fileID +" não encontrado");
      return false;
    }
    return true;
  }

  /**
   * Verifica se pedido é do próprio.
   * @return verdadeiro ou falso
   */
  private boolean is_owned() {
    if (request.getSenderID() == parentPeer.get_ID()) {
      return true;
    }
    return false;
  }

  /**
   * Cria datagrama
   *
   * @param request pedido de serviço
   * @param chunkData dados do datagrama
   * @return datagrama criado
   */
  private Message create_message(Message request, byte[] chunkData) {
    String[] args = {
        parentPeer.get_version(),
        Integer.toString(parentPeer.get_ID()),
        request.getFileID(),
        Integer.toString(request.getChunkNo())
    };

    return new Message(Message.MessageType.CHUNK, args, chunkData);
  }

  /**
   * Envia datagrama por TCP
   *
   * @param request serviço pedido
   * @param chunkData dados a enviar
   */
  private void send_message_by_TCP(Message request, byte[] chunkData) {
    Message msgToSend = create_message(request, chunkData);

    String hostName = request.getTCPHost();
    int portNumber = request.getTCPPort();

    create_socket(hostName, portNumber, msgToSend);

    utilitarios.Notificacoes_Terminal.printAviso("Emissor (TCP): " + request.toString());
  }

  /**
   * Cria o socket TCP
   *
   * @param hostName
   * @param portNumber
   * @param msgToSend
   */
  private void create_socket(String hostName, int portNumber, Message msgToSend) {
    try {
      Socket serverSocket;
      serverSocket = new Socket(hostName, portNumber);
      tcp_socket_send(hostName, portNumber, msgToSend, serverSocket);
      serverSocket.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar chunk por TCP");
    }
  }

  /**
   * Envia o datagrama pelo socket TCP
   *
   * @param hostName nome do host
   * @param portNumber porto do socket a criar
   * @param msgToSend datagrama a enviar
   * @throws IOException Exceção In/Out a ser lançado se acontecer um erro
   */
  private void tcp_socket_send(String hostName, int portNumber, Message msgToSend, Socket serverSocket)
      throws IOException {
    utilitarios.Notificacoes_Terminal.printNotificao("Ligado ao servidor TCP");
    ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
    oos.writeObject(msgToSend);
    oos.close();
  }

  /**
   * Envia datagrama pelo canal MDR
   *
   * @param request serviço pedido
   * @param chunkData dados a enviar
   */
  private void send_message_by_MDR(Message request, byte[] chunkData) {
    Message msgToSend = create_message(request, chunkData);

    parentPeer.get_peer_data().attachChunkObserver(this);
    this.handler = parentPeer.send_delayed_message(
        msgToSend, Channel.ChannelType.MDR,
        random.nextInt(Macros.MAX_DELAY),
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
      parentPeer.get_peer_data().detachChunkObserver(this);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Actualização do estado da transmissão e cancelamento se necessário
   * @param msg datagrama
   */
  @Override
  public void update(Message msg) {
    if (this.handler == null) {
      return;
    }
    if (msg.getFileID().equals(request.getFileID()) && msg.getChunkNo() == request.getChunkNo()) {
      this.handler.cancel(true);
      utilitarios.Notificacoes_Terminal.printNotificao("datagrama cancelado para não assoberbar o host");
    }
  }
}
