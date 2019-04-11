package canais;

import service.Peer;


/**
 * Imprime o aviso referente ao canal MC
 * Usado para o protocolo backup, restore, deletion, reclaiming
 * */
public class MC extends Canal {

  public MC(String endereco_multicast, String porta_multicast, Peer peer) {
    super( endereco_multicast, porta_multicast, peer);
    utilitarios.Notificacoes_Terminal.printNotificao("Canal de controlo (MC) activo");
  }
}
