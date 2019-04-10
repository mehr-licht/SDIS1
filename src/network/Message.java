package network;

import static utilitarios.Utils.get_IPv4_address;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import utilitarios.Utils;

public class Message implements Serializable {

  public enum Categoria_Mensagem {
    PUTCHUNK,
    STORED,
    GETCHUNK,
    REMOVED,
    DELETE,
    ENH_GETCHUNK,
    DELETED,
    UP,
    CHUNK
  }

  private int numberArgs;
  //    Header
  private Categoria_Mensagem type;
  private String version;
  private int senderID;
  private String fileID;
  private int chunkNo;
  private int replicationDegree;
  //    Body
  private byte[] body;
  private String mTCPHost;
  private int mTCPPort;

  /**
   * Constructor 1/3 das mensagens recebidas
   *
   * @param data data do datagram
   * @param length tamanho do datagram
   */
  public Message(byte[] data, int length) throws Exception {

    String header = extractHeader(data);

    if (header.equals("") || !parse_identificar_protocolo(header)) {
      throw new Exception("Invalid message...Ignoring it!");
    }

    if (type == Categoria_Mensagem.PUTCHUNK || type == Categoria_Mensagem.CHUNK) {
      this.body = extractBody(data, header.length(), length);
    }
  }

  /** Constructor 2/3 para enviar mensagens sem corpo */
  public Message(Categoria_Mensagem type, String[] args) {
    System.out.println("construtor mmsg do restore 00");
    this.type = type;
    version = args[0];
    senderID = Integer.parseInt(args[1]);
    System.out.println("construtor mmsg do restore 01");
    if (type == Categoria_Mensagem.UP) {
      System.out.println("construtor mmsg do restore 01111");
      return;
    }
    System.out.println("construtor mmsg do restore 02");
    fileID = args[2];
    System.out.println("construtor mmsg do restore 03_1");
    System.out.println("construtor mmsg do restore 03: args " + args[3]);
    System.out.println("construtor mmsg do restore 03_2");

    System.out.println("construtor mmsg do restore 03_3");
    if (type != Categoria_Mensagem.DELETE && type != Categoria_Mensagem.DELETED) {
      System.out.println("construtor mmsg do restore 03: args " + args[0]);
      System.out.println("construtor mmsg do restore 03: args " + args[1]);
      System.out.println("construtor mmsg do restore 03: args " + args[2]);
      System.out.println("construtor mmsg do restore 03: args " + args[3]);
      if (type == Categoria_Mensagem.GETCHUNK || type == Categoria_Mensagem.ENH_GETCHUNK) {
          String tmp = (args[3]).substring(3);
        System.out.println("construtor mmsg do restore 03: tmp " + tmp);
        chunkNo = Integer.parseInt(tmp);
        System.out.println("construtor mmsg do restore 03: args[3] " + args[3]);
        System.out.println("construtor mmsg do restore 03: chunkNo " + tmp);
      }else{
          chunkNo = Integer.parseInt(args[3]);
      }
      System.out.println("construtor mmsg do restore 03333");
    }
    if (type == Categoria_Mensagem.PUTCHUNK) {
      System.out.println("construtor mmsg do restore 04");
      replicationDegree = Integer.parseInt(args[4]);
      System.out.println("construtor mmsg do restore 05");
    }
    System.out.println("construtor mmsg do restore 06");
    if (type == Categoria_Mensagem.ENH_GETCHUNK) {
      System.out.println("construtor mmsg do restore 07");
      mTCPPort = Integer.parseInt(args[4]);
      System.out.println("construtor mmsg do restore 08");
      mTCPHost = get_IPv4_address();
      System.out.println("construtor mmsg do restore 09");
    }
    System.out.println("construtor mmsg do restore 10");
  }

  /** Constructor pra enviar mensagens com corpo */
  public Message(Categoria_Mensagem type, String[] args, byte[] data) {
    this(type, args);
    body = data;
  }

