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
  private int sender_ID;
  private String file_ID;
  private int chunkNo;
  private int replication_degree;
  //    Body
  private byte[] body;
  private String m_TCP_host;
  private int m_TCP_port;

  /**
   * Constructor 1/3 das mensagens recebidas
   *
   * @param data data do datagrama
   * @param length tamanho do datagrama
   */
  public Message(byte[] data, int length) throws Exception {

    String header = extractHeader(data);

    if (header.equals("") || !parse_identificar_protocolo(header)) {
      throw new Exception("Datagrama Inválido... -> ignorado");
    }

    if (type == Categoria_Mensagem.PUTCHUNK || type == Categoria_Mensagem.CHUNK) {
      this.body = extract_body(data, header.length(), length);
    }
  }

  /**
   * Constructor 2/3 para enviar mensagens sem corpo
   *
   * @param type
   * @param args
   */
  public Message(Categoria_Mensagem type, String[] args) {
    this.type = type;
    version = args[0];
    sender_ID = Integer.parseInt(args[1]);
    if (type == Categoria_Mensagem.UP) {
      return;
    }
    file_ID = args[2];
    if (type != Categoria_Mensagem.DELETE && type != Categoria_Mensagem.DELETED) {
      if (type == Categoria_Mensagem.GETCHUNK
          || type == Categoria_Mensagem.ENH_GETCHUNK
          || type == Categoria_Mensagem.REMOVED) {
        String tmp = (args[3]).substring(3);
        chunkNo = Integer.parseInt(tmp);

      } else {
        chunkNo = Integer.parseInt(args[3]);
      }
    }

    if (type == Categoria_Mensagem.PUTCHUNK) {
      replication_degree = Integer.parseInt(args[4]);
    }
    if (type == Categoria_Mensagem.ENH_GETCHUNK) {
      m_TCP_port = Integer.parseInt(args[4]);
      m_TCP_host = get_IPv4_address();
    }

    System.out.println("\nmensagem:"+type+"\nFileID:"+ file_ID +"\nchunkNo:"+chunkNo+"\n");
  }


  /**
   *  Constructor pra enviar mensagens com corpo
   *
   * @param type
   * @param args
   * @param data
   */
  public Message(Categoria_Mensagem type, String[] args, byte[] data) {
    this(type, args);
    body = data;
  }

  /**
   * Extrai o cabeçalho da mensagem de dentro
   *
   * @param data
   * @return
   */
  private String extractHeader(byte[] data) {
    ByteArrayInputStream stream = new ByteArrayInputStream(data);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String header = read_header(reader);

    return header;
  }

  /**
   * Lê o cabeçalho
   *
   * @param reader um bufferedReader
   * @return cabeçalho
   */
  private String read_header(BufferedReader reader) {
    String header = "";
    try {
      header = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return header;
  }

  /**
   * Extrai o corpo da mensagem de dentro do datagrama
   *
   * @param data dados
   * @param header_length tamanho do cabeçalho
   * @param data_length tamanho dos dados
   * @return conteúdo do corpo da mensagem
   */
  private byte[] extract_body(byte[] data, int header_length, int data_length) {
    int length = data_length;
    int readBytes = length - header_length - 4;
    ByteArrayInputStream message = new ByteArrayInputStream(data, header_length + 4, readBytes);
    byte[] bodyContent = new byte[readBytes];
    message.read(bodyContent, 0, readBytes);
    return bodyContent;
  }


  /**
   * Define os tipos de mensagens recebidas segundo as categorias definidas
   *
   * @param header cabeçalho do datagrama
   * @return verdadeiro se efetuado com sucesso ou falso no caso contrário
   */
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
        type = Categoria_Mensagem.GETCHUNK;
        numberArgs = 5;
        break;
      case "CHUNK":
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
    sender_ID = Integer.parseInt(mensagem_em_array[2]);

    if (type == Categoria_Mensagem.UP) return true;

    // <FileId>
    file_ID = mensagem_em_array[3];

    // <ChunkNumero>
    if (numberArgs > 4) chunkNo = Integer.parseInt(mensagem_em_array[4]);

    // <ReplicationDeg>
    if (type == Categoria_Mensagem.PUTCHUNK)
      replication_degree = Integer.parseInt(mensagem_em_array[5]);

    if (type == Categoria_Mensagem.ENH_GETCHUNK) {
      String[] tcpAddress = mensagem_em_array[5].split(":");
      m_TCP_host = tcpAddress[0];
      m_TCP_port = Integer.parseInt(tcpAddress[1]);
    }
    return true;
  }

  /**
   * Obtem cabeçalho do datagram como string
   *
   * @return cabeçalho do datagrama
   */
  public String get_header_as_string() {
    String str;

    switch (type) {
      case PUTCHUNK:
        str =
            type
                + " "
                + version
                + " "
                + sender_ID
                + " "
                + file_ID
                + " "
                + chunkNo
                + " "
                + replication_degree
                + " "
                + Utils.bi_CRLF;
        break;
      case DELETE:
      case DELETED:
        str = type + " " + version + " " + sender_ID + " " + file_ID + " " + Utils.bi_CRLF;
        break;
      case ENH_GETCHUNK:
        str =
            type
                + " "
                + version
                + " "
                + sender_ID
                + " "
                + file_ID
                + " "
                + chunkNo
                + " "
                + m_TCP_host
                + ":"
                + m_TCP_port
                + " "
                + Utils.bi_CRLF;
        break;
      case UP:
        str = type + " " + version + " " + sender_ID + " " + Utils.bi_CRLF;
        break;
      default:
        str =
            type
                + " "
                + version
                + " "
                + sender_ID
                + " "
                + file_ID
                + " "
                + chunkNo
                + " "
                + Utils.bi_CRLF;
        break;
    }

    return str;
  }

  /**
   * obtem datagrama
   *
   * @return
   */
  public byte[] get_bytes() {

    byte header[] = get_header_as_string().getBytes();

    return write_oos(header);
  }

  /**
   * cria datagrama em array de bytes
   *
   * @param header cabeçalho da mensagem
   * @return datagrama
   */
  private byte[] write_oos(byte[] header) {
    if (body != null) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        outputStream.write(header);
        outputStream.write(body);
      } catch (IOException e) {
        utilitarios.Notificacoes_Terminal.printMensagemError(
            "Não foi possível criar a mensagem de array de bytes para enviar");
      }
      return outputStream.toByteArray();

    } else return header;
  }

  /**
   * Faz uma string de acordo com o especificado para os headers
   *
   * @return string de acordo com o especificado para os headers
   */
  @Override
  public String toString() {
    String str;

    switch (type) {
      case PUTCHUNK:
        str = type + " " + version + " " + sender_ID + " " + file_ID + " " + chunkNo;
        break;
      case DELETE:
      case DELETED:
        str = type + " " + version + " " + sender_ID + " " + file_ID;
        break;
      case ENH_GETCHUNK:
        str =
            type + " " + version + " " + sender_ID + " " + file_ID + " " + chunkNo + " " + m_TCP_host
                + ":" + m_TCP_port;
        break;
      case UP:
        str = type + " " + version + " " + sender_ID;
        break;
      default:
        str = type + " " + version + " " + sender_ID + " " + file_ID + " " + chunkNo;
        break;
    }
    return str;
  }

  /** GETS METODOS */

  /**
   * Obtem o nome do servidor TCP
   *
   * @return nome do servidor TCP
   */
  public String get_TCP_hostname() {
    return m_TCP_host;
  }

  /**
   * Obtem o porto TCP
   *
   * @return porto TCP
   */
  public int get_TCP_porta() {
    return m_TCP_port;
  }

  /**
   * Obtem tipo da mensagem
   *
   * @return tipo da mensagem
   */
  public Categoria_Mensagem get_type() {
    return type;
  }

  /**
   * Obtem numero do chunk
   *
   * @return numero do chunk
   */
  public int get_chunk_numero() {
    return chunkNo;
  }

  /**
   * Obtem a identificação do emissor
   *
   * @return identificação do emissor
   */
  public int get_sender_ID() {
    return sender_ID;
  }

  /**
   * Obtem o grau de replicação do ficheiro
   *
   * @return grau de replicação do ficheiro
   */
  public int get_file_replication_degree() {
    return replication_degree;
  }

  /**
   * Obtem o corpo da mensagem
   *
   * @return corpo da mensagem
   */
  public byte[] get_Corpo_Mensagem() {
    return body;
  }

  /**
   * obtem a versão do pedido
   *
   * @return versão do pedido
   */
  public String get_version() {
    return version;
  }

  /**
   * identificação do ficheiro
   *
   * @return identificação do ficheiro
   */
  public String get_file_ID() {
    return file_ID;
  }
}
