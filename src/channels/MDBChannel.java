package channels;

import service.Peer;

public class MDBChannel extends Channel {

  public MDBChannel(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Backup channel initialized!");
  }
}
