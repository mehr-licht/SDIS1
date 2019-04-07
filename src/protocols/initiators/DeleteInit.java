package protocols.initiators;

import channels.Channel;
import filesystem.Database;
import filesystem.FileInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import network.Message;
import service.Peer;

public class DeleteInit implements Runnable {

  private String version;
  private String path;
  private Peer parentPeer;

  public DeleteInit(String version, String path, Peer parentPeer) {
    this.version = version;
    this.path = path;
    this.parentPeer = parentPeer;

    utilitarios.Notificacoes_Terminal.printAviso("A começar a apagar o ficheiro");
  }

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

    //Send Delete message to MC channel
    sendMessageToMC(fileInfo);
    deleteFile();

    //Delete file from database
    database.removeRestorableFile(fileInfo);
    utilitarios.Notificacoes_Terminal.printAviso("Acabei de apagar o ficheiro");
  }

  private void deleteFile() {
    //Delete the file from fileSystem
    try {
      Files.delete(Paths.get(path));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível apagar o ficheiro");
    }
  }

  private void sendMessageToMC(FileInfo fileInfo) {
    Message msg = makeDELETE(fileInfo);

    try {
      parentPeer.send_message(msg, Channel.ChannelType.MC);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível enviar para o canal multicast");
    }
  }

  private Message makeDELETE(FileInfo fileInfo) {
    String[] args = {
        version,
        Integer.toString(parentPeer.get_ID()),
        fileInfo.getFileID()
    };

    return new Message(Message.MessageType.DELETE, args);
  }

}
