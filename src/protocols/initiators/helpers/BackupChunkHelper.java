package protocols.initiators.helpers;

import network.Message;
import utilitarios.Utils;
import protocols.initiators.BackupInit;
import service.Peer;
import channels.Channel;
import filesystem.ChunkData;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * classe BackupChunkHelper
 */
public class BackupChunkHelper implements Runnable {

  private final String protocol_version;
  private Peer parent_peer;
  private ChunkData chunk;
  private AtomicIntegerArray chunk_replic;

  /**
   * construtor de BackupChunkHelper
   *
   * @param backup_init
   * @param chunk
   */
  public BackupChunkHelper(BackupInit backup_init, ChunkData chunk) {
    this.chunk = chunk;
    this.parent_peer = backup_init.get_parent_peer();
    this.protocol_version = backup_init.get_version();
    this.chunk_replic = parent_peer.get_peer_data().get_replic(chunk.getFileID());
  }

  /**
   * construtor de BackupChunkHelper
   *
   * @param parent_peer
   * @param chunk
   */
  BackupChunkHelper(Peer parent_peer, ChunkData chunk) {
    this.chunk = chunk;
    this.parent_peer = parent_peer;
    this.protocol_version = parent_peer.get_version();
    this.chunk_replic = null;
  }

  /**
   * lança o BackupChunkHelper
   */
  @Override
  public void run() {

    int wait_time = 1000; // milisegundos
    Message msg = generate_putchunk(protocol_version, chunk);

    for (int i = 0; i < Utils.PUTCHUNK_RETRIES; i++) {
      if (achieved_replication_degree()) {
        utilitarios.Notificacoes_Terminal.printNotificao("Atingiu o grau de replicação desejado com " + i + " tentativas");
        break;
      }

      send_msg_MDB(msg);

      sleep(wait_time);
      wait_time *= 2;
    }
  }

  /**
   * Envia datagrama para o canal de Backup
   *
   * @param msg datagrama a enviar
   */
  private void send_msg_MDB(Message msg) {
    try {
      parent_peer.send_message(msg, Channel.ChannelType.MDB);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  /**
   * Verifica se o nivel de replicação desejado já foi atingido
   *
   * @return verdadeiro ou falso
   */
  protected boolean achieved_replication_degree() {
    utilitarios.Notificacoes_Terminal.printNotificao("grau de replicação atual do chunk " + chunk.getChunkNo() + ": " + chunk_replic
        .get(chunk.getChunkNo()));
    return chunk_replic != null && chunk_replic.get(chunk.getChunkNo()) >= chunk
        .getReplicationDegree();
  }

  /**
   * Suspende a thread pelo periodo de tempo definido em wait_time
   *
   * @param wait_time periodo de tempo de suspensão da thread
   */
  private void sleep(int wait_time) {
    try {
      Thread.sleep(wait_time);
    } catch (InterruptedException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError(e.getMessage());
    }
  }

  /**
   * Cria o datagrama de putchunk
   *
   * @param version versão do protocolo
   * @param chunk dados do chunk
   * @return datagrama de putchunk
   */
  private Message generate_putchunk(String version, ChunkData chunk) {
    String[] args = {
        version,
        Integer.toString(parent_peer.get_ID()),
        chunk.getFileID(),
        Integer.toString(chunk.getChunkNo()),
        Integer.toString(chunk.getReplicationDegree())
    };

    return new Message(Message.MessageType.PUTCHUNK, args, chunk.getData());
  }

}
