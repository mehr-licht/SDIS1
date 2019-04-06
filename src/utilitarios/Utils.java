package utilitarios;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Utils {

  public static final String bi_CRLF = "" + (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A;
  private static final char[] hex_digits_usados_no_nome = "0123456789ABCDEF".toCharArray();

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
   * @param accessPoint ponto de acesso
   * @param Server se é server ou não
   * @return ponto de acesso do peer
   */
  public static String[] parse_RMI(String accessPoint, boolean Server) {
    Pattern rmiPattern;
    rmiPattern = get_pattern(Server);

    Matcher m = rmiPattern.matcher(accessPoint);
    String[] peer_ap = null;

    peer_ap = get_ap_strings_from_groups(peer_ap, m);

    return peer_ap;
  }

  /**
   * Obtem ponto de acesso do peer e verificação da sua validade
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
   * @param serviceAccessPoint pontos de acesso para o RMIservice
   * @return localização do registo RMI
   */
  public static Registry get_registry(String[] serviceAccessPoint) {
    Registry registry = null;
    // Bind the remote object's stub in the registry
    if (serviceAccessPoint[1] == null) {
      registry = locate_registry(serviceAccessPoint[0]);
    } else {
      registry = locate_registryAP(serviceAccessPoint);
    }
    return registry;
  }

  /**
   * Obtem o ponto de acesso do registo RMI ou por localHost ou por endereço
   * @param serviceAccessPoint pontos de acesso do RMIservice
   * @return registo RMI
   */
  private static Registry locate_registryAP(String[] serviceAccessPoint) {
    Registry registry = null;
    try {
      if (serviceAccessPoint[0] == "localhost") {
        registry = LocateRegistry.getRegistry(serviceAccessPoint[1]);
      } else {
        registry =
            LocateRegistry.getRegistry(
                serviceAccessPoint[0], Integer.parseInt(serviceAccessPoint[1]));
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return registry;
  }


  /**
   * Localiza o ponto de acesso do serviço
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
}
