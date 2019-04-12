package utilitarios;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.Message;
import service.Peer;

public class Utils {

  public static final String bi_CRLF = "" + (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A;
  private static final char[] hex_digits_usados_no_nome = "0123456789ABCDEF".toCharArray();


  /**  Set the default time-to-live for multicast packets sent out on this
   * MulticastSocket in order to control the scope of the multicasts.
   */
  public static final int TTL = 1;
   /**Tamanho maximo da mensagem envia no canal*/
  public static final int MAXIMO_TAMANHO_MESSAGE_CANAL = 65000;

  /** Número de tentativas de envio de PUTCHUNK (enunciado) * */
  public static final int PUTCHUNK_RETRIES = 5;

  /** Tamanho máximo de cada chunk (64KB enunciado) * */
  public static final int MAX_CHUNK_SIZE = 64000;

  /** versão do melhoramento pedido para o sub-protocolo BACKUP * */
  public static final String BACKUP_ENH = "1.1";

  /** versão do melhoramento pedido para o sub-protocolo RESTORE * */
  public static final String RESTORE_ENH = "1.2";

  /** versão do melhoramento pedido para o sub-protocolo DELETE * */
  public static final String DELETE_ENH = "1.3";

  /** versão com todos os melhoramentos pedidos para todos os sub-protocolos * */
  public static final String ENHANCEMENTS = "2.0";

  /** Atraso máximo para o tempo de espera aleatório da resposta STORED (enunciado 400ms)*/
  public static final int MAX_DELAY = 400;

  /** Memória máxima do sistema (8MB) * */
  public static final int MAX_SYSTEM_MEMORY = (int) Math.pow(10, 6) * 8;

  /** Grau de replicação máximo * */
  public static final int MAX_REPLICATION_DEGREE = 9;

  /** Número máximo de chunks * */
  public static final int MAX_NUM_CHUNKS = (int) Math.pow(10, 6);

  /** Porto do servidor TCP * */
  public static final int TCPSERVER_PORT = 4444;

  /** Numero de threads a manter na pool * */
  public static final int PEER_CORE_POOL_SIZE = 10;
  public static final int MSG_CORE_POOL_SIZE = 5;

  /**
   * Conversor de Bytes para Hexadecimal
   *
   * @param bytes array a ser transformado num string hexadecimal
   * @return string Hexadecimal convertida desde o array de Bytes
   */
  public static String bytes_to_hex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hex_digits_usados_no_nome[v >>> 4];
      hexChars[j * 2 + 1] = hex_digits_usados_no_nome[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Função de Hash
   *
   * @param unhashed unhashed filename
   * @return Mensagem já hasheada
   */
  public static String hash(String unhashed) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      Notificacoes_Terminal.printMensagemError(
          "Algoritmo de hash não encontrado: " + e.getMessage());
      return null;
    }
    return get_string(digest, unhashed);
  }

  /**
   * Obtem representação hexadecimal do hash
   *
   * @param digest objecto de SHA-256
   * @param msg
   * @return
   */
  private static String get_string(MessageDigest digest, String msg) {
    byte[] hash = digest.digest(msg.getBytes(StandardCharsets.UTF_8));
    String hashedID = bytes_to_hex(hash);
    return hashedID;
  }

