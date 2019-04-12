package protocols;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.Collection;
import java.util.Collections;
import network.Message;
import filesystem.ChunkData;

/**
 * classe Peer_Info
 */
public class Peer_Info {

  /**
   * Mapa com o número de mensagens de STORE recebidas.
   * < file_ID => < chunk_num => num_stores_received > >
   */
  private ConcurrentMap<String, AtomicIntegerArray> received_stores;

  /**
   * Mapa dos chunks restaurados. < file_ID => < chunk_num => chunk_data > >
   */
  private ConcurrentMap<String, ConcurrentSkipListMap<Integer, ChunkData>> restored_chunks;

  /**
   * Colecções de observadores dos datagramas CHUNK. Essencial para o sub-protocolo de Restore.
   */
  private Collection<MessageObserver> chunk_observers;
  private Collection<MessageObserver> stored_observers;

  /**
   * contructor da classe Peer_Info
   */
  public Peer_Info() {
    received_stores = new ConcurrentHashMap<>();
    restored_chunks = new ConcurrentHashMap<>();
    chunk_observers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    stored_observers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  /**
   * Adiciona o observador de BACKUP à colecção
   *
   * @param observer observador a adicionar
   */
  public void add_stored_observer(MessageObserver observer) {
    this.stored_observers.add(observer);
  }

  /**
   * Remove o observador de BACKUP da colecção
   *
   * @param observer observador a remover
   */
  public void remove_stored_observer(MessageObserver observer) {
    this.stored_observers.remove(observer);
  }

  /**
   * Avisa os observadores de BACKUP de novas mensagens STORED
   *
   * @param msg mensagem STORED
   */
  public void notify_stored_observers(Message msg) {
    for (MessageObserver observer : stored_observers) {
      observer.update(msg);
    }
  }

  /**
   * Adiciona o observador de RESTORE à colecção
   *
   * @param observer observador a adicionar
   */
  public void add_chunk_observer(MessageObserver observer) {
    this.chunk_observers.add(observer);
  }

  /**
   * Remove o observador de RESTORE da colecção
   *
   * @param observer observador a remover
   */
  public void remove_chunk_observer(MessageObserver observer) {
    this.chunk_observers.remove(observer);
  }

  /**
   * Avisa os observadores de RESTORE de novas mensagens CHUNK
   *
   * @param msg mensagem CHUNK
   */
  public void notify_chunk_observers(Message msg) {
    for (MessageObserver observer : chunk_observers) {
      observer.update(msg);
    }
  }

  /**
   * Quando começa o RESTORE associa chave ao valor no mapa. Se apagado, remove
   *
   * @param file_ID identificador do ficheiro
   * @param flag true ou false
   */
  public void set_restored_flag(String file_ID, boolean flag) {
    if (flag) {
      restored_chunks.putIfAbsent(file_ID, new ConcurrentSkipListMap<>());
    } else {
      restored_chunks.remove(file_ID);
    }
  }

  /**
   * Vai buscar o estado de restore do ficheiro
   *
   * @param file_ID identificador do ficheiro
   * @return flag estado de restore do ficheiro
   */
  public boolean get_restored_flag(String file_ID) {
    return restored_chunks.containsKey(file_ID);
  }

  /**
   * Vai buscar a data do chunk
   *
   * @param chunk data do chunk
   */
  public void get_restored_chunk_data(ChunkData chunk) {
    ChunkData ret = restored_chunks.get(chunk.getFileID()).putIfAbsent(chunk.getChunkNo(), chunk);

    if (ret != null) {
      utilitarios.Notificacoes_Terminal.printAviso("ChunkData already exists!");
    } else {
      utilitarios.Notificacoes_Terminal.printAviso("Adding chunk to merge!");
    }
  }

  /**
   * Vai buscar o numero de chunks a restaurar do ficheiro
   *
   * @param file_ID identificador do ficheiro
   * @return numero de chunks do ficheiro
   */
  public Integer get_number_restored_chunks(String file_ID) {
    return restored_chunks.get(file_ID).size();
  }

  /**
   * Vai buscar o identificador do chunk a restaurar
   *
   * @param file_ID identificador do ficheiro
   * @return identificador do chunk a restaurar
   */
  public ConcurrentMap<Integer, ChunkData> get_restored_chunk_id(String file_ID) {
    return restored_chunks.get(file_ID);
  }

  /**
   * Coloca grau de replicação do ficheiro a zero
   *
   * @param file_ID identificador do ficheiro
   */
  public void reset_replic(String file_ID) {
    received_stores.remove(file_ID);
  }

  /**
   *
   *
   * @param file_ID identificador do ficheiro
   * @param num_chunks
   */
  public void start_chunk_replic(String file_ID, int num_chunks) {
    utilitarios.Notificacoes_Terminal.printNotificao("incio da replicacao de " + file_ID);
    received_stores.putIfAbsent(file_ID, new AtomicIntegerArray(num_chunks));
  }

  /**
   * Incrementa o grau de replicação do chunk
   *
   * @param file_ID identificador do ficheiro
   * @param chunk_No Numero do Chunk
   * @return grau de replicação após o incremento
   */
  public Integer inc_chunk_replic(String file_ID, int chunk_No) {

    if (!received_stores.containsKey(file_ID)) {
      utilitarios.Notificacoes_Terminal.printAviso("inc_chunk_replic não encontrou: " + file_ID);
      return null;
    }

    int replication = received_stores.get(file_ID).addAndGet(chunk_No, 1);
    utilitarios.Notificacoes_Terminal.printAviso("Aumento da replicação do" + file_ID + "/chk" + chunk_No + " para " + replication);
    return replication;
  }

  /**
   * Vai buscar o grau de replicação do ficheiro
   *
   * @param file_ID identificador do ficheiro
   * @return grau de replicação do ficheiro
   */
  public AtomicIntegerArray get_replic(String file_ID) {
    return received_stores.get(file_ID);
  }

  /**
   * Actualiza observador
   */
  interface MessageObserver {
    void update(Message msg);
  }

}
