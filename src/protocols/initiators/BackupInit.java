package protocols.initiators;

import static utilitarios.Utils.MAX_NUM_CHUNKS;
import static utilitarios.Utils.MAX_REPLICATION_DEGREE;
import static filesystem.SystemManager.splitFileInChunks;

import service.Peer;
import utilitarios.Utils;
import filesystem.ChunkData;
import filesystem.ChunkInfo;
import filesystem.FileInfo;
import filesystem.SystemManager;
import protocols.initiators.helpers.BackupChunkHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * classe BackupInit
 */
public class BackupInit implements Runnable {

  private byte[] file_data;
  private int replic_degree;
  private String pathname;
  private Peer parent_peer;
  private String version;

  /**
   * construtor de BackupInit
   *
   * @param parent_peer
   * @param pathname
   * @param replic_degree
   * @param version
   */
  public BackupInit(Peer parent_peer, String pathname, int replic_degree, String version) {
    this.version = version;
    this.pathname = pathname;
    this.replic_degree = replic_degree;
    this.parent_peer = parent_peer;

    utilitarios.Notificacoes_Terminal.printAviso("A começar o backup na fonte");
  }

  /**
   * lança backupInit
   */
  @Override
  public void run() {
    file_data = SystemManager.loadFile(pathname);

    String file_ID = generate_file_ID(pathname);
    ArrayList<ChunkData> chunks = splitFileInChunks(file_data, file_ID, replic_degree);

    if (!valid_backup(replic_degree, chunks.size())) {
      return;
    }

    add_restorable_file(chunks, file_ID);
    parent_peer.get_peer_data().start_chunk_replic(file_ID, chunks.size());

    ArrayList<Thread> helper_threads = get_thread_array_list(chunks);

    reset_replic(helper_threads, file_ID);

    utilitarios.Notificacoes_Terminal.printAviso("Terminou o backup na fonte");
  }

  /**
   * Cria uma thread auxiliar por cada chunk
   *
   * @param chunks lista de chunks de um ficheiro
   * @return lista de threads auxiliares
   */
  private ArrayList<Thread> get_thread_array_list(ArrayList<ChunkData> chunks) {
    ArrayList<Thread> helper_threads = new ArrayList<>(chunks.size());
    for (ChunkData chunk : chunks) {
      Thread t = new Thread(new BackupChunkHelper(this, chunk));
      helper_threads.add(t);
      t.start();
    }
    return helper_threads;
  }

  /**
   * Coloca grau de replicação do ficheiro a zero
   *
   * @param helper_threads threads auxiliares
   * @param file_ID identificador do ficheiro
   */
  private void reset_replic(ArrayList<Thread> helper_threads, String file_ID) {
    try {
      join_threads(helper_threads);
      parent_peer.get_peer_data().reset_replic(file_ID);
    } catch (InterruptedException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Backup: join das helper_threads falhou");
    }
  }

  /**
   * verifica se o pedido de backup cumpre os valores máximos definidos
   *
   * @param replic_degree grau de replicação do ficheiro
   * @param size tamanho do ficheiro
   * @return verdadeiro ou falso
   */
  private boolean valid_backup(int replic_degree, int size) {
    if (replic_degree_high(replic_degree))return false;

    if (size_too_big( size)) return false;

    return true;
  }

  /**
   * Verifica se o grau de replicação pedido é maior que o máximo definido
   *
   * @param replic_degree grau de replicação pedido
   * @return verdadeiro ou falso
   */
  private boolean replic_degree_high(int replic_degree){
    if (MAX_REPLICATION_DEGREE >= replic_degree) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Backup: grau de replicacao maior que o máximo definido(" + MAX_REPLICATION_DEGREE + ")");
      return false;
      }
    return true;
  }

  /**
   * Verifica se o ficheiro não é maior que o tamanho máximo definido
   *
   * @param size tamanho do ficheiro
   * @return verdadeiro ou falso
   */
  private boolean size_too_big(int size){
    if (size >= MAX_NUM_CHUNKS ) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Backup: Tamanho do ficheiro maior que o máximo definido");
      return true;
    }
    return false;
  }


  /**
   * Adiciona o ficheiro como restauravel
   *
   * @param chunks lista de chunks do ficheiro
   * @param file_ID identificador do ficheiro
   * */
  private void add_restorable_file(ArrayList<ChunkData> chunks, String file_ID) {
    ChunkInfo[] chunk_info_array = new ChunkInfo[chunks.size()];
    for (int i = 0; i < chunks.size(); i++) {
      ChunkData chunk = chunks.get(i);
      chunk_info_array[i] = new ChunkInfo(file_ID, chunk.getChunkNo(), chunk.getReplicationDegree(),
          chunk.getSize());
    }
    parent_peer.get_database()
        .addRestorableFile(new FileInfo(pathname, file_ID, replic_degree, chunk_info_array));
  }

  /**
   * Faz join das helper_threads para esperar que elas terminem
   *
   * @param threads helper_threads
   * @throws InterruptedException Exceção a ser lançada se a thread referenciada for interrompida
   */
  private void join_threads(List<Thread> threads) throws InterruptedException {
    for (Thread t : threads) {
      t.join();
    }
  }

  /**
   * Gera o identificador final do ficheiro
   *
   * @param pathname caminho completo do ficheiro
   * @return identificador final do ficheiro já concatenado e hashado
   */
  private String generate_file_ID(String pathname) {
     return Utils.hash(generate_unhashed_file_ID(pathname));
  }

  /**
   * Cria um identificador do ficheiro
   *
   * @param abs_path caminho absoluto do ficheiro
   * @return identificador do ficheiro
   */
  private String generate_unhashed_file_ID(String abs_path) {
    BasicFileAttributes attr;
    String owner;//CH
    try {
      attr = Files.readAttributes(Paths.get(abs_path), BasicFileAttributes.class);
      owner = Files.getOwner(Paths.get(abs_path)).getName() ;
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível ler a metadata do ficheiro: " + e.getMessage());
      return null;
    }

    Path path_obj = Paths.get(abs_path);
    return path_obj.getFileName().toString() + attr.lastModifiedTime() + owner;//CH
  }

  /**
   * Obtem o peer que iniciou o serviço
   *
   * @return peer que iniciou o serviço
   */
  public Peer get_parent_peer() {
    return parent_peer;
  }

  /**
   * Obtem a versão do protocolo
   *
   * @return versão do protocolo
   */
  public String get_version() {
    return version;
  }
}
