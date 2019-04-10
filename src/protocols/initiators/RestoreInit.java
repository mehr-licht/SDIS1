package protocols.initiators;

import static filesystem.SystemManager.fileMerge;
import static utilitarios.Utils.RESTORE_ENH;
import static utilitarios.Utils.TCPSERVER_PORT;
import static utilitarios.Utils.enhancement_compatible_peer;
import static utilitarios.Utils.isPeerCompatibleWithEnhancement;

import canais.Canal;
import filesystem.ChunkData;
import filesystem.FileInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import network.Message;
import network.Message.Categoria_Mensagem;
import protocols.initiators.helpers.TCPServer;
import service.Peer;


/** classe RestoreInit */
public class RestoreInit implements Runnable {

  private FileInfo file_info;
  private String file_path;
  private String version;

  private Peer parent_peer;
  private TCPServer tcp_server;

  /**
   * construtor do RestoreInit
   *
   * @param version
   * @param file_path
   * @param parent_peer
   */
  public RestoreInit(String version, String file_path, Peer parent_peer) {
System.out.println("construtor do restoreInit 00");
    this.version = version;
    this.file_path = file_path;
    this.parent_peer = parent_peer;
    System.out.println("construtor do restoreInit 01");
    file_info = parent_peer.get_database().getFileInfoByPath(file_path);
    System.out.println("construtor do restoreInit 02");
    utilitarios.Notificacoes_Terminal.printAviso("A começar o restore na fonte");
  }

  /** Lançamento do stateInit */
  @Override
  public void run() {
    System.out.println("run do restoreInit 00");
    if (file_info == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Ficheiro para restaurar não encontrado");
      return;
    }
    System.out.println("run do restoreInit 01");
    parent_peer.set_restoring(true, file_info.getFileID());
    System.out.println("run do restoreInit 02");
    if (enhancement_compatible_peer(parent_peer, RESTORE_ENH)) {
      initialize_TCP_server();
    }
    System.out.println("run do restoreInit 03");
    //send_getchunk();
    getChunk();
    System.out.println("run do restoreInit 04");
    while (!parent_peer.has_restore_finished(file_path, file_info.getFileID())) {
      Thread.yield();
    }
    System.out.println("run do restoreInit 05");
    if (enhancement_compatible_peer(parent_peer, RESTORE_ENH)) {
      close_TCP_server();
    }
    System.out.println("run do restoreInit 06");
    utilitarios.Notificacoes_Terminal.printAviso("Todos os chunks recebidos");
    System.out.println("run do restoreInit 07");
    ConcurrentMap<Integer, ChunkData> chunksRestored =
        parent_peer.get_peer_data().get_restored_chunk_id(file_info.getFileID());
    System.out.println("run do restoreInit 08");
    String pathToSave = parent_peer.get_path("restored");
    System.out.println("run do restoreInit 09");
    save_restores(pathToSave, chunksRestored);
    System.out.println("run do restoreInit 10");
    parent_peer.set_restoring(false, file_info.getFileID());
    utilitarios.Notificacoes_Terminal.printAviso("Acabou o restauro na fonte");
  }

  /**
   * Guarda os chunks restaurados
   *
   * @param path_to_save caminho onde guardar
   * @param restored_chunks os chunks a guardar
   */
  private void save_restores(
      String path_to_save, ConcurrentMap<Integer, ChunkData> restored_chunks) {
    try {
      parent_peer
          .get_system_manager()
          .saveFile(
              file_info.getFileName(),
              path_to_save,
              fileMerge(new ArrayList<>(restored_chunks.values())));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Erro ao gravar ficheiro em " + file_info.getPath());
    }
  }

  /**
   * Envia GETCHUNK para o canal multicast
   */
  private void getChunk() {
    System.out.println("getchunk do restore 00");
    // Send GETCHUNK to MC
    for (int i = 0; i < file_info.getNumChunks(); i++) {
      System.out.println("getchunk do restore 01");
      if (isPeerCompatibleWithEnhancement(RESTORE_ENH, parent_peer)) {
        System.out.println("getchunk do restore 02");
        send_message(Message.Categoria_Mensagem.ENH_GETCHUNK, i);
        System.out.println("getchunk do restore 03");
      } else {
        System.out.println("getchunk do restore 04");
        send_message(Message.Categoria_Mensagem.GETCHUNK, i);
        System.out.println("getchunk do restore 05");
      }
      System.out.println("getchunk do restore 06");
    }
    System.out.println("getchunk do restore 07");
  }

  private void sendMessageToMC(Message.Categoria_Mensagem type, int chunkNo) {
    String[] args = {
        version,
        Integer.toString(parent_peer.get_ID()),
        file_info.getFileID(),
        Integer.toString(chunkNo),
        Integer.toString(parent_peer.get_ID() + TCPSERVER_PORT)
    };

    Message msg = new Message(type, args);

    sendMsg(msg);
  }

  private void sendMsg(Message msg) {
    try {
      parent_peer.send_message(msg,Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }
  /*
  private void send_getchunk() {
    System.out.println("get_chunk do restore 00");
    for (int i = 0; i < file_info.getNumChunks(); i++) {
      System.out.println("get_chunk do restore 01");
      if (enhancement_compatible_peer(parent_peer, RESTORE_ENH)) {
        System.out.println("get_chunk do restore 02");
        send_message(Categoria_Mensagem.ENH_GETCHUNK, i);
        System.out.println("get_chunk do restore 03");
      } else {
        System.out.println("get_chunk do restore 04");
        send_message(Categoria_Mensagem.GETCHUNK, i);
        System.out.println("get_chunk do restore 05");
      }
      System.out.println("get_chunk do restore 06");
    }
    System.out.println("get_chunk do restore 07");
  }*/

  /**
   * Encerra o servidor TCP
   */
  private void close_TCP_server() {
    tcp_server.close_TCP_server();
  }

  /**
  * Inicia o servidor TCP
  */
  private void initialize_TCP_server() {
    tcp_server = new TCPServer(parent_peer);
    new Thread(tcp_server).start();
  }

  /**
   * Envia datagrama
   *
   * @param type tipo de mensagem
   * @param chunk_No numero do chunk
   */
  private void send_message(Categoria_Mensagem type, int chunk_No) {
    System.out.println("send_msg do restore 00");
    String[] args = {
      version,
      Integer.toString(parent_peer.get_ID()),
      file_info.getFileID(),
      "chk" + chunk_No,
      Integer.toString(parent_peer.get_ID() + TCPSERVER_PORT)
    };
    System.out.println("send_msg do restore 01");
    Message msg = new Message(type, args);
    System.out.println("send_msg do restore 02");
    send_msg_MC(msg);
    System.out.println("send_msg 03");
  }

  /**
   * Envia datagrama para o canal multicast
   *
   * @param msg datagrama
   */
  private void send_msg_MC(Message msg) {
    System.out.println("send_msg_MC do restore 00");
    try {
      System.out.println("send_msg_MC do restore 01");
      parent_peer.send_message(msg, Canal.ChannelType.MC);
      System.out.println("send_msg_MC do restore 02");
    } catch (IOException e) {
      System.out.println("send_msg_MC do restore 03");
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Não foi possível enviar para o canal multicast");
    }
    System.out.println("send_msg_MC do restore 04");
  }
}
