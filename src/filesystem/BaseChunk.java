package filesystem;

import java.io.Serializable;

public class BaseChunk implements Serializable {

  private String file_ID;
  private int chunk_No;
  private Integer replication_degree;

  /**
   * construtor da class BaseChunk
   *
   * @param file_ID identificador do ficheiro
   * @param chunk_No numero do chunk
   * @param replication_degree grau de replicação
   */
  BaseChunk(String file_ID, int chunk_No, Integer replication_degree) {
    this.file_ID = file_ID;
    this.chunk_No = chunk_No;
    this.replication_degree = replication_degree;
  }

  /**
   * Obtem identificador do ficheiro
   *
   * @return identificador do ficheiro
   */
  public String get_file_ID() {
    return file_ID;
  }

  /**
   * Obtem numero do chunk
   *
   * @return numero do chunk
   */
  public int get_chunk_No() {
    return chunk_No;
  }

  /**
   * Obtem grau de replicação
   *
   * @return grau de replicação
   */
  public int get_replication_degree() {
    return replication_degree;
  }

  /**
   * Estabelece o grau de replicação
   *
   * @param replication_degree grau de replicação
   */
  protected void set_replication_degree(int replication_degree) {
    this.replication_degree = replication_degree;
  }
}
