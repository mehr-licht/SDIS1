package network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import service.Peer;


/**
 * Vai ser os nossos CTT
 * */
public abstract class AbstractMessageDispatcher implements Runnable {

  protected Peer parentPeer;
  private BlockingQueue<Message> fila_mensagens_recebendo;
  private Map<Message.Categoria_Mensagem, MessageHandler> map_message_handlers;

  AbstractMessageDispatcher(Peer parentPeer) {
    this.parentPeer = parentPeer;

    fila_mensagens_recebendo = new LinkedBlockingDeque<>();
    map_message_handlers = new HashMap<>();

    setupMessageHandlers();
  }

  // Template Method
  protected abstract void setupMessageHandlers();

  protected void addMessageHandler(Message.Categoria_Mensagem msgType, MessageHandler handler) {
    map_message_handlers.put(msgType, handler);
  }

  protected void removeMessageHandler(Message.Categoria_Mensagem msgType) {
    map_message_handlers.remove(msgType);
  }

  /**
   * Descarta as mensagens que foram enviadas para ele próprio
   * */
  private void processamento_msg_inicial(Message message) {
    //Ignoring invalid messages
    if (message == null || message.get_Sender_ID() == parentPeer.get_ID()) {
      return;
    }

    utilitarios.Notificacoes_Terminal.printNotificao("Recetor: " + message.toString());

    MessageHandler messageHandler = map_message_handlers.get(message.getType());
    if (messageHandler != null) {
      messageHandler.handle(message);
    } else {
      utilitarios.Notificacoes_Terminal.printMensagemError("Received unregistered message");
    }
  }

  /**
   * Implements Runnable, logo corre quando lançada em thread
   * Corre preenchendo a BlockingQueue com as mensagens que estão a chegar
   * */
  @Override
  public void run() {
    Message message;

    while (true) {
      try { // BlockingQueue.take() yields CPU until a message is available
        message = fila_mensagens_recebendo.take();
        processamento_msg_inicial(message);
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
      utilitarios.Notificacoes_Terminal.printMensagemError(e.getMessage());
      return;
    }
    fila_mensagens_recebendo.add(msgParsed);

  }

  /**
   * Interface a
   * */
  interface MessageHandler {

    void handle(Message msg);
  }

}
