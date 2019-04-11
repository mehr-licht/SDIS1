package canais;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import service.Peer;
import utilitarios.Utils;

/**
 * Por cada objecto vamos lançar 3 canais MC, MDB, MDR
 */
public abstract class Canal implements Runnable {


    private MulticastSocket multicast_socket;
    private InetAddress multicast_address;
    private int multicast_port;
    private Peer parent_peer;

    /**
     * Criação dos canais de comunicação
     * Cria e Join multicast group
     * @param peer peer criado dos 3 canais de comunicação
     * @param mcastAddr
     * @param mcastPort
     */
    public Canal( String mcastAddr, String mcastPort, Peer peer) {
        this.parent_peer = peer;

        try {
            this.multicast_address = InetAddress.getByName(mcastAddr);
            this.multicast_port = Integer.parseInt(mcastPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //Inicializacao do multicast
            multicast_socket = new MulticastSocket(this.multicast_port);
            multicast_socket.setTimeToLive(Utils.TTL);
            //join do group como na LAB
            multicast_socket.joinGroup(this.multicast_address);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Implementa a classe Runnable. Assim que a thread é lançada faz o run()
     * Os 3 canais de comunicação ficam à escuta por mensages que serão reenviadas para o peer
     */
    @Override
    public void run() {

        byte[] buffer_for_datagram = new byte[Utils.MAXIMO_TAMANHO_MESSAGE_CANAL];
        DatagramPacket packet = new DatagramPacket(buffer_for_datagram, buffer_for_datagram.length);

        // Loop waiting for messages
        while (true) {

            try { // blocking method
                this.multicast_socket.receive(packet);
                this.parent_peer.add_msg_to_handler(packet.getData(), packet.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Tipos de canais
     * */
    public enum ChannelType {
        MC, MDB, MDR
    }


    synchronized public void sendMessage(byte[] message) throws IOException {

        DatagramPacket packet = new DatagramPacket(message, message.length, multicast_address, multicast_port);
        multicast_socket.send(packet);
    }


    /**
     * Fecha a multicast_socket do multicast
     * */
    public void close() {

        multicast_socket.close();
    }


    /**
     * Gets and Setters
     */
    public MulticastSocket getMulticast_socket() {
        return multicast_socket;
    }

    public void setMulticast_socket(MulticastSocket multicast_socket) {
        this.multicast_socket = multicast_socket;
    }

    public InetAddress getMulticast_address() {
        return multicast_address;
    }

    public void setMulticast_address(InetAddress multicast_address) {
        this.multicast_address = multicast_address;
    }

    public int getMulticast_port() {
        return multicast_port;
    }

    public void setMulticast_port(int multicast_port) {
        this.multicast_port = multicast_port;
    }

    public Peer getParent_peer() {
        return parent_peer;
    }

    public void setParent_peer(Peer parent_peer) {
        this.parent_peer = parent_peer;
    }
}
