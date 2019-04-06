package service;

import static protocols.Macros.*;
import static utilitarios.Utils.parse_RMI;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import protocols.PeerData;
import protocols.initiators.*;
import network.AbstractMessageDispatcher;
import network.ConcreteMessageDispatcher;
import network.Message;
import filesystem.Database;
import filesystem.SystemManager;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import channels.Channel;
import channels.Channel.ChannelType;
import channels.MChannel;
import channels.MDBChannel;
import channels.MDRChannel;

/**
 * classe Peer
 */
public class Peer implements MyRemote {

  private final String[] server_access_point;
  private final String protocol_version;
  private final int id;

  private AbstractMessageDispatcher message_dispatcher;
  private Map<ChannelType, Channel> channels;

  //serviço Executor service responsável pelo escalonamento das respostas e fazer todas as tarefas dos sub-protocolos RMI.
  private ScheduledExecutorService executor;

  private SystemManager system_manager;
  private Database database;
  private PeerData peer_data;

  /**
   * Construtor de um Peer
   *
   * @param protocol_version versão do protocolo
   * @param id id do peer
   * @param server_access_point ponto de acesso do peer
   * @param mc_address endereço do canal de controle
   * @param mdb_address endereço do canal do backup de dados
   * @param mdr_address endereço do canal do restore de dados
   */
  public Peer(String protocol_version,int id,String[] server_access_point,String[] mc_address,String[] mdb_address,String[] mdr_address) {
    this.protocol_version = protocol_version;
    this.id = id;
    this.server_access_point = server_access_point;

    system_manager = new SystemManager(this, MAX_SYSTEM_MEMORY);
    database = system_manager.getDatabase();

    setup_channels(mc_address, mdb_address, mdr_address);
    setup_message_handler();

    executor = new ScheduledThreadPoolExecutor(10);

    send_UP_message();

    utilitarios.Notificacoes_Terminal.printAviso("peer " + id + " activo");
  }

  /**
   * main Peer
   *
   * @param args argumentos recebidos pela main
   */
  public static void main(String args[]) {
    if (!usage(args)) {
      return;
    }

    String protocol_version = args[0];
    int server_ID = Integer.parseInt(args[1]);

    // host/     ou   //host:port/
    String[] service_access_point = parse_RMI(args[2], true);
    if (service_access_point == null) {
      return;
    }

    String[] mc_address = args[3].split(":");
    String[] mdb_address = args[4].split(":");
    String[] mdr_address = args[5].split(":");

    // Os sistemas que usam o IPv6 por omissão precisam da flag seguinte
    System.setProperty("java.net.preferIPv4Stack", "true");

    construct(new Peer(
        protocol_version, server_ID, service_access_point, mc_address, mdb_address, mdr_address),
        args[1]);
  }

  /**
   * Constructor do peer
   *
   * @param obj1 Peer
   * @param arg argumentos para o construtor
   */
  private static void construct(Peer obj1, String arg) {
    try {
      Peer obj = obj1;
      MyRemote stub = (MyRemote) UnicastRemoteObject.exportObject(obj, 0);

      // Get own registry, to rebind to correct stub
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(arg, stub);

      utilitarios.Notificacoes_Terminal.printNotificao("Servidor "+arg+ " pronto");
    } catch (Exception e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Excepção no Servidor: " + e.toString());
    }
  }

  /**
   * Verifica se o número de argumentos está correcto
   *
   * @param args  arguments recebidos pela main
   * @return verdadeiro ou falso
   */
  private static boolean usage(String[] args) {
    if (!(args.length == 6)) {
      System.out.println(
          "Usage: java -classpath bin service.Peer"
              + " <protocol_version> <server_id> <service_access_point>"
              + " <mc:port> <mdb:port> <mdr:port>");
      return false;
    }
    return true;
  }

  /**
   * Cria o handler da mensagem
   */
  private void setup_message_handler() {
    peer_data = new PeerData();
    message_dispatcher = new ConcreteMessageDispatcher(this);
    new Thread(message_dispatcher).start();
  }

  /**
   * Cria os canais de comunicação e inicia as suas threads
   *
   * @param mc_address endereço do canal de controle
   * @param mdb_address endereço do canal do backup de dados
   * @param mdr_address endereço do canal do restore de dados
   */
  private void setup_channels(String[] mc_address, String[] mdb_address, String[] mdr_address) {
    Channel mc = new MChannel(this, mc_address[0], mc_address[1]);
    Channel mdb = new MDBChannel(this, mdb_address[0], mdb_address[1]);
    Channel mdr = new MDRChannel(this, mdr_address[0], mdr_address[1]);

    new Thread(mc).start();
    new Thread(mdb).start();
    new Thread(mdr).start();

    channels = new HashMap<>();
    channels.put(ChannelType.MC, mc);
    channels.put(ChannelType.MDB, mdb);
    channels.put(ChannelType.MDR, mdr);
  }

  /**
   * Enviar mensagem quando possível
   *
   * @param message mensagem a enviar
   * @param channel_type qual o canal
   * @param delay atraso
   * @param unit unidades de tempo em que o atraso está definido
   * @return
   */
  public Future send_delayed_message(
      Message message, ChannelType channel_type, long delay, TimeUnit unit) {
    return executor.schedule( () -> {   sender_message(message, channel_type);   },  delay,   unit);
  }