  /**
   * Vai buscar o endereço do localhost (IPv4)
   *
   * @return o endereço do localhost (IPv4)
   */
  public static String get_IPv4_address() {
    String IP = null;

    try {
      IP = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return IP;
  }

  /**
   * Obtem ponto de acesso do peer. Verificacao do nome do RMI
   *
   * @param access_point ponto de acesso
   * @param Server se é server ou não
   * @return ponto de acesso do peer
   */
  public static String[] parse_RMI(String access_point, boolean Server) {
    Pattern rmi_pattern;
    rmi_pattern = get_pattern(Server);

    Matcher m = rmi_pattern.matcher(access_point);
    String[] peer_ap = null;

    peer_ap = get_ap_strings_from_groups(peer_ap, m);

    return peer_ap;
  }

  /**
   * Obtem ponto de acesso do peer e verificação da sua validade
   *
   * @param peer_ap ponto de acesso
   * @param m padrao a encontrar
   * @return ponto de acesso do peer
   */
  private static String[] get_ap_strings_from_groups(String[] peer_ap, Matcher m) {
    if (m.find()) {
      peer_ap = new String[] {m.group(1), m.group(2), m.group(3)};
    } else {
      Notificacoes_Terminal.printMensagemError("Ponto de Acesso Inválido");
    }
    return peer_ap;
  }

  /**
   * Faz um padrão para o identificador do serviço RMI
   *
   * @param Server se é server ou não
   * @return indentificador do serviço RMI
   */
  private static Pattern get_pattern(boolean Server) {
    Pattern identifier;
    String aux = Server ? "?" : "";
    String pattern = "//([\\w.]+)(?::(\\d+))?/(\\w+)" + aux;
    identifier = Pattern.compile(pattern);
    return identifier;
  }

  /**
   * Obtem o registo RMI
   *
   * @param service_access_point pontos de acesso para o RMIservice
   * @return localização do registo RMI
   */
  public static Registry get_registry(String[] service_access_point) {
    Registry registry = null;
    // Bind the remote object's stub in the registry
    if (service_access_point[1] == null) {
      registry = locate_registry(service_access_point[0]);
    } else {
      registry = locate_registryAP(service_access_point);
    }
    return registry;
  }

  /**
   * Obtem o ponto de acesso do registo RMI ou por localHost ou por endereço
   *
   * @param service_access_point pontos de acesso do RMIservice
   * @return registo RMI
   */
  private static Registry locate_registryAP(String[] service_access_point) {
    Registry registry = null;
    try {
      if (service_access_point[0] == "localhost") {
        registry = LocateRegistry.getRegistry(service_access_point[1]);
      } else {
        registry =
            LocateRegistry.getRegistry(
                service_access_point[0], Integer.parseInt(service_access_point[1]));
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return registry;
  }

  /**
   * Localiza o ponto de acesso do serviço
   *
   * @param access_point ponto de acesso do serviço
   * @return registo RMI
   */
  private static Registry locate_registry(String access_point) {
    Registry registry = null;
    try {
      if (access_point == "localhost") {
        registry = LocateRegistry.getRegistry();
      } else {
        registry = LocateRegistry.getRegistry(access_point);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return registry;
  }

  /**
   * Verifica se a versão do pedido é compativel com a versão do peer
   *
   * @param peer
   * @param request
   * @param enhanced_version
   * @return
   */
  public static boolean enhancements_compatible(
      Peer peer, Message request, String enhanced_version) {

      return ((request.get_version().equals(enhanced_version)
          || request.get_version().equals(ENHANCEMENTS))
          && (peer.get_version().equals(enhanced_version)
          || peer.get_version().equals(ENHANCEMENTS)));

  }

  public static boolean isPeerCompatibleWithEnhancement(String enhancedVersion, Peer peer) {
    return (peer.get_version().equals(enhancedVersion) || peer.get_version().equals(ENHANCEMENTS));
  }

  /**
   * Verifica se o peer é compativel com todos os melhoramentos
   *
   * @param peer um peer
   * @param enhanced_version versão melhorada
   * @return verdadeiro ou falso
   */
  public static boolean enhancement_compatible_peer(Peer peer, String enhanced_version) {
    if (peer.get_version().equals(ENHANCEMENTS) || peer.get_version().equals(enhanced_version)) {
      return true;
    } else {
      return false;
    }

  }

  /**
   * Verifica se a mensagem é compativel com todos os melhoramentos
   *
   * @param msg mensagem
   * @param enhanced_version versão melhorada
   * @return verdadeiro ou falso
   */
  public static boolean enhancement_compatible_msg(Message msg, String enhanced_version) {
    if (msg.get_version().equals(ENHANCEMENTS) || msg.get_version().equals(enhanced_version)) {
      return true;
    } else {
      return false;
    }
  }
}