  private String extractHeader(byte[] data) {
    ByteArrayInputStream stream = new ByteArrayInputStream(data);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String header = "";

    try {
      header = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return header;
  }

  private byte[] extractBody(byte[] data, int headerLength, int dataLength) {
    int length = dataLength;
    int readBytes = length - headerLength - 4;
    ByteArrayInputStream message = new ByteArrayInputStream(data, headerLength + 4, readBytes);
    byte[] bodyContent = new byte[readBytes];
    message.read(bodyContent, 0, readBytes);
    return bodyContent;
  }

  /** Define os tipos de mensagens recebidas segundo as categorias definidas */
  private boolean parse_identificar_protocolo(String header) {

    String headerCleaned =
        header.trim().replaceAll("\\s+", " "); // More than one space between fields
    String[] mensagem_em_array = headerCleaned.split("\\s+"); // Split by space the header elements

    switch (mensagem_em_array[0]) {
      case "PUTCHUNK":
        type = Categoria_Mensagem.PUTCHUNK;
        numberArgs = 6;
        break;
      case "STORED":
        type = Categoria_Mensagem.STORED;
        numberArgs = 5;
        break;
      case "GETCHUNK":
        System.out.println("recebeu GETCHUNK do restore");
        type = Categoria_Mensagem.GETCHUNK;
        numberArgs = 5;
        break;
      case "CHUNK":
        System.out.println("recebeu CHUNK do restore");
        type = Categoria_Mensagem.CHUNK;
        numberArgs = 5;
        break;
      case "DELETE":
        type = Categoria_Mensagem.DELETE;
        numberArgs = 4;
        break;
      case "DELETED":
        type = Categoria_Mensagem.DELETED;
        numberArgs = 4;
        break;
      case "REMOVED":
        type = Categoria_Mensagem.REMOVED;
        numberArgs = 5;
        break;
      case "ENH_GETCHUNK":
        type = Categoria_Mensagem.ENH_GETCHUNK;
        numberArgs = 6;
        break;
      case "UP":
        type = Categoria_Mensagem.UP;
        numberArgs = 3;
        break;
      default:
        return false;
    }

    if (mensagem_em_array.length != numberArgs) return false;

    // <Version>
    version = mensagem_em_array[1];

    // <Sender ID>
    senderID = Integer.parseInt(mensagem_em_array[2]);

    if (type == Categoria_Mensagem.UP) return true;

    // <FileId>
    fileID = mensagem_em_array[3];

    // <ChunkNumero>
    if (numberArgs > 4) chunkNo = Integer.parseInt(mensagem_em_array[4]);

    // <ReplicationDeg>
    if (type == Categoria_Mensagem.PUTCHUNK)
      replicationDegree = Integer.parseInt(mensagem_em_array[5]);

    if (type == Categoria_Mensagem.ENH_GETCHUNK) {
      String[] tcpAddress = mensagem_em_array[5].split(":");
      mTCPHost = tcpAddress[0];
      mTCPPort = Integer.parseInt(tcpAddress[1]);
    }
    return true;
  }

  public String getHeaderAsString() {
    String str;

    switch (type) {
      case PUTCHUNK:
        str =
            type
                + " "
                + version
                + " "
                + senderID
                + " "
                + fileID
                + " "
                + chunkNo
                + " "
                + replicationDegree
                + " "
                + Utils.bi_CRLF;
        break;
      case DELETE:
      case DELETED:
        str = type + " " + version + " " + senderID + " " + fileID + " " + Utils.bi_CRLF;
        break;
      case ENH_GETCHUNK:
        str =
            type
                + " "
                + version
                + " "
                + senderID
                + " "
                + fileID
                + " "
                + chunkNo
                + " "
                + mTCPHost
                + ":"
                + mTCPPort
                + " "
                + Utils.bi_CRLF;
        break;
      case UP:
        str = type + " " + version + " " + senderID + " " + Utils.bi_CRLF;
        break;
      default:
        str =
            type
                + " "
                + version
                + " "
                + senderID
                + " "
                + fileID
                + " "
                + chunkNo
                + " "
                + Utils.bi_CRLF;
        break;
    }

    return str;
  }

  public byte[] getBytes() {

    byte header[] = getHeaderAsString().getBytes();

    if (body != null) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        outputStream.write(header);
        outputStream.write(body);
      } catch (IOException e) {
        utilitarios.Notificacoes_Terminal.printMensagemError(
            "Couldn't create message byte[] to send!");
      }
      return outputStream.toByteArray();

    } else return header;
  }

  @Override
  public String toString() {
    String str;

    switch (type) {
      case PUTCHUNK:
        str = type + " " + version + " " + senderID + " " + fileID + " " + chunkNo;
        break;
      case DELETE:
      case DELETED:
        str = type + " " + version + " " + senderID + " " + fileID;
        break;
      case ENH_GETCHUNK:
        str =
            type + " " + version + " " + senderID + " " + fileID + " " + chunkNo + " " + mTCPHost
                + ":" + mTCPPort;
        break;
      case UP:
        str = type + " " + version + " " + senderID;
        break;
      default:
        str = type + " " + version + " " + senderID + " " + fileID + " " + chunkNo;
        break;
    }
    return str;
  }

  /** GETS METODOS */
  public String get_TCP_hostname() {
    return mTCPHost;
  }

  public int get_TCP_porta() {
    return mTCPPort;
  }

  public Categoria_Mensagem getType() {
    return type;
  }

  public int get_Chunk_Numero() {
    return chunkNo;
  }

  public int get_Sender_ID() {
    return senderID;
  }

  public int get_File_Replication_Degree() {
    return replicationDegree;
  }

  public byte[] get_Corpo_Mensagem() {
    return body;
  }

  public String get_version() {
    return version;
  }

  public String get_file_ID() {
    return fileID;
  }
}
