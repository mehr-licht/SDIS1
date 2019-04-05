package service;

import static utilitarios.Utils.get_registry;
import static utilitarios.Utils.parse_RMI;

import java.util.HashMap;
import java.util.Map;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.nio.file.Paths;

public class TestApp implements Runnable {
  private String[] peer_ap;
  private String sub_protocol;
  private String opnd_1;
  private String opnd_2;

  private RemoteBackupService stub;
  private Map<String, Runnable> service_handlers;

  /**
   * Corre o serviço pedido de acordo com os parametros passados
   * @param peer_ap ponto de acesso do peer (localhost/n)
   * @param sub_protocol serviço a executar
   * @param opnd_1 operando 1 do serviço a executar
   * @param opnd_2 operando 2 do serviço a executar
   */
  public TestApp(String[] peer_ap, String sub_protocol, String opnd_1, String opnd_2) {
    this.peer_ap = peer_ap;
    this.sub_protocol = sub_protocol;
    this.opnd_1 = opnd_1;
    this.opnd_2 = opnd_2;

    service_handlers = new HashMap<>();
    service_handlers.put("BACKUP", this::handle_backup);
    service_handlers.put("RESTORE", this::handle_restore);
    service_handlers.put("DELETE", this::handle_delete);
    service_handlers.put("RECLAIM", this::handle_reclaim);
    service_handlers.put("STATE", this::handle_state);
  }

  /**
   * main TestApp
   * @param args peer_ap ; sub_protocol ; opnd_1 ; opnd_2
   */
  public static void main(String[] args) {
    if (!usage_ok(args)) {
      return;
    }

    //host/name   ou    //host:port/name
    String[] peer_ap = parse_RMI(args[0], false);
    if (peer_ap == null) {
      return;
    }

    start_thread(args, peer_ap);
  }

  /**
   * Inicia uma thread com o serviço pedido
   * @param args arguments received by main
   * @param peer_ap ponto de acesso do peer
   */
  private static void start_thread(String[] args, String[] peer_ap) {
    String sub_protocol = args[1];
    String operand1 = get_operand(args,1);
    String operand2 =get_operand(args,2);

    TestApp app = new TestApp(peer_ap, sub_protocol, operand1, operand2);
    new Thread(app).start();
  }

  /**
   * Partindo dos argumentos recebidos pela main inicializa correctamente o operando1 e operando2
   * @param args argumentos recebidos pela main
   * @param which qual o operando pedido
   * @return o valor do operando pedido
   */
  private static String get_operand(String[] args, int which) {
    String operand;
    if(args.length > which+1){
      operand=args[which+1];
    }else{
      operand=null;
    }
    return operand;
  }

  /**
   * Verifica se o número de argumentos está correcto
   * @param args arguments received by main
   * @return true or false
   */
  private static boolean usage_ok(String[] args) {
    if (args.length > 1 && args.length < 5) {
      utilitarios.Notificacoes_Terminal.printAviso("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
      return true;
    }
    return false;
  }


  /**
   * Lançamento do serviço pedido
   */
  @Override
  public void run() {
    initiate_RMI_stub();
    service_handlers.get(sub_protocol).run();
  }

  /**
   * Invocação do stub do RMI
   */
  private void initiate_RMI_stub() {
    try {
      Registry registry = get_registry(peer_ap);
      stub = (RemoteBackupService) registry.lookup(peer_ap[2]);
    } catch (Exception e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Erro ao abrir o stub do RMI");
      e.printStackTrace();
    }
  }

  /**
   * handler do sub-protocolo BACKUP
   */
  private void handle_backup() {
    utilitarios.Notificacoes_Terminal.printNotificao("A fazer backup do ficheiro de \"" + Paths.get(this.opnd_1) + "\"");

    try {
      stub.backup(this.opnd_1, Integer.parseInt(this.opnd_2));
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /**
   * handler do sub-protocolo DELETE
   */
  private void handle_delete() {
    utilitarios.Notificacoes_Terminal.printNotificao("A apagar ficheiro \"" + opnd_1 + "\"");

    try {
      stub.delete(this.opnd_1);
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /**
   * handler do sub-protocolo RESTORE
   */
  private void handle_restore() {
    utilitarios.Notificacoes_Terminal.printNotificao("A restaurar o ficheiro \"" + opnd_1 + "\"");

    try {
      stub.restore(opnd_1);
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /**
   * handler do sub-protocolo RECLAIM
   */
  private void handle_reclaim() {
    utilitarios.Notificacoes_Terminal.printNotificao("A recuperar espaço em disco: \"" + opnd_1 + "\"");

    try {
      stub.reclaim(Integer.parseInt(opnd_1));
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }

  /**
   * handler do sub-protocolo STATE
   */
  private void handle_state() {
    utilitarios.Notificacoes_Terminal.printNotificao("Podes ver o meu estado na minha tab");
    try {
      stub.state();
    } catch (RemoteException e) {
      utilitarios.Notificacoes_Terminal.printMensagemError("Exceção de Cliente: " + e.toString());
    }
  }
}