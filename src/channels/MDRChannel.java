package channels;

import service.Peer;

public class MDRChannel extends Channel {

  public MDRChannel(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Restore channel initialized!");
  }
}
