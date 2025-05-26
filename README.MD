# Java Virtual Threads: Uma Análise Prática de Desempenho

## Visão Geral
Este projeto documenta um experimento prático que explora as características de desempenho das Virtual Threads do Java (introduzidas no JDK 21 através do Projeto Loom) em comparação com as tradicionais Platform Threads. Utilizamos uma aplicação Java simples para conduzir testes sob cargas de trabalho I/O-bound (limitadas por entrada/saída) e CPU-bound (limitadas pelo processador), fornecendo resultados concretos e uma análise detalhada.

## Motivação
O objetivo principal foi ir além das discussões teóricas e obter um entendimento prático sobre:
- Em quais cenários as Virtual Threads oferecem vantagens significativas?
- Como elas se comportam em tarefas que consomem intensamente a CPU?
- Quais são suas implicações práticas para o desenvolvimento de aplicações concorrentes em Java?

Este README, o código-fonte e a documentação mais extensa em LaTeX e PDF(disponível separadamente) registram o experimento, os resultados e os principais aprendizados.

## Configuração do Experimento

### Pré-requisitos
- Java Development Kit (JDK) 21 ou superior.

### Código-Fonte
A lógica central do experimento está contida no arquivo `ThreadComparisonDemo.java`. Este código simula:
- **Tarefas I/O-bound:** Utilizando `Thread.sleep()` para mimetizar a espera por operações externas (ex: chamadas de rede, consultas a banco de dados).
- **Tarefas CPU-bound:** Realizando cálculos matemáticos intensivos em um loop.
- A execução dessas tarefas utilizando tanto `Executors.newFixedThreadPool()` (para Platform Threads) quanto `Executors.newVirtualThreadPerTaskExecutor()` (para Virtual Threads).

### Ambiente de Teste (Exemplo da Execução Realizada)
- **Versão do JDK:** 21.0.4
- **Processadores Disponíveis (Núcleos):** 8

## Como Executar o Experimento
1.  Salve o código Java compartilhado anteriormente como `ThreadComparisonDemo.java`.
2.  Compile o código:
    ```bash
    javac ThreadComparisonDemo.java
    ```
3.  Execute a classe compilada:
    ```bash
    java ThreadComparisonDemo
    ```
    (Certifique-se de que sua variável de ambiente `JAVA_HOME` aponta para um JDK 21+ ou utilize o caminho completo para os executáveis `java` e `javac`).

## Principais Resultados e Descobertas (Baseado na Execução de Exemplo)

O experimento demonstrou distinções claras de desempenho entre os dois tipos de threads nos cenários testados:

### 1. Cenário I/O-Bound
- **Configuração da Tarefa:** 10.000 tarefas, cada uma simulando uma espera (sleep) de 50ms.
- **Platform Threads (Pool de 200 threads):** Tempo total $\approx$ **3255 ms**
- **Virtual Threads:** Tempo total $\approx$ **506 ms**
- **Observação:** As Virtual Threads foram aproximadamente **6,43 vezes mais rápidas**, resultando em uma redução de cerca de **84,5%** no tempo de execução. Isso evidencia sua notável eficiência ao lidar com um grande volume de operações concorrentes que passam a maior parte do tempo bloqueadas (esperando).

### 2. Cenário CPU-Bound
- **Configuração da Tarefa:** 16 tarefas (2 vezes o número de núcleos da CPU), cada uma com 30 milhões de iterações de um cálculo matemático.
- **Platform Threads (Pool de 8 threads, correspondendo aos núcleos da CPU):** Tempo total $\approx$ **14.667 ms**
- **Virtual Threads:** Tempo total $\approx$ **14.300 ms**
- **Observação:** O desempenho foi muito similar. Nesta execução específica, as Virtual Threads foram marginalmente mais rápidas (cerca de 2,5%). Isso confirma que as Virtual Threads não aumentam intrinsecamente a capacidade de processamento da CPU; o gargalo para tarefas computacionalmente intensivas continua sendo os próprios núcleos do processador.

*(Para a saída completa do console com os resultados detalhados, por favor, consulte a documentação em LaTeX ou o artigo completo derivado desta análise.)*

## Destaques da Discussão
Nossa análise mais aprofundada (detalhada no documento LaTeX) também abordou:
- **Limites de Escalabilidade:** Embora as Virtual Threads elevem drasticamente o patamar para concorrência em I/O, elas não são infinitas e possuem limites relacionados à memória, sobrecarga de gerenciamento pela JVM e capacidade dos recursos do sistema subjacente.
- **"Pinning" de Threads:** É necessário ter atenção com blocos `synchronized` ou chamadas a métodos nativos (JNI) que podem "prender" (pin) as carrier threads, potencialmente reduzindo os benefícios das Virtual Threads naquelas seções específicas do código se ocorrerem operações bloqueantes dentro delas.


## Conclusão
As Virtual Threads são uma adição poderosa e transformadora ao Java, especialmente para aplicações I/O-bound, onde melhoram drasticamente a escalabilidade e simplificam o código concorrente. Para tarefas CPU-bound, seu desempenho é comparável ao de platform threads bem configuradas. Compreender a natureza da carga de trabalho da sua aplicação é fundamental para aproveitá-las efetivamente.

## Documentação Detalhada
Para uma análise mais aprofundada, incluindo a derivação das porcentagens, por favor, verifique o código LaTeX ou o PDF disponível em Releases, ambos no diretório. 

---

Autor: Érick Cabral
