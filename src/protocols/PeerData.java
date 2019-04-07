package protocols;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.Collection;
import java.util.Collections;
import network.Message;
import filesystem.ChunkData;

public class PeerData {

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
   *
   */
  public PeerData() {
    received_stores = new ConcurrentHashMap<>();
    restored_chunks = new ConcurrentHashMap<>();
    chunk_observers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    stored_observers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  /**
   *
   * @param observer
   */
  public void attachStoredObserver(MessageObserver observer) {
    this.stored_observers.add(observer);
  }

  /**
   *
   * @param observer
   */
  public void detachStoredObserver(MessageObserver observer) {
    this.stored_observers.remove(observer);
  }

  /**
   *
   * @param msg
   */
  public void notifyStoredObservers(Message msg) {
    for (MessageObserver observer : stored_observers) {
      observer.update(msg);
    }
  }

  /**
   *
   * @param observer
   */
  public void attachChunkObserver(MessageObserver observer) {
    this.chunk_observers.add(observer);
  }

  /**
   *
   * @param observer
   */
  public void detachChunkObserver(MessageObserver observer) {
    this.chunk_observers.remove(observer);
  }

  /**
   *
   * @param msg
   */
  public void notifyChunkObservers(Message msg) {
    for (MessageObserver observer : chunk_observers) {
      observer.update(msg);
    }
  }

  /**
   *
   * @param flag
   * @param fileID
   */
  public void setFlagRestored(boolean flag, String fileID) {
    if (flag) {
      restored_chunks.putIfAbsent(fileID, new ConcurrentSkipListMap<>());
    } else {
      restored_chunks.remove(fileID);
    }
  }

  /**
   *
   * @param fileID
   * @return
   */
  public boolean getFlagRestored(String fileID) {
    return restored_chunks.containsKey(fileID);
  }

  /**
   *
   * @param chunk
   */
  public void addChunkToRestore(ChunkData chunk) {
    ChunkData ret = restored_chunks.get(chunk.getFileID()).putIfAbsent(chunk.getChunkNo(), chunk);

    if (ret != null) {
      utilitarios.Notificacoes_Terminal.printAviso("ChunkData already exists!");
    } else {
      utilitarios.Notificacoes_Terminal.printAviso("Adding chunk to merge!");
    }
  }

  /**
   *
   * @param fileID
   * @return
   */
  public Integer getChunksRestoredSize(String fileID) {
    return restored_chunks.get(fileID).size();
  }

  /**
   *
   * @param fileID
   * @return
   */
  public ConcurrentMap<Integer, ChunkData> getChunksRestored(String fileID) {
    return restored_chunks.get(fileID);
  }

  /**
   *
   * @param fileID
   */
  public void reset_replic(String fileID) {
    received_stores.remove(fileID);
  }

  /**
   *
   * @param fileID
   * @param numChunks
   */
  public void start_chunk_replic(String fileID, int numChunks) {
    utilitarios.Notificacoes_Terminal.printNotificao("incio da replicacao de " + fileID);
    received_stores.putIfAbsent(fileID, new AtomicIntegerArray(numChunks));
  }

  /**
   *
   * @param fileID
   * @param chunkNo
   * @return
   */
  public Integer inc_chunk_replic(String fileID, int chunkNo) {
    if (!received_stores.containsKey(fileID)) {
      utilitarios.Notificacoes_Terminal.printAviso("inc_chunk_replic não encontrou: " + fileID);
      return null;
    }

    int replication = received_stores.get(fileID).addAndGet(chunkNo, 1);
    utilitarios.Notificacoes_Terminal.printAviso("Aumento da replicação do" + fileID + "/chk" + chunkNo + " para " + replication);
    return replication;
  }

  /**
   *
   * @param fileID
   * @return
   */
  public AtomicIntegerArray get_replic(String fileID) {
    return received_stores.get(fileID);
  }

  /**
   *
   */
  interface MessageObserver {
    void update(Message msg);
  }

}
