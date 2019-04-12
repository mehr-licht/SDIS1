package filesystem;


/**
 * Usado para o reclaim
 * */
public class MemoryManager extends PermanentStateClass {

    /*For serialization*/
    private static final long serialVersionUID = 2L;

    private long maximum_Memory;
    private long used_memory;

    /**
     * */
    MemoryManager(String savePath, long maxMemory) {
        this.maximum_Memory = maxMemory;
        this.used_memory = 0;

        this.setUp(savePath);
    }

    /**
     * Diminuir memoria disponivel no peer
     * */
    public void reduce_peer_memory(long n) {
        used_memory = used_memory - n;
        if (used_memory < 0) {
            used_memory = 0;
            utilitarios.Notificacoes_Terminal.printMensagemError("TESTE : mem < 0");
        }
    }

    /**
     * Aumentar a memoria disponvel no peer
     * */
    public boolean increase_peer_memory(long n) {
        if (used_memory + n > maximum_Memory) {
            utilitarios.Notificacoes_Terminal.printAviso("Limite de memória ultrapassada");
            return false;
        }
        used_memory += n;
        utilitarios.Notificacoes_Terminal.printAviso("Memória Usada: " + used_memory + " / " + maximum_Memory);
        return true;
    }


    /**
     * GETS AND SETTERS
     */
    public void setMaximum_Memory(int maximum_Memory) {
        this.maximum_Memory = maximum_Memory;
    }

    public long getUsed_memory() {
        return this.used_memory;
    }

    public long getMaximum_Memory() {
        return this.maximum_Memory;
    }

    public long getAvailableMemory() {
        return maximum_Memory - used_memory;
    }


}
