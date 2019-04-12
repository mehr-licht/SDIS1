package protocols.initiators;

import filesystem.ChunkInfo;
import filesystem.Database;
import filesystem.FileInfo;
import filesystem.MemoryManager;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import service.Peer;

public class StateInit implements Runnable {

  private String version;
  private Peer parent_peer;
  private Database database;

  /**
   * classe StateInit
   */
  public StateInit(Peer parent_peer, String version) {
    this.version = version;
    this.parent_peer = parent_peer;
    this.database = parent_peer.get_database();

    utilitarios.Notificacoes_Terminal.printAviso("A começar o pedido de Estado na fonte");
  }

  /**
   * Lançamento do stateInit
   */
  @Override
  public void run() {
    // Obtain info of the files from Database
    Collection<FileInfo> files = database.getHistoric_files_backed_Up();
    // Obtain info of the chunks from Database
    ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks = database.getHistoric_chunks_backed_up();
    String out = get_output(files, chunks);

    // Storage capacity
    MemoryManager mm = parent_peer.get_system_manager().get_memory_manager();
   /* long available=  mm.getMaxMemory() - mm.getUsedMemory();
    System.out.println("mem:"+mm.getMemory());
    System.out.println("max:"+mm.getMaxMemory());
    System.out.println("used:"+mm.getUsedMemory());
    System.out.println("avail:"+mm.getAvailableMemory());
*/
    out +=
        "\n\nStorage: "
            + "\n Total memory: "
            + mm.getMaxMemory()
            + "\n Used memory: "
            + mm.getUsedMemory()
            + "\n Available memory: "
            + mm.getAvailableMemory();

    System.out.println(out); // TODO: enviar para o TestApp
    utilitarios.Notificacoes_Terminal.printAviso("Terminou o pedido de Estado na fonte");
  }

  /**
   * Compoe a mensagem do estado do peer para apresentar no terminal
   *
   * @param files colecção dos ficheiros que estão no peer
   * @param chunks colecção dos chunks que estão no peer
   * @return toda a informação de ficheiros e chunks no peer concatenada e pronta a ser enviada para o terminal
   */
  private String get_output(
      Collection<FileInfo> files, ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks) {
    String out = "";
    out += output_to_save(files);
    out += all_chunks_info(chunks);
    return out;
  }

  /**
   * Obtem a informação dos chunks guardados no peer
   *
   * @param chunks colecção de chunks guardados no peer
   * @return cabeçalho "chunks" concatenado com a concatenação da informação dos chunks guardados no peer
   */
  private String all_chunks_info(ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks) {
    String out = "";
    out += "\n\nChunks:\n";
    out += files_with_chunks_loop(chunks);
    return out;
  }

  /**
   * Devolve a informação de todos os ficheiros replicados em cada peer e seus chunks
   *
   * @param chunks colecção dos chunks guardados no peer
   * @return concatenação das informações dos chunks de um ficheiro que estão num peer
   */
  private String files_with_chunks_loop(ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> chunks) {
    String out = "";
    for (Map.Entry<String, ConcurrentMap<Integer, ChunkInfo>> outer : chunks.entrySet()) {
      out += "\nFile: " + outer.getKey();
      out += chunks_of_each_file_loop(outer);
    }
    return out;
  }

  /**
   * Devolve a informação de todos os chunks de cada ficheiro
   *
   * @param outer chave da colecção correspondendo ao ficheiro com chunks
   * @return concatenação da informação de todos os chunks de cada ficheiro
   */
  private String chunks_of_each_file_loop(Entry<String, ConcurrentMap<Integer, ChunkInfo>> outer) {
    String tmp = "";
    for (Entry<Integer, ChunkInfo> inner : outer.getValue().entrySet()) {
      ChunkInfo chunk = inner.getValue();
      tmp +=
          "\n ChunkData: "
              + "\n  ChunkID: "
              + chunk.get_chunk_No()
              + "\n  Size: "
              + (float)chunk.getSize() / 1000
              + "\n  Perceived Replication: "
              + chunk.getNumMirrors();
    }
    return tmp;
  }

  /**
   * Obtem a informação dos ficheiros no peer
   *
   * @param files colecção dos ficheiros no peer
   * @return cabeçalho "files" concatenado com a concatenação da informação dos ficheiros no peer
   */
  private String output_to_save(Collection<FileInfo> files) {
    String out = "";
    out += "\nFiles:\n";
    out += files_in_peer_loop(files);
    return out;
  }

  /**
   * Devolve a informação de todos os ficheiros no peer e seus chunks
   *
   * @param files ficheiros no peer
   * @return cancatenação dos dados dos ficheiros
   */
  private String files_in_peer_loop(Collection<FileInfo> files) {
    String tmp = "";
    for (FileInfo file : files) {
      tmp +=
          "\nFile: "
              + file.getFileName()
              + "\n Pathname: "
              + file.getPath()
              + "\n FileID: "
              + file.getFileID()
              + "\n Desired Replication: "
              + file.getDesiredReplicationDegree()
              + "\n  Chunks:";
      ChunkInfo[] fileChunks = file.getChunks();
      tmp += add_chunk_IDs_loop(fileChunks);
    }
    return tmp;
  }

  /**
   * Adiciona os ids de cada chunk do ficheiro ao output
   *
   * @param fileChunks chunks do ficheiro
   * @return concatenação dos ids dos chunks
   */
  private String add_chunk_IDs_loop(ChunkInfo[] fileChunks) {
    String tmp = "";
    for (ChunkInfo chunk : fileChunks) {
      tmp += "\n   ChunkID:" + chunk.get_chunk_No();
    }
    return tmp;
  }
}
