package filesystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Database extends PermanentStateClass {

  private static final long serialVersionUID = 1L;
  /**
   * @param string file id
   * @param FileInfo  informaçãod do ficheiro inclui chunks informacao
   */
  private ConcurrentMap<String, FileInfo> historic_files_backed_Up;

  /**
   * Maps (filePath -> FileInfo)
   */
  private ConcurrentMap<String, FileInfo> filesByPath;

  /**
   * History of the chunks in peer
   * @param String file id
   * @param <Int,Chunk Info> chunk number, chunk information
   */
  private ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> historic_chunks_backed_up;

  /**
   * Contains peerIDs of mirrors of local files. Maps (fileID -> Set<PeerID>)
   * @param String ficheiro ID
   * @param Set<Int> ids dos peers
   */
  private ConcurrentMap<String, Set<Integer>> fileMirrors;

  /**
   * Contains fileIDs of files that were deleted on the peer's network. DELETE -> add fileID to Set
   * PUTCHUNK -> remove fileID from Set
   */
  private Set<String> filesToDelete;

  Database(String savePath) {
    historic_files_backed_Up = new ConcurrentHashMap<>();
    filesByPath = new ConcurrentHashMap<>();
    historic_chunks_backed_up = new ConcurrentHashMap<>();

    fileMirrors = new ConcurrentHashMap<>();
    filesToDelete = new HashSet<>();

    this.setUp(savePath);
  }

  public boolean addToFilesToDelete(String fileID) {
    return filesToDelete.add(fileID);
  }

  /**
   * Remove o file do lixo - é como se fosse o TRASH do ubuntu
   * */
  public boolean removeFromFilesToDelete(String fileID) {
    return filesToDelete.remove(fileID);
  }


  public void addFileMirror(String fileID, int senderID) {
    fileMirrors.putIfAbsent(fileID, new ConcurrentSkipListSet<>());
    fileMirrors.get(fileID).add(senderID);
  }

  public Set<String> getFilesToDelete(int senderID) {
    Set<String> files = new ConcurrentSkipListSet<>();
    for (Map.Entry<String, Set<Integer>> fileMirrorEntry : fileMirrors.entrySet()) {
      for (Integer mirrorID : fileMirrorEntry.getValue()) {
        String fileID = fileMirrorEntry.getKey();
        if (mirrorID == senderID && filesToDelete.contains(fileID)) {
          files.add(fileID);
          break;
        }
      }
    }

    return files;
  }

  public void deleteFileMirror(String fileID, int senderID) {
    Set<Integer> peers = fileMirrors.get(fileID);
    if (peers != null) {
      peers.remove(senderID);
    }
  }

  public void addRestorableFile(FileInfo fileInfo) {
    historic_files_backed_Up.put(fileInfo.getFileID(), fileInfo);
    filesByPath.put(fileInfo.getPath(), fileInfo);
  }

  //Delete file from database
  public void removeRestorableFile(FileInfo fileInfo) {
    historic_files_backed_Up.remove(fileInfo.getFileID());
    filesByPath.remove(fileInfo.getPath());
  }

  public void removeRestorableFileByPath(String path) {
    removeRestorableFile(filesByPath.get(path));
  }

  public boolean hasBackedUpFileById(String fileID) {
    return historic_files_backed_Up.containsKey(fileID);
  }

  public boolean hasBackedUpFileByPath(String path) {
    return filesByPath.containsKey(path);
  }

  public FileInfo getFileInfoByPath(String pathName) {
    return filesByPath.get(pathName);
  }

  // historic_chunks_backed_up
  public boolean hasChunk(String fileID, int chunkNo) {

    Map<Integer, ChunkInfo> fileChunks = historic_chunks_backed_up.get(fileID);

    return fileChunks != null && fileChunks.containsKey(chunkNo);
  }

  public void addChunk(ChunkInfo chunkInfo, Integer parentPeerID) {
    chunkInfo.addMirror(parentPeerID);

    String fileID = chunkInfo.get_file_ID();
    int chunkNo = chunkInfo.get_chunk_No();

    ConcurrentMap<Integer, ChunkInfo> fileChunks;
    fileChunks = historic_chunks_backed_up.getOrDefault(fileID, new ConcurrentHashMap<>());
    fileChunks.putIfAbsent(chunkNo, chunkInfo);

    historic_chunks_backed_up.putIfAbsent(fileID, fileChunks);
  }

  public ChunkInfo getChunkInfo(String fileID, int chunkNo) {
    Map<Integer, ChunkInfo> fileChunks = historic_chunks_backed_up.get(fileID);

    return fileChunks != null ? fileChunks.get(chunkNo) : null;
  }

  public void removeChunk(String fileID, int chunkNo) {

    if (!historic_chunks_backed_up.containsKey(fileID)) {
      return;
    }

    historic_chunks_backed_up.get(fileID).remove(chunkNo);
  }

  public Map<Integer, ChunkInfo> removeChunksBackedUpByFileID(String fileID) {
    if (!historic_chunks_backed_up.containsKey(fileID)) {
      return null;
    }

    return historic_chunks_backed_up.remove(fileID);
  }

  public int getNumChunksByFilePath(String path) {
    return filesByPath.get(path).getNumChunks();
  }

  public String getFileInfoByFileID(String fileID) {
    return historic_files_backed_Up.get(fileID).getPath();
  }

  public Boolean addChunkMirror(String fileID, int chunkNo, int peerID) {
    boolean ret;
    try {
      ret = historic_chunks_backed_up.get(fileID).get(chunkNo).addMirror(peerID);
    } catch (NullPointerException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("addChunkMirror " + e.getMessage());
      return null;
    }

    return ret;
  }

  /**
   * Removes the given peerID as a mirror of the specified chunk.
   *
   * @param fileID The chunk's fileID
   * @param chunkNo The chunk's id number
   * @param peerID The peerID to be removed
   * @return True if the peerID was a mirror, False if it wasn't, null if ChunkData was not found
   */
  public Boolean removeChunkMirror(String fileID, int chunkNo, int peerID) {
    boolean ret;
    try {
      ret = historic_chunks_backed_up.get(fileID).get(chunkNo).removeMirror(peerID);
    } catch (NullPointerException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("(removeChunkMirror) ChunkData not found: " + e.getMessage());
      return null;
    }

    return ret;
  }

  public Integer getChunkPerceivedReplication(String fileID, int chunkNo) {

    int ret;
    try {
      ret = historic_chunks_backed_up.get(fileID).get(chunkNo).getNumMirrors();
    } catch (NullPointerException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("getChunkPerceivedReplication " + e.getMessage());
      return null;
    }

    return ret;
  }

  public boolean hasChunks(String fileID) {
    return historic_chunks_backed_up.containsKey(fileID);
  }

  public Collection<FileInfo> getHistoric_files_backed_Up() {
    return historic_files_backed_Up.values();
  }

  public ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> getHistoric_chunks_backed_up() {
    return historic_chunks_backed_up;
  }

  /**
   * Getter for any one ChunkData to be removed for reclaiming memory space.
   *
   * @return The chosen ChunkData.
   */
  public ChunkInfo getChunkForRemoval() {
    // Currently, chunk for removal is most backed-up chunk
    return getMostBackedUpChunk();
  }

  private ChunkInfo getMostBackedUpChunk() {
    ChunkInfo mostBackedUpChunk = null;
    int maxMirroring = -1;

    for (ConcurrentMap.Entry<String, ConcurrentMap<Integer, ChunkInfo>> fileEntry : historic_chunks_backed_up
        .entrySet()) {
      for (ConcurrentMap.Entry<Integer, ChunkInfo> chunkEntry : fileEntry.getValue().entrySet()) {
        int numMirrors = chunkEntry.getValue().getNumMirrors();
        if (numMirrors > maxMirroring) {
          maxMirroring = numMirrors;
          mostBackedUpChunk = chunkEntry.getValue();
        }
      }
    }

    return mostBackedUpChunk;
  }

}
