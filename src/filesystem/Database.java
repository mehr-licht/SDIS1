package filesystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Database extends AuxMemAdmin {

    private static final long serialVersionUID = 1L;
    /**
     * @param string file id
     * @param FileInfo  informaçãod do ficheiro inclui chunks informacao
     */
    private ConcurrentMap<String, FileInfo> historic_files_backed_Up;
    /**
     * historic do path dos fiels
     */
    private ConcurrentMap<String, FileInfo> historic_files_Path;

    /**
     * History of the chunks in peer
     *
     * @param String file id
     * @param <Int,Chunk Info> chunk number, chunk information
     */
    private ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> historic_chunks_backed_up;

    /**
     * Registo das copias dos files nos peers
     *
     * @param String ficheiro ID
     * @param Set<Int> ids dos peers que contem o file
     */
    private ConcurrentMap<String, Set<Integer>> history_file_copys_on_peers;
    /**
     * Nosso Trash antes de apagar o ficheiro totalmente
     *
     * @param String file id
     * TODO Check PUTCHUNK -> remove fileID from Set
     */
    private Set<String> files_trash_deleted;


    Database(String savePath) {
        historic_files_backed_Up = new ConcurrentHashMap<>();
        historic_files_Path = new ConcurrentHashMap<>();
        historic_chunks_backed_up = new ConcurrentHashMap<>();

        history_file_copys_on_peers = new ConcurrentHashMap<>();
        files_trash_deleted = new HashSet<>();

        this.set_up(savePath);
    }

    /**
     * INSERT INTO database
     **/

    public boolean add_files_to_trash(String fileID) {
        return files_trash_deleted.add(fileID);
    }

    public void add_to_history_file_copys_in_peers(String fileID, int senderID) {
        try {
            history_file_copys_on_peers.putIfAbsent(fileID, new ConcurrentSkipListSet<>());
            history_file_copys_on_peers.get(fileID).add(senderID);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void add_restorable_file(FileInfo fileInfo) {

        try {
            historic_files_backed_Up.put(fileInfo.getFileID(), fileInfo);
            historic_files_Path.put(fileInfo.getPath(), fileInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void add_to_history_chunks__backed_up(ChunkInfo chunkInfo, Integer parentPeerID) {
        chunkInfo.addMirror(parentPeerID);

        String fileID = chunkInfo.get_file_ID();
        int chunkNo = chunkInfo.get_chunk_No();
        try {
            ConcurrentMap<Integer, ChunkInfo> fileChunks;
            fileChunks = historic_chunks_backed_up.getOrDefault(fileID, new ConcurrentHashMap<>());
            fileChunks.putIfAbsent(chunkNo, chunkInfo);

            historic_chunks_backed_up.putIfAbsent(fileID, fileChunks);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public Boolean add_to_history_chunks_backed_upMirror(String fileID, int chunkNo, int peerID) {
        boolean ret;
        try {
            ret = historic_chunks_backed_up.get(fileID).get(chunkNo).addMirror(peerID);
        } catch (NullPointerException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("line 95 add_to_history_chunks_backed_upMirror " + e.getMessage());
            return null;
        }

        return ret;
    }


    /**
     * DELETE FROM database
     * */
    /**
     * Remove o file do lixo - é como se fosse o TRASH do ubuntu
     */
    public boolean remove_file_from_trash(String fileID) {

        return files_trash_deleted.remove(fileID);
    }

    //Delete file from database
    public void final_remove_file_from_database(FileInfo fileInfo) {
        try {
            historic_files_backed_Up.remove(fileInfo.getFileID());
            historic_files_Path.remove(fileInfo.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void remove_from_history_file_copys_on_peers(String fileID, int senderID) {
        Set<Integer> peers = history_file_copys_on_peers.get(fileID);
        try {
            if (peers != null) {
                peers.remove(senderID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void remove_from_history_chunks_backed_up(String fileID, int chunkNo) {

        if (!historic_chunks_backed_up.containsKey(fileID)) {
            return;
        }

        try {
            historic_chunks_backed_up.get(fileID).remove(chunkNo);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public Map<Integer, ChunkInfo> remove_from_history_chunks_backed_up_By_File_ID(String fileID) {
        if (!historic_chunks_backed_up.containsKey(fileID)) {
            return null;
        }

        return historic_chunks_backed_up.remove(fileID);
    }

    /**
     * Removes the given peerID as a mirror of the specified chunk.
     *
     * @param fileID  The chunk's fileID
     * @param chunkNo The chunk's id number
     * @param peerID  The peerID to be removed
     * @return True if the peerID was a mirror, False if it wasn't, null if ChunkData was not found
     */
    public Boolean remove_chunk_mirror(String fileID, int chunkNo, int peerID) {
        boolean ret;
        try {
            ret = historic_chunks_backed_up.get(fileID).get(chunkNo).removeMirror(peerID);
        } catch (NullPointerException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("(remove_chunk_mirror) ChunkData not found: " + e.getMessage());
            return null;
        }

        return ret;
    }


    public void remove_from_historiy_files_path(String path) {
        try {
            final_remove_file_from_database(historic_files_Path.get(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * CHECKS IF EXISTS IN database
     */
    public boolean has_in_history_files_BackedUp_By_File_Id(String fileID) {
        return historic_files_backed_Up.containsKey(fileID);
    }

    public boolean has_in_history_files_path_By_file_path(String path) {
        return historic_files_Path.containsKey(path);
    }


    // historic_chunks_backed_up
    public boolean hasChunk(String fileID, int chunkNo) {

        Map<Integer, ChunkInfo> fileChunks = historic_chunks_backed_up.get(fileID);

        return fileChunks != null && fileChunks.containsKey(chunkNo);
    }


    public boolean has_chunks(String fileID) {

        return historic_chunks_backed_up.containsKey(fileID);
    }


    /**
     * GETS AND SETTERS
     */
    public String getFileInfoByFileID(String fileID) {

        return historic_files_backed_Up.get(fileID).getPath();
    }


    public Set<String> getFiles_to_delete(int senderID) {
        Set<String> files = new ConcurrentSkipListSet<>();
        try {
            for (Map.Entry<String, Set<Integer>> fileMirrorEntry : history_file_copys_on_peers.entrySet()) {
                aux_get_files_to_delete(senderID, files, fileMirrorEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    private void aux_get_files_to_delete(int senderID, Set<String> files, Map.Entry<String, Set<Integer>> fileMirrorEntry) {
        try {
            for (Integer mirrorID : fileMirrorEntry.getValue()) {
                String fileID = fileMirrorEntry.getKey();
                if (mirrorID == senderID && files_trash_deleted.contains(fileID)) {
                    files.add(fileID);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public ConcurrentMap<String, ConcurrentMap<Integer, ChunkInfo>> getHistoric_chunks_backed_up() {
        return historic_chunks_backed_up;
    }

    public Collection<FileInfo> getHistoric_files_backed_Up() {

        return historic_files_backed_Up.values();
    }

    public int getNumChunksByFilePath(String path) {

        return historic_files_Path.get(path).getNumChunks();
    }


    public Integer getChunkPerceivedReplication(String fileID, int chunkNo) {

        int ret;
        try {
            ret = historic_chunks_backed_up.get(fileID).get(chunkNo).getNumMirrors();
        } catch (NullPointerException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("getChunkPerceivedReplication " + e.getMessage());
            return null;
        }

        return ret;
    }

    public FileInfo getFileInfoByPath(String pathName) {

        return historic_files_Path.get(pathName);
    }

    public ChunkInfo getChunkInfo(String fileID, int chunkNo) {
        Map<Integer, ChunkInfo> fileChunks = historic_chunks_backed_up.get(fileID);

        return fileChunks != null ? fileChunks.get(chunkNo) : null;
    }

    /**
     * Getter for any one ChunkData to be removed for reclaiming memory space.
     *
     * @return The chosen ChunkData.
     */
    public ChunkInfo getChunkForRemoval() {
        // Currently, chunk for removal is most backed-up chunk
        return getMostBackedUpChunk();
    }

    /**
     * Calcula o chuck que tem mais replicas
     * */
    private ChunkInfo getMostBackedUpChunk() {
        ChunkInfo mostBackedUpChunk = null;
        int maxMirroring = -1;
        try {
            for (ConcurrentMap.Entry<String, ConcurrentMap<Integer, ChunkInfo>> fileEntry : historic_chunks_backed_up
                    .entrySet()) {
                for (ConcurrentMap.Entry<Integer, ChunkInfo> chunkEntry : fileEntry.getValue().entrySet()) {
                    int numMirrors = chunkEntry.getValue().getNumMirrors();
                    if (numMirrors > maxMirroring) {
                        maxMirroring = numMirrors;
                        mostBackedUpChunk = chunkEntry.getValue();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mostBackedUpChunk;
    }


}
