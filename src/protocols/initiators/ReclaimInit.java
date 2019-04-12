package protocols.initiators;

import canais.Canal;
import filesystem.ChunkInfo;
import filesystem.MemoryManager;
import filesystem.SystemManager;
import java.io.IOException;
import network.Message;
import service.Peer;

public class ReclaimInit implements Runnable {

  private Peer parentPeer;
  private SystemManager systemManager;
  private String version;


  /** classe ReclaimInit */
  public ReclaimInit(String version, Peer parentPeer) {
    this.parentPeer = parentPeer;
    this.systemManager = parentPeer.get_system_manager();
    this.version = version;

    utilitarios.Notificacoes_Terminal.printNotificao("A começar a recuperar na fonte");
  }

  /** Lançamento do reclaimInit */
  @Override
  public void run() {
    MemoryManager memory_mgr = systemManager.get_memory_manager();
    remove_chunks(memory_mgr);

    utilitarios.Notificacoes_Terminal.printNotificao("Memória disponível: " + memory_mgr.getAvailableMemory());
    utilitarios.Notificacoes_Terminal.printNotificao("Terminou de recuperar na fonte");
  }

  /**
   * Apaga chunks para recuperar espaço em memória
   *
   * @param mem_mgr gerenciador de memória
   */
  private void remove_chunks(MemoryManager mem_mgr) {
    while (mem_mgr.getAvailableMemory() < 0) {
      utilitarios.Notificacoes_Terminal.printNotificao("Memória disponível: " + mem_mgr.getAvailableMemory());
      ChunkInfo chunk_info = systemManager.get_database().getChunkForRemoval();

      byte[] chunkData = systemManager.load_chunk(chunk_info.get_file_ID(), chunk_info.get_chunk_No());
      if (chunkData == null) {
        utilitarios.Notificacoes_Terminal.printAviso("Não existe a ChunkData seleccionada para recuperar");
        continue;
      }

      systemManager.delete_chunk(chunk_info.get_file_ID(), chunk_info.get_chunk_No());
      send_removed(chunk_info.get_file_ID(), chunk_info.get_chunk_No());
    }
  }

  /**
   * Cria mensagem de chunk apagado
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No número do chunk
   */
  private void send_removed(String file_ID, int chunk_No) {
    String args[] = {
        version,
        Integer.toString(parentPeer.get_ID()),
        file_ID,
        "chk"+chunk_No
    };

    Message msg = new Message(Message.Categoria_Mensagem.REMOVED, args);

    send_msg(msg);
  }

  /**
   * Envia mensagem para o canal multicast
   *
   * @param msg mensagem a enviar
   */
  private void send_msg(Message msg) {
    try {
      parentPeer.send_message(msg, Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviarr para o canal multicast");
    }
  }

}
