package protocols.initiators;

import filesystem.Database;
import filesystem.FileInfo;
import canais.Canal;
import network.Message;
import service.Peer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * class DeleteInit
 */
public class DeleteInit implements Runnable {

  private String version;
  private String path;
  private Peer parentPeer;

  /**
   * Construtor do DeleteInit
   *
   * @param version
   * @param path
   * @param parent_peer
   */
  public DeleteInit(String version, String path, Peer parent_peer) {
    this.version = version;
    this.path = path;
    this.parentPeer = parent_peer;

    utilitarios.Notificacoes_Terminal.printAviso("A começar a apagar o ficheiro");
  }

  /**
   * lança o deleteInit
   */
  @Override
  public void run() {
    Database database = parentPeer.get_database();
    //Obtain info of the file from Database

    FileInfo fileInfo = database.getFileInfoByPath(path);
    if (fileInfo == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Ficheiro não existe -  A abortar");
      return;
    }

    database.addToFilesToDelete(fileInfo.getFileID());

    send_message_MC(fileInfo);
    delete_file();

    database.removeRestorableFile(fileInfo);
    utilitarios.Notificacoes_Terminal.printAviso("Acabei de apagar o ficheiro");
  }

  /**
   * Apaga o ficheiro no sistema de ficheiros
   */
  private void delete_file() {
    try {
      Files.delete(Paths.get(path));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível apagar o ficheiro");
    }
  }

  /**
   * Envia mensagem para o canal multicast
   *
   * @param file_info informações do ficheiro a apagar
   */
  private void send_message_MC(FileInfo file_info) {
    Message msg = make_delete(file_info);

    try {
      parentPeer.send_message(msg, Canal.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  /**
   * Cria o datagrama de delete
   *
   * @param file_info informações do ficheiro a apagar
   * @return datagrama de delete já construido
   */
  private Message make_delete(FileInfo file_info) {
    String[] args = {
        version,
        Integer.toString(parentPeer.get_ID()),
        file_info.getFileID()
    };

    return new Message(Message.Categoria_Mensagem.DELETE, args);
  }

}
