package canais;

import service.Peer;


/**
 * Imprime o aviso referente ao canal MC
 * Usado para o protocolo backup, restore, deletion, reclaiming
 * */
public class MC extends Canal {

  public MC(Peer parentPeer, String mcastAddr, String mcastPort) {
    super(parentPeer, mcastAddr, mcastPort);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal de controlo (MC) activo");
  }
}
