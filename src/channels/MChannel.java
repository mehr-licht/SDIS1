package channels;

import service.Peer;

public class MChannel extends Channel {

  public MChannel(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Control channel initialized!");
  }
}
