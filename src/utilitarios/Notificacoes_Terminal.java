package utilitarios;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Classe para visualização de Notificações, Erros e Avisos
 */
public class Notificacoes_Terminal {

    private static PrintStream printConsola = System.err;
    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Padrao body de notificacao, para impresão na consola do número de thread e data e hora
     * @return String body da mensagem impressa na consola
     */
    private static  String getNotificacaoNormal() {
        Date date = new Date();
        return "" + "ID: " + Thread.currentThread().getId()+ ", PID: "+ ProcessHandle.current().pid() + ", " + dateFormat.format(date) + ": ";
    }

    /**
     * Impressão da mensagem total de erro predefinida
     * @param msg mensagem de erro
     *
     */
    synchronized public static void printMensagemError(String msg) {
        String mensagemHeader = "Ocorreu um ERRO: ";
        String mensagemBody = getNotificacaoNormal() + msg;
        String mensagemEnd = "--";
        printConsola.println(mensagemHeader + mensagemBody + mensagemEnd);
        printConsola.flush();
    }

    /**
     * Impressão da mensagem total de erro
     * @param msg mensagem com o aviso definido
     *
     */
    synchronized public static void printAviso(String msg) {
        String mensagemHeader = "AVISO: ";
        String mensagemEnd = "--";

        printConsola.println(mensagemHeader + getNotificacaoNormal() + msg + mensagemEnd);
        printConsola.flush();
    }

    /**
     * Impressão da mensagem total de notificação
     * @param msg mensagem com a notificação definido
     * @return
     */
    synchronized public static void printNotificao(String msg) {
        String mensagemHeader = "Notificacao => "+msg;
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
