package protocols;

import network.Message;
import service.Peer;

public class ProtocolSettings {

    /**
     * Maximum delay for random wait-time for STORED response.
     */
    public static final int MAX_DELAY = 400;

    public static final int MAX_SYSTEM_MEMORY = (int) Math.pow(10, 6) * 8; // 8MB

    public static final int PUTCHUNK_RETRIES = 5;

    public static final int MAX_CHUNK_SIZE = 64000;

    public static final int MAX_REPLICATION_DEGREE = 9;

    public static final int MAX_NUM_CHUNKS = 1000000;

    public static final int TCPSERVER_PORT = 4444;

    public static final String ENHANCEMENT_BACKUP = "1.1";

    public static final String ENHANCEMENT_RESTORE = "1.2";

    public static final String ENHANCEMENT_DELETE = "1.3";

    public static final String ENHANCEMENT_ALL = "2.0";

    public static boolean isCompatibleWithEnhancement(String enhancedVersion, Message request, Peer peer) {
        return ((request.getVersion().equals(enhancedVersion) || request.getVersion().equals(ENHANCEMENT_ALL)) &&
                (peer.getVersion().equals(enhancedVersion) || peer.getVersion().equals(ENHANCEMENT_ALL)));

    }

    public static boolean isPeerCompatibleWithEnhancement(String enhancedVersion, Peer peer) {
        return (peer.getVersion().equals(enhancedVersion) || peer.getVersion().equals(ENHANCEMENT_ALL));
    }

    public static boolean isMessageCompatibleWithEnhancement(String enhancedVersion, Message msg) {
        return (msg.getVersion().equals(enhancedVersion) || msg.getVersion().equals(ENHANCEMENT_ALL));
    }

}
