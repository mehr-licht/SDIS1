package network;

import static network.Message.Categoria_Mensagem.*;
import static utilitarios.Utils.*;

import filesystem.ChunkData;
import filesystem.ChunkInfo;
import filesystem.Database;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import protocols.*;
import utilitarios.Utils;
import protocols.initiators.helpers.DeleteEnhHelper;
import protocols.initiators.helpers.RemovedChunkHelper;
import service.Peer;

public class CaixaCorreio extends CTTpostBox {

  private ScheduledExecutorService executor;

  private Map<String, Map<Integer, Future>> backUpHandlers;

  private Random random;

  /**
   * Inicia as threads da caixa de correio
   * */
  public CaixaCorreio(Peer parentPeer) {
    super(parentPeer);

    this.parent_peer = parentPeer;
    this.executor = Executors.newScheduledThreadPool(MSG_CORE_POOL_SIZE);
    this.backUpHandlers = new HashMap<>();
    this.random = new Random();
  }

  private void handle_GETCHUNK(Message msg) {
    Restore restore_enh = new Restore(parent_peer, msg);
    executor.execute(restore_enh);
  }

  private void handle_DELETE(Message msg) {
    parent_peer.get_database().add_files_to_trash(msg.get_file_ID());

    Delete delete = new Delete(parent_peer, msg);
    executor.execute(delete);
  }

  private void handle_ACTIVE(Message msg) {
    if (enhancements_compatible(parent_peer, msg, DELETEENH)) {
      executor.execute(new DeleteEnhHelper(msg, parent_peer));
    }
  }

  private void handle_HASDELETED(Message msg) {
    Database database = parent_peer.get_database();

    if (enhancements_compatible(parent_peer, msg, DELETEENH)) {
      database.remove_from_history_file_copys_on_peers(msg.get_file_ID(), msg.get_sender_ID());
    }
  }

  private void handle_CHUNK(Message msg) {
    Peer_Info peerData = parent_peer.get_peer_data();

    peerData.notify_chunk_observers(msg);

    if (!peerData.get_restored_flag(msg.get_file_ID())) { // Restoring File
      return;
    }

    if (!enhancement_compatible_msg(msg, RESTOREENH)) {
      peerData.get_restored_chunk_data(new ChunkData(msg.get_file_ID(), msg.get_chunk_numero(), msg.get_Corpo_Mensagem()));
    }
  }

  private void handle_PUTCHUNK(Message msg) {
    Database database = parent_peer.get_database();
    database.remove_file_from_trash(msg.get_file_ID());

    if (database.hasChunk(msg.get_file_ID(), msg.get_chunk_numero())) {
      // If chunk is backed up by parent_peer, notify
      Map<Integer, Future> fileBackUpHandlers = backUpHandlers.get(msg.get_file_ID());
      if (fileBackUpHandlers == null) {
        return;
      }

      final Future handler = fileBackUpHandlers.remove(msg.get_chunk_numero());
      if (handler == null) {
        return;
      }
      handler.cancel(true);
      utilitarios.Notificacoes_Terminal.printNotificao("Stopping chunk back up, due to received PUTCHUNK");
    } else if (!database.has_in_history_files_BackedUp_By_File_Id(msg.get_file_ID())) {
      // If file is not a local file, Mirror/Backup ChunkData
      Backup backup = new Backup(parent_peer, msg);
      executor.execute(backup);
    } else {
      utilitarios.Notificacoes_Terminal.printNotificao("Ignoring PUTCHUNK of own file");
    }
  }

  private void handle_STORED(Message msg) {

    parent_peer.get_peer_data().notify_stored_observers(msg);

    Database database = parent_peer.get_database();
    if (database.hasChunk(msg.get_file_ID(), msg.get_chunk_numero())) {
      database.add_to_history_chunks_backed_upMirror(msg.get_file_ID(), msg.get_chunk_numero(), msg.get_sender_ID());
    } else if (database.has_in_history_files_BackedUp_By_File_Id(msg.get_file_ID())) {
      parent_peer.get_peer_data().inc_chunk_replic(msg.get_file_ID(), msg.get_chunk_numero());
      database.add_to_history_file_copys_in_peers(msg.get_file_ID(), msg.get_sender_ID());
    }
  }

  private void handle_REMOVED(Message msg) {
    Database database = parent_peer.get_database();
    String fileID = msg.get_file_ID();
    int chunkNo = msg.get_chunk_numero();

    if (database.remove_chunk_mirror(fileID, chunkNo, msg.get_sender_ID()) == null) {
      utilitarios.Notificacoes_Terminal.printNotificao("Ignoring REMOVED of non-local ChunkData");
      return;
    }

    ChunkInfo chunkInfo = database.getChunkInfo(fileID, chunkNo);

    int perceivedReplication = database.getChunkPerceivedReplication(fileID, chunkNo);
    int desiredReplication = chunkInfo.get_replication_degree();

    if (perceivedReplication < desiredReplication) {
      byte[] chunkData = parent_peer.load_chunk(fileID, chunkNo);

      Future handler = executor.schedule(
          new RemovedChunkHelper(parent_peer, chunkInfo, chunkData),
          this.random.nextInt(Utils.MAX_DELAY + 1),
          TimeUnit.MILLISECONDS
      );

      backUpHandlers.putIfAbsent(msg.get_file_ID(), new HashMap<>());
      backUpHandlers.get(msg.get_file_ID()).put(msg.get_chunk_numero(), handler);
    }
  }

  /**
   * Configuração da caixa de correio
   * Adiciona accoes para cada tipo de mensagem recebida
   * */
  @Override
  protected void configuracao_mensagem_handlers() {
    adiciona_handle_mensagem(PUTCHUNK, this::handle_PUTCHUNK);
    adiciona_handle_mensagem(STORED, this::handle_STORED);
    adiciona_handle_mensagem(GETCHUNK, this::handle_GETCHUNK);
    adiciona_handle_mensagem(ENH_GETCHUNK, this::handle_GETCHUNK);
    adiciona_handle_mensagem(CHUNK, this::handle_CHUNK);
    adiciona_handle_mensagem(REMOVED, this::handle_REMOVED);
    adiciona_handle_mensagem(DELETE, this::handle_DELETE);
    adiciona_handle_mensagem(HASDELETED, this::handle_HASDELETED);
    adiciona_handle_mensagem(ACTIVE, this::handle_ACTIVE);
  }

}