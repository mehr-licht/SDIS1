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

public class Utils {

  //public static final String my_CRLF = "" + (char) 0x0D + (char) 0x0A;
  public static final String bi_CRLF = "" + (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A;
  private static final char[] digits = "0123456789ABCDEF".toCharArray();
  //public static final char[] high_prime = "2147483647".toCharArray();
  /**
   * Converte Bytes numa string Hexadecimal
   * @param bytes array a ser transformado num string hexadecimal
   * @return string Hexadecimal convertida desde o array de Bytes
   */
  public static String bytes_to_hex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = digits[v >>> 4];
      hexChars[j * 2 + 1] = digits[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Função de Hash (digest)
   * @param msg Mensagem a ser hasheada
   * @return Mensagem já hasheada
   */
  public static String hash(String msg) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      Notificacoes_Terminal.printMensagemError("Algoritmo de hash não encontrado: " + e.getMessage());
      return null;
    }
    return get_string(digest, msg);
  }

  /**
   * Obtem hashado
   * @param digest
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
   * @return o enderço do localhost (IPv4)
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
   *
   * @param accessPoint
   * @param Server
   * @return
   */
  public static String[] parse_RMI(String accessPoint, boolean Server) {
    Pattern rmiPattern;
    rmiPattern = get_pattern(Server);

    Matcher m = rmiPattern.matcher(accessPoint);
    String[] peer_ap = null;

    peer_ap = get_strings(peer_ap, m);

    return peer_ap;
  }

  /**
   *
   * @param peer_ap
   * @param m
   * @return
   */
  private static String[] get_strings(String[] peer_ap, Matcher m) {
    if (m.find()) {
      peer_ap = new String[] {m.group(1), m.group(2), m.group(3)};
    } else {
      Notificacoes_Terminal.printMensagemError("Ponto de Acesso Inválido");
    }
    return peer_ap;
  }

  /**
   *
   * @param Server
   * @return
   */
  private static Pattern get_pattern(boolean Server) {
    Pattern rmiPattern;
   // String aux = Server?"?":"";
    //String pattern="//([\\w.]+)(?::(\\d+))?/(\\w+)"+aux;

   // rmiPattern = Pattern.compile(pattern);

    if (Server) {
      rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)?");
    } else {
      rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)");
    }
    return rmiPattern;
  }

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

  private static Registry locate_registryAP(String[] serviceAccessPoint) {
    Registry registry=null;
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

  private static Registry locate_registry(String string) {
    Registry registry=null;
    try{
    if (string == "localhost") {

      registry = LocateRegistry.getRegistry();

    } else {
      registry = LocateRegistry.getRegistry(string);
    }
  } catch (RemoteException e) {
    e.printStackTrace();
  }
    return registry;
  }
}
