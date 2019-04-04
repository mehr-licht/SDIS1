package protocols.initiators;

import channels.Channel;
import filesystem.Database;
import filesystem.FileInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import network.Message;
import service.Peer;

public class DeleteInitiator implements Runnable {

  private String version;
  private String path;
  private Peer parentPeer;

  public DeleteInitiator(String version, String path, Peer parentPeer) {
    this.version = version;
    this.path = path;
    this.parentPeer = parentPeer;

    utilitarios.Notificacoes_Terminal.printAviso("Starting deleteInitiator!");
  }

  @Override
  public void run() {
    Database database = parentPeer.getDatabase();
    //Obtain info of the file from Database
    FileInfo fileInfo = database.getFileInfoByPath(path);
    if (fileInfo == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError("File didn't exist! Aborting Delete!");
      return;
    }

    database.addToFilesToDelete(fileInfo.getFileID());

    //Send Delete message to MC channel
    sendMessageToMC(fileInfo);
    deleteFile();

    //Delete file from database
    database.removeRestorableFile(fileInfo);
    utilitarios.Notificacoes_Terminal.printAviso("Finished deleteInitiator!");
  }

  private void deleteFile() {
    //Delete the file from fileSystem
    try {
      Files.delete(Paths.get(path));
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't delete a file!");
    }
  }

  private void sendMessageToMC(FileInfo fileInfo) {
    Message msg = makeDELETE(fileInfo);

    try {
      parentPeer.sendMessage(Channel.ChannelType.MC, msg);
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't send message to multicast channel!");
    }
  }

  private Message makeDELETE(FileInfo fileInfo) {
    String[] args = {
        version,
        Integer.toString(parentPeer.getID()),
        fileInfo.getFileID()
    };

    return new Message(Message.MessageType.DELETE, args);
  }

}
