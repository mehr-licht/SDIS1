package filesystem;

public class MemoryManager extends PermanentStateClass {

  private static final long serialVersionUID = 2L;

  private long maxMemory;
  private long usedMemory;

  MemoryManager(long maxMemory, String savePath) {
    this.maxMemory = maxMemory;
    this.usedMemory = 0;

    this.setUp(savePath);
  }

    public void setMaxMemory(int maxMemory) {
    this.maxMemory = maxMemory;
  }

  public long getUsedMemory() {
    return this.usedMemory;
  }

  public long getMaxMemory() {
    return this.maxMemory;
  }

  public long getAvailableMemory() {
    return maxMemory - usedMemory;
  }

  public void reduceUsedMemory(long n) {
    usedMemory -= n;
    if (usedMemory < 0) {
      usedMemory = 0;
      utilitarios.Notificacoes_Terminal.printMensagemError("Used memory went below 0");
    }
  }

  public boolean increaseUsedMemory(long n) {
    if (usedMemory + n > maxMemory) {
      utilitarios.Notificacoes_Terminal.printAviso("Tried to surpass memory restrictions");
      return false;
    }
    usedMemory += n;
    utilitarios.Notificacoes_Terminal.printAviso("Used memory: " + usedMemory + " / " + maxMemory);
    return true;
  }

}
