package protocols.initiators;

import channels.Channel;
import filesystem.ChunkInfo;
import filesystem.MemoryManager;
import filesystem.SystemManager;
import java.io.IOException;
import network.Message;
import service.Peer;

public class ReclaimInitiator implements Runnable {

  private Peer parentPeer;
  private SystemManager systemManager;
  private String version;

  public ReclaimInitiator(String version, Peer parentPeer) {
    this.parentPeer = parentPeer;
    this.systemManager = parentPeer.getSystemManager();
    this.version = version;

    utilitarios.Notificacoes_Terminal.printNotificao("Starting reclaimInitiator!");
  }

  @Override
  public void run() {
    MemoryManager memoryManager = systemManager.getMemoryManager();
    whileMemory(memoryManager);

    utilitarios.Notificacoes_Terminal.printNotificao("Available memory: " + memoryManager.getAvailableMemory());
    utilitarios.Notificacoes_Terminal.printNotificao("Finished reclaimInitiator!");
  }

  private void whileMemory(MemoryManager memoryManager) {
    while (memoryManager.getAvailableMemory() < 0) {
      utilitarios.Notificacoes_Terminal.printNotificao("Available memory: " + memoryManager.getAvailableMemory());
      ChunkInfo chunkInfo = systemManager.getDatabase().getChunkForRemoval();

      byte[] chunkData = systemManager.loadChunk(chunkInfo.getFileID(), chunkInfo.getChunkNo());
      if (chunkData == null) { // Confirm chunk exists
        utilitarios.Notificacoes_Terminal.printAviso("ChunkData selected for reclaim doesn't exist");
        continue;
      }

      systemManager.deleteChunk(chunkInfo.getFileID(), chunkInfo.getChunkNo());
      sendREMOVED(chunkInfo);
    }
  }

  private void sendREMOVED(ChunkInfo chunkInfo) {
    sendREMOVED(chunkInfo.getFileID(), chunkInfo.getChunkNo());
  }

  private void sendREMOVED(String fileID, int chunkNo) {
    String args[] = {
        version,
        Integer.toString(parentPeer.getID()),
        fileID,
        Integer.toString(chunkNo)
    };

    Message msg = new Message(Message.MessageType.REMOVED, args);
    sendMsg(msg);
  }

  private void sendMsg(Message msg) {
    try {
      parentPeer.sendMessage(Channel.ChannelType.MC, msg);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }

}
