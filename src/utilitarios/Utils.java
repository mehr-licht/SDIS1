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

  public static final String CRLF = "" + (char) 0x0D + (char) 0x0A;

  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String hash(String msg) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      Notificacoes_Terminal.printMensagemError("Hash algorithm not found: " + e.getMessage());
      return null;
    }

    return getString(msg, digest);
  }

  private static String getString(String msg, MessageDigest digest) {
    byte[] hash = digest.digest(msg.getBytes(StandardCharsets.UTF_8));
    String hashedID = bytesToHex(hash);
    return hashedID;
  }

  public static String getIPV4Address() {
    String IP = null;

    try {
      IP = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    return IP;
  }

  public static String[] parseRMI(boolean Server, String accessPoint) {
    Pattern rmiPattern;
    rmiPattern = getPattern(Server);

    Matcher m = rmiPattern.matcher(accessPoint);
    String[] peer_ap = null;

    peer_ap = getStrings(m, peer_ap);

    return peer_ap;
  }

  private static String[] getStrings(Matcher m, String[] peer_ap) {
    if (m.find()) {
      peer_ap = new String[] {m.group(1), m.group(2), m.group(3)};
    } else {
      Notificacoes_Terminal.printMensagemError("Invalid Access Point!");
    }
    return peer_ap;
  }

  private static Pattern getPattern(boolean Server) {
    Pattern rmiPattern;
    if (Server) {
      rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)?");
    } else {
      rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)");
    }
    return rmiPattern;
  }

  public static Registry getRegistry(String[] serviceAccessPoint) {
    Registry registry = null;
    // Bind the remote object's stub in the registry


      if (serviceAccessPoint[1] == null) {
        registry = getRegistry(serviceAccessPoint[0]);
      } else {
        registry = getRegistryAP(serviceAccessPoint);
      }


    return registry;
  }

  private static Registry getRegistryAP(String[] serviceAccessPoint) {
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

  private static Registry getRegistry(String string) {
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
