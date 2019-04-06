package protocols.initiators;

import static filesystem.SystemManager.splitFileInChunks;
import static protocols.Macros.MAX_NUM_CHUNKS;
import static protocols.Macros.MAX_REPLICATION_DEGREE;

import filesystem.ChunkData;
import filesystem.ChunkInfo;
import filesystem.FileInfo;
import filesystem.SystemManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import protocols.initiators.helpers.BackupChunkHelper;
import service.Peer;
import utilitarios.Utils;

public class BackupInitiator implements Runnable {

  private byte[] fileData;
  private int replicationDegree;
  private String pathname;
  private Peer parentPeer;
  private String version;

  public BackupInitiator(String version, String pathname, int replicationDegree, Peer parentPeer) {
    this.version = version;
    this.pathname = pathname;
    this.replicationDegree = replicationDegree;
    this.parentPeer = parentPeer;

    utilitarios.Notificacoes_Terminal.printAviso("Starting backupInitiator!");
  }

  @Override
  public void run() {
    fileData = SystemManager.loadFile(pathname);

    String fileID = generateFileID(pathname);
    ArrayList<ChunkData> chunks = splitFileInChunks(fileData, fileID, replicationDegree);

    if (!validBackup(replicationDegree, chunks.size())) {
      return;
    }

    addRestorableFile(chunks, fileID);
    parentPeer.get_peer_data().startChunkReplication(fileID, chunks.size());

    ArrayList<Thread> helperThreads = getThreadArrayList(chunks);

    resetReplic(fileID, helperThreads);

    utilitarios.Notificacoes_Terminal.printAviso("Finished BackupInitiator!");
  }

  private ArrayList<Thread> getThreadArrayList(ArrayList<ChunkData> chunks) {
    ArrayList<Thread> helperThreads = new ArrayList<>(chunks.size());
    for (ChunkData chunk : chunks) {
      Thread t = new Thread(new BackupChunkHelper(this, chunk));
      helperThreads.add(t);
      t.start();
    }
    return helperThreads;
  }

  private void resetReplic(String fileID, ArrayList<Thread> helperThreads) {
    try {
      joinWithThreads(helperThreads);
      parentPeer.get_peer_data().resetChunkReplication(fileID);
    } catch (InterruptedException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Backup: Failed join with helper threads");
    }
  }

  private boolean validBackup(int replicationDegree, int size) {
    if (replicationDegree > MAX_REPLICATION_DEGREE) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Backup: Failed replication degree greater than 9");
      return false;
    }

    if (size > MAX_NUM_CHUNKS) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Backup: Failed file size greater than 64GB");
      return false;
    }

    return true;
  }

  private void addRestorableFile(ArrayList<ChunkData> chunks, String fileID) {
    ChunkInfo[] chunkInfoArray = new ChunkInfo[chunks.size()];
    for (int i = 0; i < chunks.size(); i++) {
      ChunkData chunk = chunks.get(i);
      chunkInfoArray[i] = new ChunkInfo(fileID, chunk.getChunkNo(), chunk.getReplicationDegree(),
          chunk.getSize());
    }
    parentPeer.get_database()
        .addRestorableFile(new FileInfo(pathname, fileID, replicationDegree, chunkInfoArray));
  }

  private void joinWithThreads(List<Thread> threads) throws InterruptedException {
    for (Thread t : threads) {
      t.join();
    }
  }

  private String generateFileID(String pathname) {
     return Utils.hash(generateUnhashedFileID(pathname));
  }

  //CH
  private String generateUnhashedFileID(String absPath) {
    BasicFileAttributes attr;
    String owner;//CH
    try {
      attr = Files.readAttributes(Paths.get(absPath), BasicFileAttributes.class);
      owner = Files.getOwner(Paths.get(absPath)).getName() ;
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't read file's metadata: " + e.getMessage());
      return null;
    }

    Path pathObj = Paths.get(absPath);
    return pathObj.getFileName().toString() + attr.lastModifiedTime() + owner;//CH
  }

  public Peer getParentPeer() {
    return parentPeer;
  }

  public String getProtocolVersion() {
    return version;
  }
}
