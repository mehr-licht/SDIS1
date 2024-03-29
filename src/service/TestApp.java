package service;

import static utilitarios.Utils.get_registry;
import static utilitarios.Utils.parse_RMI;

import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

/** TestApp classe é o nosso cliente em comparação com as labs e os peers são o nosso server */
public class TestApp implements Runnable {
  private String[] peer_ap;
  private String operation;
  private String opnd_1;
  private String opnd_2;

  private My_Interface_Remote stub;

  private Map<String, Runnable> service_handlers;

  /**
   * Corre o serviço pedido de acordo com os parametros passados no terminal do TestApp Default
   * command: service.TestApp //127.0.0.1/1 BACKUP $file $replic
   *
   * @param peer_ap ponto de acesso do peer (localhost/n) em que n é o número do peer
   * @param operation serviço a executar
   * @param opnd_1 operando 1 do serviço a executar
   * @param opnd_2 operando 2 do serviço a executar
   */
  public TestApp(String[] peer_ap, String operation, String opnd_1, String opnd_2) {
    this.peer_ap = peer_ap;
    this.operation = operation;
    this.opnd_1 = opnd_1;
    this.opnd_2 = opnd_2;

    service_handlers = new HashMap<>();
    service_handlers.put("BACKUP", this::handle_backup);
    service_handlers.put("STATE", this::handle_state);
    service_handlers.put("DELETE", this::handle_delete);
    service_handlers.put("RESTORE", this::handle_restore);
    service_handlers.put("RECLAIM", this::handle_reclaim);
  }

  /**
   * main TestApp
   *
   * @param args [0] <peer_ap> acess point
   * @param args [1] <operation> recebe o nome do subprotocol (BACKUP | RESTORE | DELETE | RECLAIM |
   *     STATE) ou no caso de melhoramentos ( BACKUPENH | RESTOREENH | DELETEENH | RECLAIMENH )
   * @param args [2] <opnd_1> pathname of the file case: (BACKUP | RESTORE | DELETE ) or espaço
   *     maximo do disco KBYTE case: ( RECLAIM )
   * @param args [3] <opnd_2> grau de replicação = replication degree VALID only on ( BACKUP |
   *     BACKUPENH ) NOTE: IF (args[2] == deletion){doRECLAIMprotocol;}
   */
  public static void main(String[] args) {
    if (!confirmacao_numero_args(args)) {
      return;
    }

    // host/name   ou    //host:port/name Faz as confirmações necessárias
    String[] peer_ap = parse_RMI(args[0], false);
    if (peer_ap == null) {
      return;
    }
    start_thread(args, peer_ap);
  }

  /**
   * Inicia uma thread com o serviço pedido
   *
   * @param args [1] (BACKUP | RESTORE | DELETE | RECLAIM | STATE)[ENH]
   * @param args [2] pathname of the file case ou number Bytes
   * @param peer_ap ponto de acesso do peer
   */
  private static void start_thread(String[] args, String[] peer_ap) {
    String operation_ = args[1];
    String operand1 = get_operand(args, 1);
    String operand2 = get_operand(args, 2);

    TestApp app = new TestApp(peer_ap, operation_, operand1, operand2);
    new Thread(app).start();
  }

  /**
   * Partindo dos argumentos recebidos pela main inicializa correctamente o operando1 e operando2
   *
   * @param args argumentos recebidos pela main
   * @param which posicao do argumento que queremos
   * @return o valor do argumento especificado
   */
  private static String get_operand(String[] args, int which) {
    String operand;
    if (args.length > which + 1) {
      operand = args[which + 1];
    } else {
      operand = null;
    }
    return operand;
  }

  /**
   * Verificação se o número de argumentos está correcto
   *
   * @param args arguments recebidos pela main
   * @return verdadeiro ou falso
   */
  private static boolean confirmacao_numero_args(String[] args) {
    if (args.length >= 2 && args.length < 5) {
      return true;
    }
    utilitarios.Notificacoes_Terminal.printAviso(
        "Correr no formato: java TestApp <peer_ap> <operation> <opnd_1> <opnd_2>");
    return false;
  }

  /**
   * The Runnable interface should be implemented by any class whose instances are intended to be
   * executed by a thread. The class must define a method of no arguments called run Subscreve a RMI
   * e chama o run() de cada classe BACKUP STATE DELETE RESTORE RECLAIM
   */
  @Override
  public void run() {
    initiate_RMI_stub();
    service_handlers.get(operation).run();
  }

  /**
   * Subscrição do RMI, tal como feito na Lab do RMI lookup -> Como na lab:Returns the remote
   * reference bound to the specified name in this registry.
   */
  private void initiate_RMI_stub() {
    try {
      Registry registry = get_registry(peer_ap);
      stub = (My_Interface_Remote) registry.lookup(peer_ap[2]);
    } catch (Exception e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Erro ao abrir o stub do RMI");
      e.printStackTrace();
    }
  }

  /** handler do sub-protocolo BACKUP Usa filePathName + grau de replicação */
  private void handle_backup() {
    utilitarios.Notificacoes_Terminal.printNotificao(
        "A fazer backup do ficheiro de \""
            + Paths.get(this.opnd_1)
            + "\""
            + " com GrauReplicacao: "
            + this.opnd_2);
    try {
      stub.backup(this.opnd_1, Integer.parseInt(this.opnd_2));
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /** handler do sub-protocolo DELETE Usa filePathName */
  private void handle_delete() {
    utilitarios.Notificacoes_Terminal.printNotificao("A apagar ficheiro \"" + opnd_1 + "\"");

    try {
      stub.delete(this.opnd_1);
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /** handler do sub-protocolo RESTORE Usa filePathName */
  private void handle_restore() {
    utilitarios.Notificacoes_Terminal.printNotificao("A restaurar o ficheiro \"" + opnd_1 + "\"");
    try {
      stub.restore(opnd_1);
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /** handler do sub-protocolo RECLAIM Usa int em Bytes */
  private void handle_reclaim() {
    utilitarios.Notificacoes_Terminal.printNotificao(
        "A recuperar espaço em disco: \"" + opnd_1 + "\"");
    try {
      stub.reclaim(Integer.parseInt(opnd_1));
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /** handler do sub-protocolo STATE */
  private void handle_state() {
    utilitarios.Notificacoes_Terminal.printNotificao("Podes ver o meu estado na minha tab");
    try {
      stub.state();
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }
}
