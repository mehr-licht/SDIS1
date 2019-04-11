package canais;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import jdk.jshell.execution.Util;
import service.Peer;
import utilitarios.Utils;

/**
 * Por cada objecto vamos lançar 3 canais MC, MDB, MDR
 */
public abstract class Canal implements Runnable {


    private MulticastSocket socket;
    private InetAddress mcastAddr;
    private int mcastPort;
    private Peer parentPeer;

    /**
     * Criação dos canais de comunicação
     * Join multicast group
     * @param parentPeer peer criado dos 3 canais de comunicação
     * @param mcastAddr
     * @param mcastPort
     * */
    public Canal(Peer parentPeer, String mcastAddr, String mcastPort) {
        this.parentPeer = parentPeer;

        try {
            this.mcastAddr = InetAddress.getByName(mcastAddr);
            this.mcastPort = Integer.parseInt(mcastPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //Inicializacao do multicast
            socket = new MulticastSocket(this.mcastPort);
            socket.setTimeToLive(Utils.TTL);
            //join do group como na LAB
            socket.joinGroup(this.mcastAddr);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Implementa a classe Runnable. Assim que a thread é lançada faz o run()
     * Os 3 canais de comunicação ficam à escuta por mensages que serão reenviadas para o peer
     * */
    @Override
    public void run() {

        byte[] buffer_for_datagram = new byte[Utils.MAXIMO_TAMANHO_MESSAGE_CANAL];
        DatagramPacket packet = new DatagramPacket(buffer_for_datagram, buffer_for_datagram.length);

        // Loop waiting for messages
        while (true) {

            try { // blocking method
                this.socket.receive(packet);
                this.parentPeer.add_msg_to_handler(packet.getData(), packet.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    synchronized public void sendMessage(byte[] message) throws IOException {

        DatagramPacket packet = new DatagramPacket(message, message.length, mcastAddr, mcastPort);
        socket.send(packet);
    }

    public void close() {
        socket.close();
    }

    public enum ChannelType {
        MC, MDB, MDR
    }

}
