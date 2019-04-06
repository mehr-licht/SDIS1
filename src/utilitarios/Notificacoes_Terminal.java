package utilitarios;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Classe para visualização de Notificações, Erros e Avisos
 */
public class Notificacoes_Terminal {

    private static PrintStream printConsola = System.err;
    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Padrao de notificacao
     */
    private static String getNotificacaoNormal() {
        Date date = new Date();
        return "" + Thread.currentThread().getId() + ", " + dateFormat.format(date) + ": ";
    }

    /**
     * Definição da mensagem de erro
     */
    synchronized public static void printMensagemError(String msg) {
        String mensagemHeader = "Ocorreu um erro: ";
        String mensagemBody = getNotificacaoNormal() + msg;
        String mensagemEnd = "----- End";
        printConsola.println(mensagemHeader + mensagemBody + mensagemEnd);
        printConsola.flush();
    }

    /**
     * Definição da mensagem de aviso
     */
    synchronized public static void printAviso(String msg) {
        String mensagemHeader = "Aviso: ";
        String mensagemEnd = "----- End";

        printConsola.println(mensagemHeader + getNotificacaoNormal() + msg + mensagemEnd);
        printConsola.flush();
    }

    /**
     * Definição da mensagem de Notificação
     */
    synchronized public static void printNotificao(String msg) {
        String mensagemHeader = "Notificacao: "+msg;
        String mensagemEnd = "----- End";

        printConsola.println(mensagemHeader + getNotificacaoNormal() + mensagemEnd);
        printConsola.flush();
    }

  /*

  synchronized public static void setPrintConsola(String filepath) throws FileNotFoundException {
    utilitarios.Notificacoes_Terminal.printConsola = new PrintStream(new FileOutputStream(filepath, true));
  }
  */
}
