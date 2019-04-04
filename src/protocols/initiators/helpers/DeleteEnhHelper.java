package protocols.initiators.helpers;

import channels.Channel;
import filesystem.Database;
import java.io.IOException;
import java.util.Set;
import network.Message;
import service.Peer;

public class DeleteEnhHelper implements Runnable {

  private final Message request;
  private Peer parentPeer;

  public DeleteEnhHelper(Message request, Peer parentPeer) {
    this.request = request;
    this.parentPeer = parentPeer;

    utilitarios.Notificacoes_Terminal.printNotificao("Starting DeleteEnhHelper");
  }

  @Override
  public void run() {
    Database database = parentPeer.getDatabase();
    Set<String> filesToDelete = database.getFilesToDelete(request.getSenderID());

    if (filesToDelete.isEmpty()) {
      utilitarios.Notificacoes_Terminal.printNotificao("No files to delete for peer " + request.getSenderID());
      return;
    }

    for (String fileID : filesToDelete) {
      sendDELETE(fileID);
    }

  }

  private void sendDELETE(String fileID) {
    String[] args = {
        parentPeer.getVersion(),
        Integer.toString(parentPeer.getID()),
        fileID
    };

    Message msg = new Message(Message.MessageType.DELETE, args);

    try {
      parentPeer.sendMessage(Channel.ChannelType.MC, msg);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }
}
