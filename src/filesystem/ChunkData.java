package filesystem;

public class ChunkData extends BaseChunk {

  private byte[] data;

  public ChunkData(String fileID, int chunkNo, byte[] data) {
    super(fileID, chunkNo, null);
    this.data = data;
  }

  public ChunkData(String fileID, int chunkNo, int replicationDegree, byte[] data) {
    this(fileID, chunkNo, data);
    set_replication_degree(replicationDegree);

    utilitarios.Notificacoes_Terminal.printNotificao("Created CHUNK " + fileID + " @" + chunkNo);
  }

  public ChunkData(ChunkInfo chunkInfo, byte[] data) {
    this(chunkInfo.get_file_ID(), chunkInfo.get_chunk_No(), chunkInfo.get_replication_degree(), data);
  }


  public byte[] getData() {
    return data;
  }

  public int getSize() {
    return data.length;
  }

}