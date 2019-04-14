package filesystem;

import static java.util.Arrays.copyOfRange;
import static utilitarios.Utils.MAX_CHUNK_SIZE;

import java.io.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import service.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * classe SystemManager
 */
public class SystemManager {


    private static final String BACKUPS = "backup/";
    private static final String RESTORED = "restored/";
    private Peer parent_peer;
    private String root_path;
    private Database database;
    private MemoryAdmin memoryManager;

    /**
     * construtor de SystemManager
     *
     * @param parent_peer
     * @param max_memory
     */
    public SystemManager(Peer parent_peer, long max_memory) {
        this.parent_peer = parent_peer;
        this.root_path = "fileSystem/peer" + parent_peer.get_ID() + "/";

        initialize_peer_file_system();

        initialize_permanent_state(max_memory);
    }

    /**
     * cria um directorio
     *
     * @param name nome do diretorio
     */
    public static void create_folder(String name) {
        try {
            Files.createDirectories(Paths.get(name));
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Directorio nao criado");
        }
    }

    /**
     * carrega um ficheiro
     *
     * @param pathname caminho do ficheiro a carregar
     * @return dados do ficheiro
     */
    synchronized public static byte[] load_file(String pathname) {
        InputStream input_stream;
        long file_size;
        byte[] data;

        try {
            input_stream = Files.newInputStream(Paths.get(pathname));
            file_size = get_file_size(Paths.get(pathname));
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Erro no File nao encontrado");
            return null;
        }

        data = new byte[(int) file_size];

        try {
            input_stream.read(data);
            input_stream.close();
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Erro ao ler data ");
        }

        return data;
    }

    /**
     * obtem tamanho do ficheiro
     *
     * @param filepath caminho do ficheiro
     * @return tamanho do ficheiro
     */
    public static long get_file_size(Path filepath) {
        BasicFileAttributes attr = null;

        try {
            attr = Files.readAttributes(filepath, BasicFileAttributes.class);
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Erro na leitura dos atributos");
        }
        return attr.size();
    }

    /**
     * divide ficheiro em chunks de acordo com o tamanho máximo estipulado para cada chunk
     *
     * @param file_ID            identificador do ficheiro
     * @param replication_degree grau de replicação
     * @param file_data          dados do ficheiro
     * @return lista de chunks
     */
    public static ArrayList<ChunkData> file_into_chunks(String file_ID, int replication_degree,
                                                        byte[] file_data) {
        ArrayList<ChunkData> chunks = new ArrayList<>();

        int num_chunks = file_data.length / MAX_CHUNK_SIZE + 1;

        into_chunks_loop(file_ID, replication_degree, file_data, chunks, num_chunks);

        return chunks;
    }

    /**
     * ciclo que coloca cada MAX_CHUNK_SIZE KB do ficheiro num chunk
     *
     * @param file_ID            identificador do ficheiro
     * @param replication_degree grau de replicação
     * @param file_data          dados do ficheiro
     * @param chunks             lista de chunks
     * @param num_chunks         numero de chunks (tamanho do ficheiro/MAX_CHUNK_SIZE)
     */
    private static void into_chunks_loop(String file_ID, int replication_degree, byte[] file_data,
                                         ArrayList<ChunkData> chunks, int num_chunks) {
        for (int i = 0; i < num_chunks; i++) {

            byte[] chunk_data = get_chunk_data(file_data, num_chunks, i);

            ChunkData chunk = new ChunkData(file_ID, i, replication_degree, chunk_data);
            chunks.add(chunk);
        }
    }

    /**
     * obtem dados para colocar num chunk
     *
     * @param file_data  dados do ficheiro
     * @param num_chunks numero de chunks
     * @param chunk_No   numero do chunk
     * @return dados do chunk
     */
    private static byte[] get_chunk_data(byte[] file_data, int num_chunks, int chunk_No) {
        byte[] chunk_data;
        if (chunk_No == num_chunks - 1 && file_data.length % MAX_CHUNK_SIZE == 0) {
            chunk_data = new byte[0];
        } else if (chunk_No == num_chunks - 1) {
            int leftOverBytes = file_data.length - (chunk_No * MAX_CHUNK_SIZE);
            chunk_data = copyOfRange(file_data, chunk_No * MAX_CHUNK_SIZE, chunk_No * MAX_CHUNK_SIZE + leftOverBytes);
        } else {
            chunk_data = copyOfRange(file_data, chunk_No * MAX_CHUNK_SIZE, chunk_No * MAX_CHUNK_SIZE + MAX_CHUNK_SIZE);
        }
        return chunk_data;
    }

