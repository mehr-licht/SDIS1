package protocols;

import static filesystem.SystemManager.createFolder;
import static utilitarios.Utils.*;

import java.io.IOException;
import java.util.Random;
import canais.Canal;
import service.Peer;
import filesystem.ChunkInfo;
import filesystem.SystemManager.SAVE_STATE;
import network.Message;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * classe Backup
 */
public class Backup implements Runnable, Peer_Info.MessageObserver {

  private Peer parent_peer;
  private Message request;

  private Random random;
  private Future handler = null;

  private int stored_count = 0;

  private ScheduledExecutorService scheduled_executor;

  public Backup(Peer parent_peer, Message request) {
    this.parent_peer = parent_peer;
    this.request = request;

    this.random = new Random();
    this.scheduled_executor = Executors.newSingleThreadScheduledExecutor();

    utilitarios.Notificacoes_Terminal.printAviso("A iniciar o backup");
  }

  /**
   * Lançamento do backup
   */
  @Override
  public void run() {
    int sender_ID = request.get_Sender_ID();
    String file_ID = request.get_file_ID();
    int chunk_No = request.get_Chunk_Numero();
    int replication_degree = request.get_File_Replication_Degree();

    if (sender_ID == parent_peer.get_ID()) { // a peer never stores the chunks of its own files
      return;
    }

    byte[] chunk_data = request.get_Corpo_Mensagem();

    String chunk_path = parent_peer.get_path("backup") + "/" + file_ID;
    createFolder(parent_peer.get_path("backup") + "/" + file_ID);

    enhancement_compatibility_handle(file_ID, chunk_No, chunk_data, chunk_path, replication_degree);

    utilitarios.Notificacoes_Terminal.printAviso("-----------------Backup terminado-----------------------");
  }

  /**
   * Decide que tipo de Backup handler, se acordo com versao do protocolo
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @param chunk_data data do chunk
   * @param chunk_path caminho do chunk
   * @param replication_degree grau de replicação
   */
  private void enhancement_compatibility_handle(String file_ID, int chunk_No,
      byte[] chunk_data, String chunk_path, int replication_degree) {
    if (enhancements_compatible(parent_peer, request, BACKUP_ENH)) {
      handle_enhanced_request(file_ID, chunk_No, chunk_data, chunk_path, replication_degree);
    } else {
      handle_standard_request(file_ID, chunk_No, chunk_path, chunk_data, replication_degree);
    }
  }

  /**
   * Versão standard do protocolo: envia quando possível
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @param chunk_data data do chunk
   * @param chunk_path caminho do chunk
   * @param replication_degree grau de replicação
   */
  private void handle_standard_request(String file_ID, int chunk_No, String chunk_path,
      byte[] chunk_data, int replication_degree) {
    boolean success = save_chunk(file_ID, chunk_No, chunk_data, chunk_path, replication_degree);
    if (success) {
      send_delayed_STORED(request);
    }
  }

  /**
   * Versão melhorada do protocolo: envia logo
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @param chunk_data data do chunk
   * @param chunk_path caminho do chunk
   * @param replication_degree grau de replicação
   */
  private void handle_enhanced_request(String file_ID, int chunk_No, byte[] chunk_data,
      String chunk_path, int replication_degree) {
    parent_peer.get_peer_data().add_stored_observer(this);

    create_handler(file_ID, chunk_No, chunk_data, chunk_path, replication_degree);

    wait_and_remove();
  }

  /**
   * Cria handler do executor de backup e manda enviar logo
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @param chunk_data data do chunk
   * @param chunk_path caminho do chunk
   * @param replication_degree grau de replicação
   */
  private void create_handler(String file_ID, int chunk_No, byte[] chunk_data, String chunk_path,
      int replication_degree) {
    this.handler = scheduled_executor.schedule(
        () -> {
          boolean success = save_chunk(file_ID, chunk_No, chunk_data, chunk_path,
              replication_degree);
          if (success) {
            send_STORED(request);
          }
        },
        this.random.nextInt(MAX_DELAY + 1),
        TimeUnit.MILLISECONDS
    );
  }

  /**
   * Remove o observador de backup
   */
  private void wait_and_remove() {
    try {
      this.handler.wait();
      parent_peer.get_peer_data().remove_stored_observer(this);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Guarda o chunk no path fornecido
   *
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @param chunk_data data do chunk
   * @param chunk_path caminho do chunk
   * @param replication_degree grau de replicação
   * @return sucesso ou insucesso
   */
  private boolean save_chunk(String file_ID, int chunk_No, byte[] chunk_data,
      String chunk_path, int replication_degree) {
    SAVE_STATE ret;
    try {
      ret = parent_peer.get_system_manager().saveFile(
          "chk"+ chunk_No,
          chunk_path,
          chunk_data
      );
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possivel guardar o chunk");
      return false;
    }

    if (ret == SAVE_STATE.SUCCESS) {
      parent_peer.get_database().addChunk(
          new ChunkInfo(file_ID, chunk_No, replication_degree, chunk_data.length),
          parent_peer.get_ID()
      );
    } else { // Não enviia STORED se chunk já existe
      utilitarios.Notificacoes_Terminal.printAviso("Backup de dados do chunk: " + ret);
      return false;
    }

    return true;
  }

  /**
   * Envia mensagem de guardado
   *
   * @param request pedido a que se refere a mensagem
   */
  private void send_STORED(Message request) {
    Message msg = make_STORED(request);

    try {
      parent_peer.send_message(msg, Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  /**
   * Envia mensagem de guardado quando possivel pelo canal multicast
   *
   * @param request
   */
  private void send_delayed_STORED(Message request) {
    Message msg = make_STORED(request);

    parent_peer.send_delayed_message(
        msg, Canal.ChannelType.MC,
        random.nextInt(MAX_DELAY + 1),
        TimeUnit.MILLISECONDS
    );
  }

  /**
   * Compõe datagrama stored
   *
   * @param request pedido a que se refere o datagrama
   * @return datagrama a enviar
   */
  private Message make_STORED(Message request) {
    String[] args = {
        parent_peer.get_version(),
        Integer.toString(parent_peer.get_ID()),
        request.get_file_ID(),
        Integer.toString(request.get_Chunk_Numero())
    };

    return new Message(Message.Categoria_Mensagem.STORED, args);
  }

  /**
   * Actualização do estado da transmissão e cancelamento se necessário
   *
   * @param msg datagrama
   */
  @Override
  public void update(Message msg) {
    if (this.handler == null) {
      return;
    }
    if ((!(msg.get_file_ID().equals(request.get_file_ID()))) || (!(msg.get_Chunk_Numero() == request.get_Chunk_Numero()))) {
      return;
    }

    //stored_count += 1;
    if (!(request.get_File_Replication_Degree() > ++stored_count)) {
      // Cancela se o grau de replicação já atingiu o definido
      this.handler.cancel(true);
    }
  }
}