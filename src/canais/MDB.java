package canais;

import service.Peer;

/**
 * Imprime o aviso referente ao canal MDB
 * Canal usado pelo backup apenas
 * */
public class MDB extends Canal {

  public MDB( String endereco_multicast, String porta_multicast, Peer peer) {
    super( endereco_multicast, porta_multicast, peer);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal de backup (MDB) activo");
  }
}
