package canais;

import service.Peer;

/**
 * Imprime o aviso referente ao canal MDR
 * Canal usado pelo protocolo restore apenas
 * */
public class MDR extends Canal {

  public MDR(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal MDR - restore activo");
  }
}
