package protocols.initiators.helpers;

import canais.Canal;
import java.io.IOException;
import java.util.Set;
import network.Message;
import service.Peer;
import filesystem.Database;

/**
 * classe DeleteEnhHelper
 */
public class DeleteEnhHelper implements Runnable {

  private final Message request;
  private Peer parent_peer;

  /**
   * construtor de DeleteEnhHelper
   *
   * @param request
   * @param parent_peer
   */
  public DeleteEnhHelper(Message request, Peer parent_peer) {
    this.request = request;
    this.parent_peer = parent_peer;

    utilitarios.Notificacoes_Terminal.printNotificao("A começar o DeleteEnhHelper");
  }

  /**
   * lança o DeleteEnhHelper
   */
  @Override
  public void run() {
    Database database = parent_peer.get_database();
    Set<String> files_to_delete = database.getFilesToDelete(request.getSenderID());

    if (files_to_delete.isEmpty()) {
      utilitarios.Notificacoes_Terminal.printNotificao("O peer " + request.getSenderID() + " não tem ficheiros para apagar");
      return;
    }

    for (String file_ID : files_to_delete) {
      send_delete(file_ID);
    }

  }

  /**
   * Envia o datagrama de delete
   *
   * @param file_ID identificador do ficheiro
   */
  private void send_delete(String file_ID) {
    try {
      parent_peer.send_message(generate_message(file_ID), Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  /**
   * Compõe o datagrama de delete
   *
   * @param file_ID identificador do ficheiro
   * @return datagrama de delete
   */
  private Message generate_message(String file_ID) {
    String[] args = {
        parent_peer.get_version(),
        Integer.toString(parent_peer.get_ID()),
        file_ID
    };

    return new Message(Message.MessageType.DELETE, args);
  }
}
