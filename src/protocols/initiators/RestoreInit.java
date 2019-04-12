package protocols.initiators;

import static filesystem.SystemManager.fileMerge;
import static utilitarios.Utils.RESTOREENH;
import static utilitarios.Utils.TCPSERVER_PORT;
import static utilitarios.Utils.enhancement_compatible_peer;


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
    this.version = version;
    this.file_path = file_path;
    this.parent_peer = parent_peer;
    file_info = parent_peer.get_database().getFileInfoByPath(file_path);
    utilitarios.Notificacoes_Terminal.printAviso("A começar o restore na fonte");
  }

  /** Lançamento do stateInit */
  @Override
  public void run() {
    if (file_info == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Ficheiro para restaurar não encontrado");
      return;
    }
    parent_peer.set_restoring(true, file_info.getFileID());
    if (enhancement_compatible_peer(parent_peer, RESTOREENH)) {
      initialize_TCP_server();
    }
    //send_getchunk();
    send_getchunk();
    while (!parent_peer.has_restore_finished(file_path, file_info.getFileID())) {
      Thread.yield();
    }
    if (enhancement_compatible_peer(parent_peer, RESTOREENH)) {
      close_TCP_server();
    }
    utilitarios.Notificacoes_Terminal.printAviso("Todos os chunks recebidos");
    ConcurrentMap<Integer, ChunkData> chunksRestored =
        parent_peer.get_peer_data().get_restored_chunk_id(file_info.getFileID());
    String pathToSave = parent_peer.get_path("restored");
    save_restores(pathToSave, chunksRestored);
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
  private void send_getchunk() {
    for (int i = 0; i < file_info.getNumChunks(); i++) {
      if (enhancement_compatible_peer( parent_peer, RESTOREENH)) {
        send_message(Message.Categoria_Mensagem.ENH_GETCHUNK, i);
      } else {
        send_message(Message.Categoria_Mensagem.GETCHUNK, i);
      }
    }
  }


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
    String[] args = {
      version,
      Integer.toString(parent_peer.get_ID()),
      file_info.getFileID(),
      "chk" + chunk_No,
      Integer.toString(parent_peer.get_ID() + TCPSERVER_PORT)
    };
    Message msg = new Message(type, args);
    send_msg_MC(msg);
  }

  /**
   * Envia datagrama para o canal multicast
   *
   * @param msg datagrama
   */
  private void send_msg_MC(Message msg) {
    try {
      parent_peer.send_message(msg, Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Não foi possível enviar para o canal multicast");
    }
  }
}
