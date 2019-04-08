package protocols.initiators.helpers;

import service.Peer;
import filesystem.ChunkData;
import filesystem.ChunkInfo;

/**
 * classe RemovedChunkHelper
 */
public class RemovedChunkHelper extends BackupChunkHelper {

  private ChunkInfo chunkInfo;

  /**
   * construtor de RemovedChunkHelper
   * @param parent_peer peer que iniciou o serviço
   * @param chunk_info informação do chunk
   * @param chunk_data data do chunk
   */
  public RemovedChunkHelper(Peer parent_peer, ChunkInfo chunk_info, byte[] chunk_data) {
    super(parent_peer, new ChunkData(chunk_info, chunk_data));

    this.chunkInfo = chunk_info;
  }

  /**
   * Verifica se já atingiu o nivel de replicação desejado
   */
  @Override
  protected boolean achieved_replication_degree() {
    return chunkInfo.getNumMirrors() >= chunkInfo.getReplicationDegree();
  }
}