    /**
     * junta o chunks num só ficheiro
     *
     * @param chunks lista de chunks
     * @return array de bytes com o conteudo total do ficheiro unido
     */
    public static byte[] file_merge(ArrayList<ChunkData> chunks) {
        ByteArrayOutputStream output_stream = new ByteArrayOutputStream();

        for (int i = 0; i < chunks.size(); i++) {
            add_chunk(chunks, output_stream, i);
        }

        return output_stream.toByteArray();
    }

    /**
     * adiciona o chunk à output_stream
     *
     * @param chunks        lista de chunks
     * @param output_stream array de bytes
     * @param chunk_No      numero do chunk
     */
    private static void add_chunk(ArrayList<ChunkData> chunks, ByteArrayOutputStream output_stream,
                                  int chunk_No) {
        try {
            output_stream.write(chunks.get(chunk_No).get_data());
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Não foi possível juntar os chunks num ficheiro");
        }
    }

    /**
     * Inicializa estado da memória
     *
     * @param max_memory tamanho máximo da memória
     */
    private void initialize_permanent_state(long max_memory) {
        try {
            initialize_memory_manager(max_memory);
            initialize_database();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inicializa gerenciador da memória
     *
     * @param max_memory tamanho máximo da memória
     * @throws IOException            exceção de in/out
     * @throws ClassNotFoundException exceção de classe não encontrada
     */
    private void initialize_memory_manager(long max_memory) throws IOException, ClassNotFoundException {
        File mm = new File(root_path + "memoryManager");

        if (mm.exists()) {
            this.memoryManager = (MemoryAdmin) MemoryAdmin.load_from_file(mm);
        } else {
            this.memoryManager = new MemoryAdmin(mm.getAbsolutePath(), max_memory);
        }
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void initialize_database() throws IOException, ClassNotFoundException {
        File db = new File(root_path + "db");

        if (db.exists()) {
            this.database = (Database) Database.load_from_file(db);
        } else {
            this.database = new Database(db.getAbsolutePath());
        }
    }

    /**
     * @param file_name
     * @param pathname
     * @param data
     * @return
     * @throws IOException
     */
    synchronized public SAVE_STATE save_file(String file_name, String pathname, byte[] data)
            throws IOException {
        if (memoryManager.getAvailableMemory() < data.length) {
            utilitarios.Notificacoes_Terminal.printAviso("Não tem espaço suficiente para guardar o ficheiro");
            return SAVE_STATE.FAILURE;
        }
        String filepath = pathname + "/" + file_name;

        if (Files.exists(Paths.get(filepath))) {
            utilitarios.Notificacoes_Terminal.printAviso("O ficheiro já existe");
            return SAVE_STATE.EXISTS;
        }

        OutputStream out = Files.newOutputStream(Paths.get(filepath));
        out.write(data);
        out.close();

        memoryManager.increase_peer_memory(data.length);
        return SAVE_STATE.SUCCESS;
    }

    /**
     * @param file_ID
     * @param chunk_No
     * @return
     */
    public String get_chunk_path(String file_ID, int chunk_No) {

        return get_chunks_path() + file_ID + "/chk" + chunk_No;

    }

    /**
     * @param file_ID
     * @param chunk_No
     * @return
     */
    public byte[] load_chunk(String file_ID, int chunk_No) {

        String chunk_path = get_chunk_path(file_ID, chunk_No);

        return load_file(chunk_path);
    }

    /**
     *
     */
    private void initialize_peer_file_system() {
     try{   create_folder(root_path + BACKUPS);
        create_folder(root_path + RESTORED);
    }catch(Exception e){
         e.printStackTrace();}

}


    /**
     * @return
     */
    public String get_chunks_path() {
        return root_path + BACKUPS;
    }

    /**
     * @return
     */
    public String get_restored_path() {
        return root_path + RESTORED;
    }

    /**
     * @return
     */
    public Database get_database() {
        return database;
    }

    /**
     * @param file_ID
     * @param chunk_No
     */
    public void delete_chunk(String file_ID, int chunk_No) {
        String chunk_path = get_chunk_path(file_ID, chunk_No);
        Path path = Paths.get(chunk_path);
        long chunk_size = get_file_size(path);
        try {
            delete_pathchunk(path);
            memoryManager.reduce_peer_memory(chunk_size);
            database.remove_from_history_chunks_backed_up(file_ID, chunk_No);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path
     */
    private void delete_pathchunk(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            utilitarios.Notificacoes_Terminal.printMensagemError("Erro na remocao do file: " + path);
        }
    }

    /**
     * @return
     */
    public MemoryAdmin get_memory_manager() {
        return memoryManager;
    }

    /**
     *
     */
    public enum SAVE_STATE {
        EXISTS,
        SUCCESS,
        FAILURE
    }

}