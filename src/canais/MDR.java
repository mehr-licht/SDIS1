package canais;

import service.Peer;

/**
 * Imprime o aviso referente ao canal MDR
 * Canal usado pelo protocolo restore apenas
 * */
public class MDR extends Canal {

  public MDR( String endereco_multicast, String porta_multicast, Peer peer) {
    super( endereco_multicast, porta_multicast,peer);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal MDR - restore activo");
  }
}
