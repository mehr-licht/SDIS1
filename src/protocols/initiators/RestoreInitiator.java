package protocols.initiators;

import static filesystem.SystemManager.fileMerge;
import static protocols.Macros.*;

import channels.Channel;
import filesystem.ChunkData;
import filesystem.FileInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import network.Message;
import protocols.initiators.helpers.TCPServer;
import service.Peer;

public class RestoreInitiator implements Runnable {

  private FileInfo fileInfo;
  private String filePath;
  private String version;

  private Peer parentPeer;
  private TCPServer tcpServer;

  public RestoreInitiator(String version, String filePath, Peer parentPeer) {
    this.version = version;
    this.filePath = filePath;
    this.parentPeer = parentPeer;
    fileInfo = parentPeer.getDatabase().getFileInfoByPath(filePath);

    utilitarios.Notificacoes_Terminal.printAviso("Starting restoreInitiator!");
  }

  @Override
  public void run() {
    if (fileInfo == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError("File not found for RESTORE");
      return;
    }

    // Activate restore flag
    parentPeer.setRestoring(true, fileInfo.getFileID());

    //Start TCPServer if enhancement
   if (isPeerCompatibleWithEnhancement(ENHANCEMENT_RESTORE, parentPeer)) {
      initializeTCPServer();
    }
    getChunk();

    while (!parentPeer.hasRestoreFinished(filePath, fileInfo.getFileID())) {
      Thread.yield();
    }

   if (isPeerCompatibleWithEnhancement(ENHANCEMENT_RESTORE, parentPeer)) {
      closeTCPServer();
    }

    utilitarios.Notificacoes_Terminal.printAviso("Received all chunks");
    ConcurrentMap<Integer, ChunkData> chunksRestored = parentPeer.getPeerData()
        .getChunksRestored(fileInfo.getFileID());
    String pathToSave = parentPeer.getPath("restored");

    getSystemMgr(chunksRestored, pathToSave);

    // File no longer restoring
    parentPeer.setRestoring(false, fileInfo.getFileID());
    utilitarios.Notificacoes_Terminal.printAviso("Finished restoreInitiator!");
  }

  private void getSystemMgr(ConcurrentMap<Integer, ChunkData> chunksRestored, String pathToSave) {
    try {
      parentPeer.getSystemManager().saveFile(
          fileInfo.getFileName(),
          pathToSave,
          fileMerge(new ArrayList<>(chunksRestored.values()))
      );
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Failed saving file at " + fileInfo.getPath());
    }
  }

  private void getChunk() {
    // Send GETCHUNK to MC
    for (int i = 0; i < fileInfo.getNumChunks(); i++) {
      if (isPeerCompatibleWithEnhancement(ENHANCEMENT_RESTORE, parentPeer)) {
        sendMessageToMC(Message.MessageType.ENH_GETCHUNK, i);
      } else {
        sendMessageToMC(Message.MessageType.GETCHUNK, i);
      }
    }
  }

  private void closeTCPServer() {
    tcpServer.closeTCPServer();
  }

  private void initializeTCPServer() {
    tcpServer = new TCPServer(parentPeer);
    new Thread(tcpServer).start();
  }


  private void sendMessageToMC(Message.MessageType type, int chunkNo) {
    String[] args = {
        version,
        Integer.toString(parentPeer.getID()),
        fileInfo.getFileID(),
        "chk"+chunkNo,
        Integer.toString(parentPeer.getID() + TCPSERVER_PORT)
    };

    Message msg = new Message(type, args);

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
