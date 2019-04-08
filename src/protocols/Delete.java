package protocols;

import static utilitarios.Utils.*;

import channels.Channel;
import service.Peer;
import java.io.IOException;
import filesystem.ChunkInfo;
import filesystem.Database;
import network.Message;
import java.util.Collection;
import java.util.Map;

public class Delete implements Runnable {

  private Peer parent_peer;
  private Message request;
  private Database database;




  public Delete(Peer parentPeer, Message request) {
    this.parent_peer = parentPeer;
    this.request = request;
    this.database = parent_peer.get_database();

    utilitarios.Notificacoes_Terminal.printAviso("A começar a apagar");
  }

  /**
   * Lançamento do delete
   */
  @Override
  public void run() {
    String file_ID = request.getFileID();

    if (chunks_in_database(file_ID)) {
      return;
    }

    delete_loop(file_ID);

    enhancement_compatibility();

    utilitarios.Notificacoes_Terminal.printAviso("acabei de apagar");
  }

  /**
   * loop para apagar todos os chunks do ficheiro
   *
   * @param file_ID identicador do ficheiro
   */
  private void delete_loop(String file_ID) {
    Map<Integer, ChunkInfo> chunkMap = database.removeChunksBackedUpByFileID(file_ID);
    Collection<ChunkInfo> chunks = chunkMap.values();
    for (ChunkInfo chunk : chunks) {
      parent_peer.get_system_manager().deleteChunk(chunk.getFileID(), chunk.getChunkNo());
    }
  }

  /**
   * Verifica se os chunks do ficheiro estão mencionados na base de dados
   *
   * @param file_ID identicador do ficheiro
   * @return verdadeiro ou falso
   */
  private boolean chunks_in_database(String file_ID) {
    if (!database.hasChunks(file_ID)) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não há chunks - a abortar");
      return true;
    }
    return false;
  }

  /**
   * Verifica a compatibilidade e envia o datagrama para o canal multicast
   */
  private void enhancement_compatibility() {
    if (enhancements_compatible(parent_peer, request, DELETE_ENH)) {
      send_to_multicast(request);
    }
  }

  /**
   * Envia datagrama do delete para o canal multicast
   *
   * @param request pedido de delete
   */
  private void send_to_multicast(Message request) {
    Message msg = compose_delete_datagram(request);

    try {
      parent_peer.send_message(msg, Channel.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  /**
   * Compõe o datagrama a enviar
   *
   * @param request pedido do delete
   * @return
   */
  private Message compose_delete_datagram(Message request) {
    String[] args = {
        parent_peer.get_version(),
        Integer.toString(parent_peer.get_ID()),
        request.getFileID()
    };

    return new Message(Message.MessageType.DELETED, args);
  }

}