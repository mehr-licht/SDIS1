

**Serviço de Backup Distribuído**

Este &quot;read me&quot; explica como correr o Serviço de Backup Distribuído.

**Como correr**

Em linux, abrir um terminal e, a partir do diretório da raiz do projeto, correr:

1. 1)sh createPeers.sh \&lt;versão\&gt; \&lt;nºpeers\&gt;

Por exemplo, sh createPeers.sh 1.0  5

Este é um script que limpa os chunks e ficheiros restaurados, compila, verifica se o serviço RMI está a correr e inicializa-o caso não esteja e cria a quantidade de peers passada como argumento com a versão do protocolo também indicada na chamada.

Em alternativa, pode-se fazer estes passos em separado:

| sh clearFiles.sh // limpa os chunks e ficheiros restauradossh compile.sh // compila o projeto (também possível com o comando javac)sh rmi.sh // inicializa o serviço RMI em segundo planosh peer.sh \&lt;versão\&gt; \&lt;peer\_id\&gt; // lançar um para cada peer que se queira criar e idealmente em tabs separadas |
| --- |

As versões do protocolo podem ser verificadas mais abaixo.

2) A seguir, lançar a aplicação testApp no terminal inicial ou num novo no directório da raiz do projeto com:
sh run.sh \&lt;peer\_id\&gt; \&lt;serviço\&gt; \&lt;operando1\&gt; \&lt;operando2\&gt;

em que o serviço pode ser BACKUP, RESTORE, RECLAIM, DELETE ou STATE. Operando1, no caso do serviço ser BACKUP, RESTORE ou DELETE é o nome do ficheiro e no caso de ser RECLAIM indica o espaço de memória que se quer dedicada ao peer.

Por exemplo,

| sh run.sh 1 BACKUP &quot;files/lusiadas.txt&quot; 3
sh run.sh 1 RESTORE &quot;files/lusiadas.txt&quot;
sh run.sh 1 DELETE &quot;files/lusiadas.txt&quot;
sh run.sh 1 STATE
sh run.sh 1 RECLAIM 63000 |
| --- |

**Versões do protocolo**

Além do protocolo base da versão normal, implementamos mais quatro versões para os três melhoramentos:

Versão **1.0** : versão normal

Versão **1.1** : melhoramento do sub-protocolo de backup

Versão **1.2** : melhoramento do sub-protocolo de restore

Versão **1.3** : melhoramento do sub-protocolo de delete

Versão **2.0** : todos os anteriores melhoramentos em ação



**Para compilar**

Conforme já indicado, pode-se compilar o projeto com

| javac -d bin -sourcepath src src/service/TestApp.java src/service/Peer.java |
| --- |

ou com o script fornecido

| sh compile.sh |
| --- |

**Lançar o serviço RMI**

Implementamos o serviço RMI que deve estar a correr quando se corre o Serviço Distribuído de Backup.

Para iniciar o serviço, deve-se correr

| sh rmi.sh |
| --- |

Que chama o comando rmiregistry &amp;

**Criar peers**

Pode-se criar cada peer através de

| java -classpath bin service.Peer \&lt;versão\&gt; \&lt;peer\_id\&gt; \&lt;peer\_ap\&gt; \&lt;mc:porto\&gt; \&lt;mdb:porto\&gt; \&lt;mdr:porto\&gt; |
| --- |

Em que, mc é o canal multicast, mdb é o canal de backup de dados e mdr é o canal de restore de dados.

Por exemplo, com os valores default para os canais e ponto de acesso:

| java -classpath bin service.Peer 1.0 1 //localhost:1099/ 224.0.0.0:8000 224.0.0.0:8001 224.0.0.0:8002 |
| --- |



**Correr a aplicação Test App**

Pode-se lançar a Test App através de

| java -classpath bin service.TestApp \&lt;PEER\_AP\&gt; \&lt;SERVIÇO\&gt; \&lt;OPERANDO1\&gt; \&lt;OPERANDO2\&gt; |
| --- |

em que o serviço pode ser BACKUP, RESTORE, RECLAIM, DELETE ou STATE. Operando1, no caso do serviço ser BACKUP, RESTORE ou DELETE é o nome do ficheiro e no caso de ser RECLAIM indica o espaço de memória que se quer dedicada ao peer.

Por exemplo,

| java -classpath bin service.TestApp //localhost/1 BACKUP &quot;files/lusiadas.txt&quot; 1java -classpath bin service.TestApp //localhost/1 RESTORE &quot;files/lusiadas.txt&quot;java -classpath bin service.TestApp //localhost/1 DELETE &quot;files/lusiadas.txt&quot;java -classpath bin service.TestApp //localhost/1 STATEjava -classpath bin service.TestApp //localhost/1 RECLAIM 63000 |
| --- |

**Estrutura de diretórios**

- src/ : directório com o código fonte
- bin/ : directório com os ficheiros das classes
- files/ : directório com os ficheiros a testar
- fileSystem/ : diretório onde será criado o sistema de ficheiros de cada peer com a estrutura exigida para o trabalho:
  - peer#
    - ■backup
      - 0x#####################################
        - chk0
        - chk1
        - …
    - ■restored
      - lusiadas.txt



**Os nossos scripts**

- sh : script para compilar o projeto
- sh : script para verificar se o serviço rmi está a correr (se for necessário termina-lo usar kill -9 pid
- sh : script para lançar o serviço rmi em segundo plano
- sh : script para limpar sistema de ficheiros dos peers
- sh : script para iniciar/criar cada peer
- sh : script para correr os sub-protocolos
- sh : script com uma bateria de testes (backup, state, restore, reclaim e delete) em que devemos confirmar o resultado
