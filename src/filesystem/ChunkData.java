package filesystem;

public class ChunkData extends BaseChunk {

  private byte[] data;

  public ChunkData(String fileID, int chunkNo, byte[] data) {
    super(fileID, chunkNo, null);
    this.data = data;
  }

  public ChunkData(String fileID, int chunkNo, int replicationDegree, byte[] data) {
    this(fileID, chunkNo, data);
    setReplicationDegree(replicationDegree);

    utilitarios.Notificacoes_Terminal.printNotificao("Created CHUNK " + fileID + " @" + chunkNo);
  }

  public ChunkData(ChunkInfo chunkInfo, byte[] data) {
    this(chunkInfo.getFileID(), chunkInfo.getChunkNo(), chunkInfo.getReplicationDegree(), data);
  }


  public byte[] getData() {
    return data;
  }

  public int getSize() {
    return data.length;
  }

}