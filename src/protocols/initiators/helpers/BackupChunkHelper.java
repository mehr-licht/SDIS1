package protocols.initiators.helpers;

import channels.Channel;
import filesystem.ChunkData;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;
import network.Message;
import utilitarios.Utils;
import protocols.initiators.BackupInit;
import service.Peer;

public class BackupChunkHelper implements Runnable {

  private final String protocolVersion;
  private Peer parentPeer;
  private ChunkData chunk;
  private AtomicIntegerArray chunkReplication;

  public BackupChunkHelper(BackupInit backup_init, ChunkData chunk) {
    this.chunk = chunk;
    this.parentPeer = backup_init.getParentPeer();
    this.protocolVersion = backup_init.getProtocolVersion();
    this.chunkReplication = parentPeer.get_peer_data().get_replic(chunk.getFileID());
  }

  BackupChunkHelper(Peer parentPeer, ChunkData chunk) {
    this.chunk = chunk;
    this.parentPeer = parentPeer;
    this.protocolVersion = parentPeer.get_version();
    this.chunkReplication = null;
  }

  @Override
  public void run() {

    int waitTime = 1000; // wait time, in milliseconds
    Message msg = generatePutChunkMsg(chunk, protocolVersion);

    for (int i = 0; i < Utils.PUTCHUNK_RETRIES; i++) {
      if (isDesiredReplicationDegree()) {
        utilitarios.Notificacoes_Terminal.printNotificao("Achieved desired replication at i=" + i);
        break;
      }

      try {
        parentPeer.send_message(msg, Channel.ChannelType.MDB);
      } catch (IOException e) {
        utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
      }

      sleep(waitTime);
      waitTime *= 2;
    }
  }

  protected boolean isDesiredReplicationDegree() {
    utilitarios.Notificacoes_Terminal.printNotificao("Current perceived replication of " + chunk.getChunkNo() + ": " + chunkReplication
        .get(chunk.getChunkNo()));
    return chunkReplication != null && chunkReplication.get(chunk.getChunkNo()) >= chunk
        .getReplicationDegree();
  }

  private void sleep(int waitTime) {
    try {
      Thread.sleep(waitTime);
    } catch (InterruptedException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError(e.getMessage());
    }
  }

  private Message generatePutChunkMsg(ChunkData chunk, String protocolVersion) {
    String[] args = {
        protocolVersion,
        Integer.toString(parentPeer.get_ID()),
        chunk.getFileID(),
        Integer.toString(chunk.getChunkNo()),
        Integer.toString(chunk.getReplicationDegree())
    };

    return new Message(Message.MessageType.PUTCHUNK, args, chunk.getData());
  }

}
