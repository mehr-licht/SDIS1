package filesystem;

public class ChunkData extends BaseChunk {

  private byte[] data;

  /**
   * Construtor 1/3 de ChunkData
   *  @param file_ID identificador do ficheiro
   * @param chunk_No numero do chunk
   * @param data dados do chunk
   */
  public ChunkData(String file_ID, int chunk_No, byte[] data) {
    super(file_ID, chunk_No, null);
    this.data = data;
  }

  /**
   * Construtor 2/3 de ChunkData
   * @param file_ID identificador do ficheiro
   * @param chunk_No numero do chunk
   * @param replication_degree grau de replicação
   * @param data dados do chunk
   */
  public ChunkData(String file_ID, int chunk_No, int replication_degree, byte[] data) {
    this(file_ID, chunk_No, data);
    set_replication_degree(replication_degree);

    utilitarios.Notificacoes_Terminal.printNotificao("Created CHUNK " + file_ID + " @" + chunk_No);
  }

  /**
   * Construtor 3/3 de ChunkData
   *
   * @param chunk_info informação do chunk
   * @param data dados do chunk
   */
  public ChunkData(ChunkInfo chunk_info, byte[] data) {
    this(chunk_info.get_file_ID(), chunk_info.get_chunk_No(), chunk_info.get_replication_degree(), data);
  }

  /**
   * Obtem dados do chunk
   *
   * @return dados do chunk
   */
  public byte[] get_data() {
    return data;
  }

  /**
   * obtem tamanho dos dados do chunk
   *
   * @return tamanho dos dados do chunk
   */
  public int get_size() {
    return data.length;
  }

}