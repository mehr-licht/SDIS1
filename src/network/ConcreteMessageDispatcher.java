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

public class ConcreteMessageDispatcher extends AbstractMessageDispatcher {

  private ScheduledExecutorService executor;

  private Map<String, Map<Integer, Future>> backUpHandlers;

  private Random random;

  public ConcreteMessageDispatcher(Peer parentPeer) {
    super(parentPeer);

    this.parentPeer = parentPeer;
    this.executor = Executors.newScheduledThreadPool(MSG_CORE_POOL_SIZE);

    this.backUpHandlers = new HashMap<>();
    this.random = new Random();
  }

  private void handleGETCHUNK(Message msg) {
   System.out.println("do restore handleGETCHUNK 00");
    Restore restore_enh = new Restore(parentPeer, msg);
    System.out.println("do restore handleGETCHUNK 01");
    executor.execute(restore_enh);
    System.out.println("do restore handleGETCHUNK 02");
  }

  private void handleDELETE(Message msg) {
    parentPeer.get_database().addToFilesToDelete(msg.get_file_ID());

    Delete delete = new Delete(parentPeer, msg);
    executor.execute(delete);
  }

  private void handleUP(Message msg) {
    if (enhancements_compatible(parentPeer, msg, DELETE_ENH)) {
      executor.execute(new DeleteEnhHelper(msg, parentPeer));
    }
  }

  private void handleDELETED(Message msg) {
    Database database = parentPeer.get_database();

    if (enhancements_compatible(parentPeer, msg, DELETE_ENH)) {
      database.deleteFileMirror(msg.get_file_ID(), msg.get_Sender_ID());
    }
  }

  private void handleCHUNK(Message msg) {
    Peer_Info peerData = parentPeer.get_peer_data();

    peerData.notify_chunk_observers(msg);

    if (!peerData.get_restored_flag(msg.get_file_ID())) { // Restoring File
      return;
    }

    if (!enhancement_compatible_msg(msg, RESTORE_ENH)) {
      peerData.get_restored_chunk_data(new ChunkData(msg.get_file_ID(), msg.get_Chunk_Numero(), msg.get_Corpo_Mensagem()));
    }
  }

  private void handlePUTCHUNK(Message msg) {
    Database database = parentPeer.get_database();
    database.removeFromFilesToDelete(msg.get_file_ID());

    if (database.hasChunk(msg.get_file_ID(), msg.get_Chunk_Numero())) {
      // If chunk is backed up by parentPeer, notify
      Map<Integer, Future> fileBackUpHandlers = backUpHandlers.get(msg.get_file_ID());
      if (fileBackUpHandlers == null) {
        return;
      }

      final Future handler = fileBackUpHandlers.remove(msg.get_Chunk_Numero());
      if (handler == null) {
        return;
      }
      handler.cancel(true);
      utilitarios.Notificacoes_Terminal.printNotificao("Stopping chunk back up, due to received PUTCHUNK");
    } else if (!database.hasBackedUpFileById(msg.get_file_ID())) {
      // If file is not a local file, Mirror/Backup ChunkData
      Backup backup = new Backup(parentPeer, msg);
      executor.execute(backup);
    } else {
      utilitarios.Notificacoes_Terminal.printNotificao("Ignoring PUTCHUNK of own file");
    }
  }

  private void handleSTORED(Message msg) {

    parentPeer.get_peer_data().notify_stored_observers(msg);

    Database database = parentPeer.get_database();
    if (database.hasChunk(msg.get_file_ID(), msg.get_Chunk_Numero())) {
      database.addChunkMirror(msg.get_file_ID(), msg.get_Chunk_Numero(), msg.get_Sender_ID());
    } else if (database.hasBackedUpFileById(msg.get_file_ID())) {
      parentPeer.get_peer_data().inc_chunk_replic(msg.get_file_ID(), msg.get_Chunk_Numero());
      database.addFileMirror(msg.get_file_ID(), msg.get_Sender_ID());
    }
  }

  private void handleREMOVED(Message msg) {
    Database database = parentPeer.get_database();
    String fileID = msg.get_file_ID();
    int chunkNo = msg.get_Chunk_Numero();

    if (database.removeChunkMirror(fileID, chunkNo, msg.get_Sender_ID()) == null) {
      utilitarios.Notificacoes_Terminal.printNotificao("Ignoring REMOVED of non-local ChunkData");
      return;
    }

    ChunkInfo chunkInfo = database.getChunkInfo(fileID, chunkNo);

    int perceivedReplication = database.getChunkPerceivedReplication(fileID, chunkNo);
    int desiredReplication = chunkInfo.getReplicationDegree();

    if (perceivedReplication < desiredReplication) {
      byte[] chunkData = parentPeer.load_chunk(fileID, chunkNo);

      Future handler = executor.schedule(
          new RemovedChunkHelper(parentPeer, chunkInfo, chunkData),
          this.random.nextInt(Utils.MAX_DELAY + 1),
          TimeUnit.MILLISECONDS
      );

      backUpHandlers.putIfAbsent(msg.get_file_ID(), new HashMap<>());
      backUpHandlers.get(msg.get_file_ID()).put(msg.get_Chunk_Numero(), handler);
    }
  }

  @Override
  protected void setupMessageHandlers() {
    addMessageHandler(PUTCHUNK, this::handlePUTCHUNK);
    addMessageHandler(STORED, this::handleSTORED);
    addMessageHandler(GETCHUNK, this::handleGETCHUNK);
    addMessageHandler(ENH_GETCHUNK, this::handleGETCHUNK);
    addMessageHandler(CHUNK, this::handleCHUNK);
    addMessageHandler(REMOVED, this::handleREMOVED);
    addMessageHandler(DELETE, this::handleDELETE);
    addMessageHandler(DELETED, this::handleDELETED);
    addMessageHandler(UP, this::handleUP);
  }

}