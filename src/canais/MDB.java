package canais;

import service.Peer;

/**
 * Imprime o aviso referente ao canal MDB
 * Canal usado pelo backup apenas
 * */
public class MDB extends Canal {

  public MDB(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal de backup (MDB) activo");
  }
}