  /**
   * Envio de notificações pelo emissor
   *
   * @param message mensagem a enviar
   * @param channel_type qual o canal
   */
  private void sender_message(Message message, ChannelType channel_type) {
    try {
      send_message(message, channel_type);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError(
          "Erro ao enviar mensagem para o canal "
              + channel_type
              + " - "
              + message.getHeaderAsString());
    }
  }

  /**
   * Envio da notificação do emissor
   *
   * @param message mensagem a enviar
   * @param channel_type qual o canal
   * @throws IOException Exceção In/Out a ser lançado se acontecer um erro
   */
  public void send_message(Message message, ChannelType channel_type) throws IOException {
    utilitarios.Notificacoes_Terminal.printNotificao("Emissor: " + message.toString());
    channels.get(channel_type).sendMessage(message.getBytes());
  }

  /**
   * Vai buscar o canal
   *
   * @param channel_type tipo do canal
   * @return
   */
  public Channel get_channel(ChannelType channel_type) {
    return channels.get(channel_type);
  }

  /**
   * Faz backup de ficheiro
   *
   * @param pathname caminho do ficheiro
   * @param replication_degree grau de replicação
   */
  @Override
  public void backup(String pathname, int replication_degree) {
    executor.execute(new BackupInitiator(protocol_version, pathname, replication_degree, this));
  }

  /**
   * Restaura ficheiro
   *
   * @param pathname caminho do ficheiro
   */
  @Override
  public void restore(String pathname) {
    
    final Future handler;
    handler = executor.submit(new RestoreInitiator(protocol_version, pathname, this));

    executor.schedule(
        () -> {
          if (handler.cancel(true)) {
            utilitarios.Notificacoes_Terminal.printAviso("o restore_initiator foi terminado devido a falta de chunks.");
          }
        },
        20,
        TimeUnit.SECONDS);
  }

  /**
   * Apaga ficheiro
   *
   * @param pathname caminho do ficheiro
   */
  @Override
  public void delete(String pathname) {
    executor.execute(new DeleteInitiator(protocol_version, pathname, this));
  }

  /**
   * Recupera espaço em disco
   *
   * @param space espaço a ser recuperado
   */
  @Override
  public void reclaim(int space) {
    system_manager.getMemoryManager().setMaxMemory(space);
    executor.execute(new ReclaimInitiator(protocol_version, this));
  }

  /**
   * Devolve o estado do peer
   */
  @Override
  public void state() {
    executor.execute(new RetrieveStateInitiator(protocol_version, this));
  }

  /**
   * Vai buscar o id do peer
   *
   * @return id do peer
   */
  public int get_ID() {
    return id;
  }

  /**
   * Vai buscar o path completo onde guardar
   *
   * @param path directorio
   * @return o path completo onde guardae
   */
  public String get_path(String path) {
    String pathname;

    if (path.equals("backup")){//CH
      pathname = system_manager.getChunksPath();
    }else if (path.equals("restored")){
      pathname = system_manager.getRestoredPath();
    }else{
      pathname = "";
    }

    return pathname;
  }

  /**
   * Envia mensagem a dizer que está ativo. Fundamental para versão 1.3
   */
  private void send_UP_message() {
    if (isPeerCompatibleWithEnhancement(ENHANCEMENT_DELETE, this)) {
      String[] args = {
          get_version(), Integer.toString(get_ID()),
      };

      Message msg = new Message(Message.MessageType.UP, args);

      try {
        send_message(msg, Channel.ChannelType.MC);
      } catch (IOException e) {
        utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possivel enviar a mensagem para o canal multicast");
      }
    }
  }

  /**
   * Mete o datagrama no message_dispatcher
   *
   * @param data dados do datagrama
   * @param length tamanho do datagrama
   */
  public void add_msg_to_handler(byte[] data, int length) {
    message_dispatcher.pushMessage(data, length);
  }

  /**
   * Vai buscar um chunk para restaurar ficheiro ou para garantir o grau de replicação
   *
   * @param fileID identificação do ficheiro
   * @param chunkNo número do chunk
   * @return
   */
  public byte[] load_chunk(String fileID, int chunkNo) {
    return system_manager.loadChunk(fileID, chunkNo);
  }

  /**
   * Altera o estado de restore de um ficheiro
   * Enquanto está a ser restaurado o estado do ficheiro em relação ao restore é true
   *
   * @param flag true, quando se inicia o restauro e false quando se termina
   * @param fileID identificação do ficheiro
   */
  public void set_restoring(boolean flag, String fileID) {
    peer_data.setFlagRestored(flag, fileID);
  }

  /**
   * Verifica se processo de restore do ficheiro terminou
   *
   * @param pathName caminho do ficherio a restaurar
   * @param fileID identificação do ficheiro a restaurar
   * @return verdadeiro ou falso
   */
  public boolean has_restore_finished(String pathName, String fileID) {
    int numChunks = database.getNumChunksByFilePath(pathName);
    int chunksRestored = peer_data.getChunksRestoredSize(fileID);

    return numChunks == chunksRestored;
  }

  /**
   * Obtem dados do peer
   *
   * @return dados do peer
   */
  public PeerData get_peer_data() {
    return peer_data;
  }

  /**
   * Obtem a base de dados
   *
   * @return base de dados
   */
  public Database get_database() {
    return database;
  }

  /**
   * Obtem os dados do sistema (directorios, etc)
   *
   * @return dados do sistema
   */
  public SystemManager get_system_manager() {
    return system_manager;
  }

  /**
   * Obtem a versão do protocolo
   *
   * @return versão do protocolo
   */
  public String get_version() {
    return protocol_version;
  }
}
