package protocols;

import static filesystem.SystemManager.createFolder;
import static utilitarios.Utils.*;

import channels.Channel;
import filesystem.ChunkInfo;
import filesystem.SystemManager.SAVE_STATE;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import network.Message;
import service.Peer;

public class Backup implements Runnable, Peer_Info.MessageObserver {

  private Peer parentPeer;
  private Message request;

  private Random random;
  private Future handler = null;

  private int storedCount = 0;

  private ScheduledExecutorService scheduledExecutor;

  public Backup(Peer parentPeer, Message request) {
    this.parentPeer = parentPeer;
    this.request = request;

    this.random = new Random();
    this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    utilitarios.Notificacoes_Terminal.printAviso("Starting backup!");
  }


  @Override
  public void run() {
    int senderID = request.getSenderID();
    String fileID = request.getFileID();
    int chunkNo = request.getChunkNo();
    int replicationDegree = request.getReplicationDegree();

    if (senderID == parentPeer.get_ID()) { // a peer never stores the chunks of its own files
      return;
    }

    byte[] chunkData = request.getBody();

    String chunkPath = parentPeer.get_path("backup") + "/" + fileID;
    createFolder(parentPeer.get_path("backup") + "/" + fileID);


    if (enhancements_compatible(parentPeer, request, BACKUP_ENH)) {
      handleEnhancedRequest(fileID, chunkNo, replicationDegree, chunkData, chunkPath);
    } else {
      handleStandardRequest(fileID, chunkNo, replicationDegree, chunkData, chunkPath);
    }

    utilitarios.Notificacoes_Terminal.printAviso("Finished backup!");
  }

  private void handleStandardRequest(String fileID, int chunkNo, int replicationDegree,
      byte[] chunkData, String chunkPath) {
    boolean success = saveChunk(fileID, chunkNo, replicationDegree, chunkData, chunkPath);
    if (success) {
      sendDelayedSTORED(request);
    }
  }

  private void handleEnhancedRequest(String fileID, int chunkNo, int replicationDegree,
      byte[] chunkData, String chunkPath) {
    parentPeer.get_peer_data().add_stored_observer(this);

    this.handler = scheduledExecutor.schedule(
        () -> {
          boolean success = saveChunk(fileID, chunkNo, replicationDegree, chunkData, chunkPath);
          if (success) {
            sendSTORED(request);
          }
        },
        this.random.nextInt(MAX_DELAY + 1),
        TimeUnit.MILLISECONDS
    );

    try {
      this.handler.wait();
      parentPeer.get_peer_data().remove_stored_observer(this);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private boolean saveChunk(String fileID, int chunkNo, int replicationDegree, byte[] chunkData,
      String chunkPath) {
    SAVE_STATE ret;
    try {
      ret = parentPeer.get_system_manager().saveFile(
          "chk"+chunkNo,
          chunkPath,
          chunkData
      );
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't save the chunk!");
      return false;
    }

    if (ret == SAVE_STATE.SUCCESS) {
      parentPeer.get_database().addChunk(
          new ChunkInfo(fileID, chunkNo, replicationDegree, chunkData.length),
          parentPeer.get_ID()
      );
    } else { // Don't send STORED if chunk already existed
      utilitarios.Notificacoes_Terminal.printAviso("ChunkData Backup: " + ret);
      return false;
    }

    return true;
  }

  private void sendSTORED(Message request) {
    Message msg = makeSTORED(request);

    try {
      parentPeer.send_message(msg, Channel.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }

  private void sendDelayedSTORED(Message request) {
    Message msg = makeSTORED(request);

    parentPeer.send_delayed_message(
        msg, Channel.ChannelType.MC,
        random.nextInt(MAX_DELAY + 1),
        TimeUnit.MILLISECONDS
    );
  }

  private Message makeSTORED(Message request) {
    String[] args = {
        parentPeer.get_version(),
        Integer.toString(parentPeer.get_ID()),
        request.getFileID(),
        Integer.toString(request.getChunkNo())
    };

    return new Message(Message.MessageType.STORED, args);
  }

  @Override
  public void update(Message msg) {
    if (this.handler == null) {
      return;
    }
    if (msg.getChunkNo() != request.getChunkNo() || (!msg.getFileID()
        .equals(request.getFileID()))) {
      return;
    }

    storedCount += 1;
    if (storedCount >= request.getReplicationDegree()) {
      // Cancel if chunk's perceived replication fulfills requirements
      this.handler.cancel(true);
    }
  }
}