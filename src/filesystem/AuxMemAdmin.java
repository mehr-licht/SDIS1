package filesystem;

import utilitarios.Utils;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public abstract class AuxMemAdmin implements Serializable {

  private String savePath;

  /**
   * Const
   * */
  static AuxMemAdmin load_from_file(File file) throws IOException, ClassNotFoundException {
    AuxMemAdmin obj;

    FileInputStream fileIn = new FileInputStream(file);
    final ObjectInputStream inputStream = new ObjectInputStream(fileIn);
    obj = (AuxMemAdmin) inputStream.readObject();

    try {
        inputStream.close();
        fileIn.close();
    }catch (Exception e){
        utilitarios.Notificacoes_Terminal.printMensagemError("Erro AuxMemAdmin no close file");

    }

    if (obj != null) {
      obj.set_up(file.getAbsolutePath());
    } else {
      utilitarios.Notificacoes_Terminal.printMensagemError("Erro no init de AuxMemAdmin do file");
    }

    return obj;
  }

  protected void set_up(String absPath) {
    this.savePath = absPath;
      try {
          AuxMemAdmin obj = this;
          Timer timer = new Timer();
          timer.scheduleAtFixedRate(new TimerTask() {
              @Override
              public void run() {
                  obj.save_permanent_state();
              }
          }, Utils.SAVE_PERIOD, Utils.SAVE_PERIOD);
      }catch(Exception e){
          utilitarios.Notificacoes_Terminal.printMensagemError("line 47 Aux MemAdmin");

          e.printStackTrace();
      }
  }


  /**
   * Escreve o ficheiro no disco
   * */
  synchronized void save_permanent_state() {
    try {
      FileOutputStream out = new FileOutputStream(savePath);
      ObjectOutputStream oos = new ObjectOutputStream(out);
      oos.writeObject(this);
      oos.close();
    } catch (IOException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Erro na escrita do file");
      e.printStackTrace();
    }
  }

}
