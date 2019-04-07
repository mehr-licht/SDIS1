package protocols.initiators.helpers;

import canais.Canal;
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
    Database database = parentPeer.get_database();
    Set<String> filesToDelete = database.getFilesToDelete(request.get_Sender_ID());

    if (filesToDelete.isEmpty()) {
      utilitarios.Notificacoes_Terminal.printNotificao("No files to delete for peer " + request.get_Sender_ID());
      return;
    }

    for (String fileID : filesToDelete) {
      sendDELETE(fileID);
    }

  }

  private void sendDELETE(String fileID) {
    String[] args = {
        parentPeer.get_version(),
        Integer.toString(parentPeer.get_ID()),
        fileID
    };

    Message msg = new Message(Message.Categoria_Mensagem.DELETE, args);

    try {
      parentPeer.send_message(msg, Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }
}
