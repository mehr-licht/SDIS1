package network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import service.Peer;
import utils.Log;

public abstract class AbstractMessageDispatcher implements Runnable {

  protected Peer parentPeer;
  private BlockingQueue<Message> msgQueue;
  private Map<Message.MessageType, MessageHandler> messageHandlers;

  AbstractMessageDispatcher(Peer parentPeer) {
    this.parentPeer = parentPeer;

    msgQueue = new LinkedBlockingDeque<>();
    messageHandlers = new HashMap<>();

    setupMessageHandlers();
  }

  // Template Method
  protected abstract void setupMessageHandlers();

  protected void addMessageHandler(Message.MessageType msgType, MessageHandler handler) {
    messageHandlers.put(msgType, handler);
  }

  protected void removeMessageHandler(Message.MessageType msgType) {
    messageHandlers.remove(msgType);
  }

  private void dispatchMessage(Message msg) {
    //Ignoring invalid messages
    if (msg == null || msg.getSenderID() == parentPeer.getID()) {
      return;
    }

    Log.log("R: " + msg.toString());

    MessageHandler handler = messageHandlers.get(msg.getType());
    if (handler != null) {
      handler.handle(msg);
    } else {
      Log.logError("Received unregistered message");
    }
  }

  @Override
  public void run() {
    Message msg;

    while (true) {
      try { // BlockingQueue.take() yields CPU until a message is available
        msg = msgQueue.take();
        dispatchMessage(msg);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void pushMessage(byte[] data, int length) {
    Message msgParsed; // create and parse the message
    try {
      msgParsed = new Message(data, length);
    } catch (Exception e) {
      Log.logError(e.getMessage());
      return;
    }

    msgQueue.add(msgParsed);
  }

  interface MessageHandler {

    void handle(Message msg);
  }

}
