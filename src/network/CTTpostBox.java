package network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import service.Peer;


/**
 * Vai ser os nossos CTT
 */
public abstract class CTTpostBox implements Runnable {

    protected Peer parent_peer;
    private BlockingQueue<Message> fila_mensagens_recebendo;
    private Map<Message.Categoria_Mensagem, MessageHandler> map_message_handlers;

    /**
     * Inicializacao
     */
    CTTpostBox(Peer parentPeer) {
        this.parent_peer = parentPeer;

        fila_mensagens_recebendo = new LinkedBlockingDeque<>();
        map_message_handlers = new HashMap<>();

        /**
         * Configuracao dos filtros da caixa de correio
         * */
        configuracao_mensagem_handlers();
    }

    /**
     * Remocao de um protocolo
     * */
    protected void removeMessageHandler(Message.Categoria_Mensagem mensagem_type) {

        map_message_handlers.remove(mensagem_type);
    }

    /**
     * Adicona protocolo
     * */
    protected void adiciona_handle_mensagem(Message.Categoria_Mensagem categoria_mensagem, MessageHandler handler) {
        map_message_handlers.put(categoria_mensagem, handler);
    }

    /**
     * Para ser overiding in CaixaCorreio
     */
    protected abstract void configuracao_mensagem_handlers();

    /**
     * Descarta as mensagens que foram enviadas para ele próprio e as invalidas
     * Processa todas as outras
     * @param message a analisar
     */
    private void processamento_msg_inicial(Message message) {
        //Ignoring invalid messages
        if (message == null || message.get_sender_ID() == parent_peer.get_ID()) {
            return;
        }else {

            utilitarios.Notificacoes_Terminal.printNotificao("Recetor: " + message.toString());

            MessageHandler messageHandler = map_message_handlers.get(message.get_type());
            if (messageHandler != null) {
                messageHandler.handle(message);
            } else {
                utilitarios.Notificacoes_Terminal.printMensagemError("Received unregistered message");
            }
        }
    }

    /**
     * Implements Runnable, logo corre quando lançada em thread
     * Corre preenchendo a BlockingQueue com as mensagens que estão a chegar
     */
    @Override
    public void run() {
        Message message;

        while (true) {
            try { // BlockingQueue.take() yields CPU until a message is available
                message = fila_mensagens_recebendo.take();
                processamento_msg_inicial(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Envia a mensagem para a central de processamento Mensagem
     *
     * */
    public void pushMessage(byte[] data, int length) {
        Message mensagem_processada; // create and parse the message

        try {
            mensagem_processada = new Message(data, length);
        } catch (Exception e) {
            utilitarios.Notificacoes_Terminal.printMensagemError(e.getMessage());
            return;
        }
        fila_mensagens_recebendo.add(mensagem_processada);

    }

    /**
     * Interface a
     */
    interface MessageHandler {

        void handle(Message msg);
    }


    public Peer getParent_peer() {
        return parent_peer;
    }

    public void setParent_peer(Peer parent_peer) {
        this.parent_peer = parent_peer;
    }

    public BlockingQueue<Message> getFila_mensagens_recebendo() {
        return fila_mensagens_recebendo;
    }

    public void setFila_mensagens_recebendo(BlockingQueue<Message> fila_mensagens_recebendo) {
        this.fila_mensagens_recebendo = fila_mensagens_recebendo;
    }

    public Map<Message.Categoria_Mensagem, MessageHandler> getMap_message_handlers() {
        return map_message_handlers;
    }

    public void setMap_message_handlers(Map<Message.Categoria_Mensagem, MessageHandler> map_message_handlers) {
        this.map_message_handlers = map_message_handlers;
    }

}
