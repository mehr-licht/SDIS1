package network;

import static network.Message.MessageType.*;
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
    Restore restore_enh = new Restore(parentPeer, msg);
    executor.execute(restore_enh);
  }

  private void handleDELETE(Message msg) {
    parentPeer.get_database().addToFilesToDelete(msg.getFileID());

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
      database.deleteFileMirror(msg.getFileID(), msg.getSenderID());
    }
  }

  private void handleCHUNK(Message msg) {
    Peer_Info peerData = parentPeer.get_peer_data();

    peerData.notify_chunk_observers(msg);

    if (!peerData.get_restored_flag(msg.getFileID())) { // Restoring File
      return;
    }

    if (!enhancement_compatible_msg(msg, RESTORE_ENH)) {
      peerData.get_restored_chunk_data(new ChunkData(msg.getFileID(), msg.getChunkNo(), msg.getBody()));
    }
  }

  private void handlePUTCHUNK(Message msg) {
    Database database = parentPeer.get_database();
    database.removeFromFilesToDelete(msg.getFileID());

    if (database.hasChunk(msg.getFileID(), msg.getChunkNo())) {
      // If chunk is backed up by parentPeer, notify
      Map<Integer, Future> fileBackUpHandlers = backUpHandlers.get(msg.getFileID());
      if (fileBackUpHandlers == null) {
        return;
      }

      final Future handler = fileBackUpHandlers.remove(msg.getChunkNo());
      if (handler == null) {
        return;
      }
      handler.cancel(true);
      utilitarios.Notificacoes_Terminal.printNotificao("Stopping chunk back up, due to received PUTCHUNK");
    } else if (!database.hasBackedUpFileById(msg.getFileID())) {
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
    if (database.hasChunk(msg.getFileID(), msg.getChunkNo())) {
      database.addChunkMirror(msg.getFileID(), msg.getChunkNo(), msg.getSenderID());
    } else if (database.hasBackedUpFileById(msg.getFileID())) {
      parentPeer.get_peer_data().inc_chunk_replic(msg.getFileID(), msg.getChunkNo());
      database.addFileMirror(msg.getFileID(), msg.getSenderID());
    }
  }

  private void handleREMOVED(Message msg) {
    Database database = parentPeer.get_database();
    String fileID = msg.getFileID();
    int chunkNo = msg.getChunkNo();

    if (database.removeChunkMirror(fileID, chunkNo, msg.getSenderID()) == null) {
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

      backUpHandlers.putIfAbsent(msg.getFileID(), new HashMap<>());
      backUpHandlers.get(msg.getFileID()).put(msg.getChunkNo(), handler);
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