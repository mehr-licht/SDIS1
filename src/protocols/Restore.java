package protocols;

//import static protocols.Macros.ENHANCEMENT_RESTORE;
//import static protocols.Macros.isCompatibleWithEnhancement;

import channels.Channel;
import filesystem.Database;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import network.Message;
import service.Peer;
import utils.Log;

public class Restore implements Runnable, PeerData.MessageObserver {

  private Peer parentPeer;
  private Message request;
  private Database database;
  private Random random;
  private Future handler = null;

  public Restore(Peer parentPeer, Message request) {
    this.parentPeer = parentPeer;
    this.request = request;
    this.database = parentPeer.getDatabase();
    this.random = new Random();

    Log.logWarning("Starting restore!");
  }

  @Override
  public void run() {
    if (logIgnore()) {
      return;
    }

    String fileID = request.getFileID();
    int chunkNo = request.getChunkNo();
    if (chunkNotFound(fileID, chunkNo)) {
      return;
    }

    byte[] chunkData = parentPeer.loadChunk(fileID, chunkNo);

    compatWenh(chunkData);

    Log.logWarning("Finished restore!");
  }

  private void compatWenh(byte[] chunkData) {
  /*  if (isCompatibleWithEnhancement(ENHANCEMENT_RESTORE, request, parentPeer)) {
      sendMessageToTCP(request, chunkData);
      sendMessageToMDR(request, null);
    } else {*/
      sendMessageToMDR(request, chunkData);
    //}
  }

  private boolean chunkNotFound(String fileID, int chunkNo) {
    //Access database to get the ChunkData
    if (!database.hasChunk(fileID, chunkNo)) {
      Log.logError("ChunkData not found locally: " + fileID + "/" + chunkNo);
      return true;
    }
    return false;
  }

  private boolean logIgnore() {
    //Ignore Chunks of own files
    if (request.getSenderID() == parentPeer.getID()) {
      Log.logWarning("Ignoring CHUNKs of own files");
      return true;
    }
    return false;
  }

  private Message createMessage(Message request, byte[] chunkData) {
    String[] args = {
        parentPeer.getVersion(),
        Integer.toString(parentPeer.getID()),
        request.getFileID(),
        Integer.toString(request.getChunkNo())
    };

    return new Message(Message.MessageType.CHUNK, args, chunkData);
  }

  private void sendMessageToTCP(Message request, byte[] chunkData) {
    Message msgToSend = createMessage(request, chunkData);

    String hostName = request.getTCPHost();
    int portNumber = request.getTCPPort();

    Socket serverSocket;

    server_socket(msgToSend, hostName, portNumber);

    Log.logWarning("S TCP: " + request.toString());
  }

  private void server_socket(Message msgToSend, String hostName, int portNumber) {
    Socket serverSocket;
    try {
      serverSocket = new Socket(hostName, portNumber);
      Log.log("Connected to TCPServer");
      ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
      oos.writeObject(msgToSend);
      oos.close();
      serverSocket.close();
    } catch (IOException e) {
      Log.logError("Couldn't send CHUNK via TCP");
    }
  }

  private void sendMessageToMDR(Message request, byte[] chunkData) {
    Message msgToSend = createMessage(request, chunkData);

    parentPeer.getPeerData().attachChunkObserver(this);
    this.handler = parentPeer.sendDelayedMessage(
        Channel.ChannelType.MDR,
        msgToSend,
        random.nextInt(Macros.MAX_DELAY),
        TimeUnit.MILLISECONDS
    );

    handler_wait();

  }

  private void handler_wait() {
    try {
      this.handler.wait();
      parentPeer.getPeerData().detachChunkObserver(this);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void update(Message msg) {
    if (this.handler == null) {
      return;
    }
    if (msg.getFileID().equals(request.getFileID()) && msg.getChunkNo() == request.getChunkNo()) {
      this.handler.cancel(true);
      Log.log("Cancelled CHUNK message, to avoid flooding host");
    }
  }
}
