package protocols.initiators.helpers;

import filesystem.ChunkData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import network.Message;
import protocols.Peer_Info;
import service.Peer;

public class TCPClientHandler implements Runnable {

  private Peer parentPeer;
  private Socket clientSocket;

  public TCPClientHandler(Peer parentPeer, Socket clientSocket) {
    this.parentPeer = parentPeer;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    //Receive the CHUNK
    Message msg = null;

    try {
      ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
      msg = (Message) ois.readObject();
      ois.close();
      clientSocket.close();
    } catch (IOException | ClassNotFoundException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Couldn't read the message through TCPServer!");
    }

    utilitarios.Notificacoes_Terminal.printAviso("R TCP: " + msg.toString());

    if (msg == null) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Invalid CHUNK from TCP. Aborting!");
      return;
    }

    //Handle the CHUNK
    Peer_Info peerData = parentPeer.get_peer_data();

    if (!peerData.get_restored_flag(msg.getFileID())) {
      utilitarios.Notificacoes_Terminal.printNotificao("Discarded ChunkData!");
      return;
    }

    peerData.get_restored_chunk_data(new ChunkData(msg.getFileID(), msg.getChunkNo(), msg.getBody()));
  }
}
