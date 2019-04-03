package protocols.initiators;

import filesystem.ChunkInfo;
import filesystem.Database;
import filesystem.FileInfo;
import filesystem.MemoryManager;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import service.Peer;
import utils.Log;

public class RetrieveStateInitiator implements Runnable {

  private String version;
  private Peer parentPeer;
  private Database database;

  public RetrieveStateInitiator(String version, Peer parentPeer) {
    this.version = version;
    this.parentPeer = parentPeer;
    this.database = parentPeer.getDatabase();

    Log.logWarning("Starting retrieveStateInitiator!");
  }

  @Override
  public void run() {
    // Obtain info of the files from Database
    Collection<FileInfo> files = database.getFilesBackedUp();
    // Obtain info of the chunks from Database
    ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks = database.getChunksBackedUp();
    String out = getOutput(files, chunks);

    // Storage capacity
    MemoryManager mm = parentPeer.getSystemManager().getMemoryManager();
    out += "\n\nStorage: " +
        "\n Available memory: " + mm.getAvailableMemory() +
        "\n Used memory: " + mm.getUsedMemory();

    System.out.println(out); //TODO: Retrieve to TestApp
    Log.logWarning("Finished retrieveStateInitiator!");
  }

  private String getOutput(Collection<FileInfo> files,
      ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks) {
    // Save output string
    String out = "";
    out = saveLoop(files, out);
    out = chunkLoop(chunks, out);
    return out;
  }

  private String chunkLoop(ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks,
      String out) {
    //Loop to save the chunks
    out += "\n\nChunks:\n";
    out = saveLoop(chunks, out);
    return out;
  }

  private String saveLoop(ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks,
      String out) {
    for (Map.Entry<String, ConcurrentMap<Integer, ChunkInfo>> outer : chunks.entrySet()) {
      out += "\nFile: " + outer.getKey();
      for (Map.Entry<Integer, ChunkInfo> inner : outer.getValue().entrySet()) {
        ChunkInfo chunk = inner.getValue();
        out += "\n ChunkData: " +
            "\n  ChunkID: " + chunk.getChunkNo() +
            "\n  Size: " + chunk.getSize() / 1000 +
            "\n  Perceived Replication: " + chunk.getNumMirrors();
      }
    }
    return out;
  }

  private String saveLoop(Collection<FileInfo> files, String out) {
    // Loop to save the files
    out += "\nFiles:\n";
    out = chunkLoop(files, out);
    return out;
  }

  private String chunkLoop(Collection<FileInfo> files, String out) {
    for (FileInfo file : files) {
      out += "\nFile: " + file.getFileName() +
          "\n Pathname: " + file.getPath() +
          "\n FileID: " + file.getFileID() +
          "\n Desired Replication: " + file.getDesiredReplicationDegree() +
          "\n  Chunks:";
      ChunkInfo[] fileChunks = file.getChunks();
      for (ChunkInfo chunk : fileChunks) {
        out += "\n   ChunkID:" + chunk.getChunkNo();
      }
    }
    return out;
  }

}
