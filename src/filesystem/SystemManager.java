package filesystem;

import static java.util.Arrays.copyOfRange;
import static utilitarios.Utils.MAX_CHUNK_SIZE;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import service.Peer;

public class SystemManager {

  public static final String FILES = "../files/";
  private static final String BACKUPS = "backup/";
  private static final String RESTORED = "restored/";
  private Peer parentPeer;
  private String rootPath;
  private Database database;
  private MemoryManager memoryManager;

  public SystemManager(Peer parentPeer, long maxMemory) {
    this.parentPeer = parentPeer;
    this.rootPath = "fileSystem/peer" + parentPeer.get_ID() + "/";

    initializePeerFileSystem();

    initializePermanentState(maxMemory);
  }

  public static void createFolder(String name) {
    try {
      Files.createDirectories(Paths.get(name));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't create file directory!");
    }
  }

  synchronized public static byte[] loadFile(String pathname) {
    InputStream inputStream;
    long fileSize;
    byte[] data;

    try {
      inputStream = Files.newInputStream(Paths.get(pathname));
      fileSize = getFileSize(Paths.get(pathname));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("File not found!");
      return null;
    }

    data = new byte[(int) fileSize];

    try {
      inputStream.read(data);
      inputStream.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't read data of a file!");
    }

    return data;
  }

  public static long getFileSize(Path filepath) {
    BasicFileAttributes attr = null;
    System.out.println(filepath);
    try {
      attr = Files.readAttributes(filepath, BasicFileAttributes.class);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't read attributes of a file!");
    }
    return attr.size();
  }

  public static ArrayList<ChunkData> splitFileInChunks(byte[] fileData, String fileID,
      int replicationDegree) {
    ArrayList<ChunkData> chunks = new ArrayList<>();

    int numChunks = fileData.length / MAX_CHUNK_SIZE + 1;

    for (int i = 0; i < numChunks; i++) {
      byte[] chunkData;

      if (i == numChunks - 1 && fileData.length % MAX_CHUNK_SIZE == 0) {
        chunkData = new byte[0];
      } else if (i == numChunks - 1) {
        int leftOverBytes = fileData.length - (i * MAX_CHUNK_SIZE);
        chunkData = copyOfRange(fileData, i * MAX_CHUNK_SIZE, i * MAX_CHUNK_SIZE + leftOverBytes);
      } else {
        chunkData = copyOfRange(fileData, i * MAX_CHUNK_SIZE, i * MAX_CHUNK_SIZE + MAX_CHUNK_SIZE);
      }

      ChunkData chunk = new ChunkData(fileID, i, replicationDegree, chunkData);
      chunks.add(chunk);
    }

    return chunks;
  }

  public static byte[] fileMerge(ArrayList<ChunkData> chunks) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    for (int i = 0; i < chunks.size(); i++) {
      try {
        outputStream.write(chunks.get(i).getData());
      } catch (IOException e) {
        utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't merge chunks of a file!");
      }
    }

    return outputStream.toByteArray();
  }

  private void initializePermanentState(long maxMemory) {
    try {
      initializeMemoryManager(maxMemory);
      initializeDatabase();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  private void initializeMemoryManager(long maxMemory) throws IOException, ClassNotFoundException {
    File mm = new File(rootPath + "memoryManager");

    if (mm.exists()) {
      this.memoryManager = (MemoryManager) MemoryManager.loadFromFile(mm);
    } else {
      this.memoryManager = new MemoryManager(maxMemory, mm.getAbsolutePath());
    }
  }

  private void initializeDatabase() throws IOException, ClassNotFoundException {
    File db = new File(rootPath + "db");

    if (db.exists()) {
      this.database = (Database) Database.loadFromFile(db);
    } else {
      this.database = new Database(db.getAbsolutePath());
    }
  }

  synchronized public SAVE_STATE saveFile(String fileName, String pathname, byte[] data)
      throws IOException {
    if (memoryManager.getAvailableMemory() < data.length) {
      utilitarios.Notificacoes_Terminal.printAviso("Not enough space for saveFile!");
      return SAVE_STATE.FAILURE;
    }
    String filePath = pathname + "/" + fileName;

    if (Files.exists(Paths.get(filePath))) {
      utilitarios.Notificacoes_Terminal.printAviso("File already exists!");
      return SAVE_STATE.EXISTS;
    }

    OutputStream out = Files.newOutputStream(Paths.get(filePath));
    out.write(data);
    out.close();

    memoryManager.increaseUsedMemory(data.length);
    return SAVE_STATE.SUCCESS;
  }

  public String getChunkPath(String fileID, int chunkNo) {
    return getChunksPath() + fileID + "/chk" + chunkNo;
  }

  public byte[] loadChunk(String fileID, int chunkNo) {
    String chunkPath = getChunkPath(fileID, chunkNo);
    return loadFile(chunkPath);
  }

  private void initializePeerFileSystem() {
    createFolder(rootPath + BACKUPS);
    createFolder(rootPath + RESTORED);
  }

  public String getRootPath() {
    return rootPath;
  }

  public String getChunksPath() {
    return rootPath + BACKUPS;
  }

  public String getRestoredPath() {
    return rootPath + RESTORED;
  }

  public Database getDatabase() {
    return database;
  }

  public void deleteChunk(String fileID, int chunkNo) {
    String chunkPath = getChunkPath(fileID, chunkNo);
    Path path = Paths.get(chunkPath);

    long chunkSize = getFileSize(path);
    try {
      Files.delete(path);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't delete file: " + path);
    }
    memoryManager.reduceUsedMemory(chunkSize);
    database.removeChunk(fileID, chunkNo);
  }

  public MemoryManager getMemoryManager() {
    return memoryManager;
  }

  public enum SAVE_STATE {
    EXISTS,
    SUCCESS,
    FAILURE
  }

}