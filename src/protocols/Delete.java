package protocols;

import static protocols.Macros.ENHANCEMENT_DELETE;
import static protocols.Macros.isCompatibleWithEnhancement;

import channels.Channel;
import filesystem.ChunkInfo;
import filesystem.Database;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import network.Message;
import service.Peer;

public class Delete implements Runnable {

  private Peer parentPeer;
  private Message request;
  private Database database;

  public Delete(Peer parentPeer, Message request) {
    this.parentPeer = parentPeer;
    this.request = request;
    this.database = parentPeer.getDatabase();

    utilitarios.Notificacoes_Terminal.printAviso("Starting delete!");
  }


  @Override
  public void run() {
    String fileID = request.getFileID();

    if (dbHasChunks(fileID)) {
      return;
    }

    Map<Integer, ChunkInfo> chunkMap = database.removeChunksBackedUpByFileID(fileID);
    Collection<ChunkInfo> chunks = chunkMap.values();
    for (ChunkInfo chunk : chunks) {
      parentPeer.getSystemManager().deleteChunk(chunk.getFileID(), chunk.getChunkNo());
    }

    compatWenh();

    utilitarios.Notificacoes_Terminal.printAviso("Finished delete!");
  }

  private void compatWenh() {
    if (isCompatibleWithEnhancement(ENHANCEMENT_DELETE, request, parentPeer)) {
      sendMessageToMC(request);
    }
  }

  private boolean dbHasChunks(String fileID) {
    if (!database.hasChunks(fileID)) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Chunks didn't exist! Aborting Delete!");
      return true;
    }
    return false;
  }

  private void sendMessageToMC(Message request) {
    Message msg = makeDELETED(request);

    try {
      parentPeer.sendMessage(Channel.ChannelType.MC, msg);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }

  private Message makeDELETED(Message request) {
    String[] args = {
        parentPeer.getVersion(),
        Integer.toString(parentPeer.getID()),
        request.getFileID()
    };

    return new Message(Message.MessageType.DELETED, args);
  }

}