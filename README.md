# COBOL para Java — DCV Bancário

Projeto de estudo para praticar a conversão de código COBOL para Java,
com evolução progressiva até uma aplicação desktop completa com banco de dados.

## Sobre o projeto

Conversão de um sistema de processamento de contas bancárias (DCV) escrito 
em COBOL para Java, evoluindo de um programa de console para uma aplicação 
desktop com interface gráfica e persistência em banco de dados.

## O que o sistema faz

- Cadastra clientes com dados bancários via interface gráfica
- Processa movimentações bancárias (PIX, TED, DOC, SAQ, DEP)
- Valida saldo disponível antes de aprovar débitos
- Valida limite de PIX por transação
- Verifica bloqueio de conta
- Gera extrato com totais de créditos, débitos, aprovadas e negadas
- Persiste clientes, dados financeiros e movimentações no PostgreSQL
- Lista clientes cadastrados em tabela interativa

## Tecnologias

- Java 21
- Eclipse IDE
- Java Swing (interface gráfica desktop)
- JDBC + PostgreSQL (persistência de dados)
- DBeaver (cliente de banco de dados)

## Estrutura do projeto
```
bankDCV/
├── Bank_DCV.java        — estruturas de dados (equivalente ao WORKING-STORAGE do COBOL)
├── DcvGUI.java          — interface gráfica Swing com 4 telas
├── DcvDAO.java          — acesso ao banco de dados (padrão DAO)
└── ConexaoPostgres.java — conexão JDBC com PostgreSQL
```

## Banco de dados
```sql
CREATE TABLE cliente (
    cpf        VARCHAR(14) PRIMARY KEY,
    nome       VARCHAR(40),
    agencia    VARCHAR(4),
    conta      VARCHAR(7),
    digito     INT,
    tipo_conta VARCHAR(2)
);

CREATE TABLE financeiro (
    cpf           VARCHAR(14) PRIMARY KEY,
    saldo_atual   DECIMAL(13,2),
    limite_cheque DECIMAL(13,2),
    saldo_total   DECIMAL(13,2),
    limite_pix    DECIMAL(13,2),
    FOREIGN KEY (cpf) REFERENCES cliente(cpf)
);

CREATE TABLE movimentacao (
    id         SERIAL PRIMARY KEY,
    cpf        VARCHAR(14),
    tipo_mov   VARCHAR(3),
    valor      DECIMAL(11,2),
    hora       VARCHAR(6),
    status_mov VARCHAR(1),
    FOREIGN KEY (cpf) REFERENCES cliente(cpf)
);
```

## Como executar

1. Suba o PostgreSQL e crie o banco `dcv_banco`
2. Execute o script SQL acima para criar as tabelas
3. Configure usuário e senha em `ConexaoPostgres.java`
4. Execute `DcvGUI.java` como Java Application no Eclipse

## Equivalências COBOL → Java aprendidas

| COBOL | Java |
|---|---|
| `WORKING-STORAGE SECTION` | Classes internas `static` |
| `PIC 9(n)` | `int` / `long` / `String` |
| `PIC S9(n)V99` | `double` |
| `PIC X(n)` | `String` |
| `OCCURS n TIMES` | Array / `new Classe[n]` |
| `88 NIVEL-88` | `enum` ou método `boolean` |
| `PERFORM UNTIL` | `while` |
| `PERFORM VARYING` | `for` |
| `MOVE x TO y` | `y = x` |
| `ADD x TO y` | `y += x` |
| `ADD x y GIVING z` | `z = x + y` |
| `FUNCTION ABS` | `Math.abs()` |
| `DISPLAY` | `System.out.println()` |

## Código COBOL original

```cobol
IDENTIFICATION DIVISION.
PROGRAM-ID. DCV-CONTAS.
AUTHOR. BANCO-EXEMPLO.

ENVIRONMENT DIVISION.
CONFIGURATION SECTION.
SOURCE-COMPUTER. IBM-MAINFRAME.
OBJECT-COMPUTER. IBM-MAINFRAME.

DATA DIVISION.
WORKING-STORAGE SECTION.

    *> Dados do cliente
    01 WS-CLIENTE.
        05 WS-CPF            PIC 9(11).
        05 WS-NOME           PIC X(40).
        05 WS-AGENCIA        PIC 9(4).
        05 WS-CONTA          PIC 9(7).
        05 WS-DIGITO         PIC 9(1).
        05 WS-TIPO-CONTA     PIC X(2).
           88 CONTA-CORRENTE VALUE 'CC'.
           88 CONTA-POUPANCA VALUE 'CP'.
           88 CONTA-SALARIO  VALUE 'CS'.

    *> Saldo e limites
    01 WS-FINANCEIRO.
        05 WS-SALDO-ATUAL    PIC S9(13)V99.
        05 WS-LIMITE-CHEQUE  PIC S9(13)V99.
        05 WS-SALDO-TOTAL    PIC S9(13)V99.
        05 WS-LIMITE-PIX     PIC S9(13)V99.

    *> Movimentações do dia
    01 WS-MOVIMENTACOES.
        05 WS-MOV OCCURS 10 TIMES.
            10 WS-TIPO-MOV   PIC X(3).
               88 MOV-PIX    VALUE 'PIX'.
               88 MOV-TED    VALUE 'TED'.
               88 MOV-DOC    VALUE 'DOC'.
               88 MOV-SAQ    VALUE 'SAQ'.
               88 MOV-DEP    VALUE 'DEP'.
            10 WS-VALOR-MOV  PIC S9(11)V99.
            10 WS-HORA-MOV   PIC 9(6).
            10 WS-STATUS-MOV PIC X(1).
               88 MOV-APROVADA  VALUE 'A'.
               88 MOV-NEGADA    VALUE 'N'.
               88 MOV-PENDENTE  VALUE 'P'.

    *> Controle de processamento
    01 WS-CONTROLE.
        05 WS-IDX            PIC 9(2) VALUE 0.
        05 WS-TOT-APROVADAS  PIC 9(3) VALUE 0.
        05 WS-TOT-NEGADAS    PIC 9(3) VALUE 0.
        05 WS-SOMA-DEBITOS   PIC S9(13)V99 VALUE 0.
        05 WS-SOMA-CREDITOS  PIC S9(13)V99 VALUE 0.
        05 WS-RETORNO        PIC 9(2) VALUE 0.

    *> Codigos de retorno
    01 WS-CODIGOS.
        05 WS-COD-OK         PIC 9(2) VALUE 00.
        05 WS-COD-SALDO      PIC 9(2) VALUE 01.
        05 WS-COD-LIMITE     PIC 9(2) VALUE 02.
        05 WS-COD-BLOQUEIO   PIC 9(2) VALUE 03.
        05 WS-CONTA-BLOQ     PIC X(1) VALUE 'N'.
           88 CONTA-BLOQUEADA VALUE 'S'.

PROCEDURE DIVISION.

    *> Inicializa dados do cliente
    MOVE 12345678901     TO WS-CPF
    MOVE 'JOAO DA SILVA' TO WS-NOME
    MOVE 0001            TO WS-AGENCIA
    MOVE 0123456         TO WS-CONTA
    MOVE 9               TO WS-DIGITO
    MOVE 'CC'            TO WS-TIPO-CONTA

    *> Inicializa financeiro
    MOVE 1500.00         TO WS-SALDO-ATUAL
    MOVE 500.00          TO WS-LIMITE-CHEQUE
    MOVE 1000.00         TO WS-LIMITE-PIX
    ADD WS-SALDO-ATUAL WS-LIMITE-CHEQUE
        GIVING WS-SALDO-TOTAL

    *> Popula movimentacoes do dia
    MOVE 'PIX' TO WS-TIPO-MOV(1)
    MOVE -200.00 TO WS-VALOR-MOV(1)
    MOVE 083022  TO WS-HORA-MOV(1)
    MOVE 'P'     TO WS-STATUS-MOV(1)

    MOVE 'DEP' TO WS-TIPO-MOV(2)
    MOVE 300.00  TO WS-VALOR-MOV(2)
    MOVE 091500  TO WS-HORA-MOV(2)
    MOVE 'P'     TO WS-STATUS-MOV(2)

    MOVE 'SAQ' TO WS-TIPO-MOV(3)
    MOVE -100.00 TO WS-VALOR-MOV(3)
    MOVE 103045  TO WS-HORA-MOV(3)
    MOVE 'P'     TO WS-STATUS-MOV(3)

    MOVE 'TED' TO WS-TIPO-MOV(4)
    MOVE -800.00 TO WS-VALOR-MOV(4)
    MOVE 113000  TO WS-HORA-MOV(4)
    MOVE 'P'     TO WS-STATUS-MOV(4)

    MOVE 'PIX' TO WS-TIPO-MOV(5)
    MOVE -600.00 TO WS-VALOR-MOV(5)
    MOVE 140000  TO WS-HORA-MOV(5)
    MOVE 'P'     TO WS-STATUS-MOV(5)

    *> Processa cada movimentacao
    PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 5

        *> Verifica bloqueio antes de qualquer coisa
        IF CONTA-BLOQUEADA
            MOVE WS-COD-BLOQUEIO TO WS-RETORNO
            MOVE 'N' TO WS-STATUS-MOV(WS-IDX)
            ADD 1 TO WS-TOT-NEGADAS
            GO TO FIM-PROCESSAMENTO
        END-IF

        *> Verifica se eh debito
        IF WS-VALOR-MOV(WS-IDX) < 0

            *> Verifica se tem saldo total suficiente
            IF FUNCTION ABS(WS-VALOR-MOV(WS-IDX)) > WS-SALDO-TOTAL
                MOVE WS-COD-SALDO TO WS-RETORNO
                MOVE 'N' TO WS-STATUS-MOV(WS-IDX)
                ADD 1 TO WS-TOT-NEGADAS

            *> Verifica limite PIX
            ELSE IF MOV-PIX AND
                    FUNCTION ABS(WS-VALOR-MOV(WS-IDX)) > WS-LIMITE-PIX
                MOVE WS-COD-LIMITE TO WS-RETORNO
                MOVE 'N' TO WS-STATUS-MOV(WS-IDX)
                ADD 1 TO WS-TOT-NEGADAS

            *> Aprova debito
            ELSE
                ADD WS-VALOR-MOV(WS-IDX) TO WS-SALDO-ATUAL
                ADD WS-VALOR-MOV(WS-IDX) TO WS-SOMA-DEBITOS
                MOVE WS-COD-OK TO WS-RETORNO
                MOVE 'A' TO WS-STATUS-MOV(WS-IDX)
                ADD 1 TO WS-TOT-APROVADAS
            END-IF

        *> Se eh credito, aprova direto
        ELSE
            ADD WS-VALOR-MOV(WS-IDX) TO WS-SALDO-ATUAL
            ADD WS-VALOR-MOV(WS-IDX) TO WS-SOMA-CREDITOS
            MOVE WS-COD-OK TO WS-RETORNO
            MOVE 'A' TO WS-STATUS-MOV(WS-IDX)
            ADD 1 TO WS-TOT-APROVADAS
        END-IF

        *> Recalcula saldo total apos cada operacao
        ADD WS-SALDO-ATUAL WS-LIMITE-CHEQUE
            GIVING WS-SALDO-TOTAL

    END-PERFORM.

    FIM-PROCESSAMENTO.
        DISPLAY '=============================='
        DISPLAY 'EXTRATO DCV - ' WS-NOME
        DISPLAY 'AG: ' WS-AGENCIA ' CC: ' WS-CONTA '-' WS-DIGITO
        DISPLAY '=============================='
        DISPLAY 'Saldo final  : ' WS-SALDO-ATUAL
        DISPLAY 'Total creditos: ' WS-SOMA-CREDITOS
        DISPLAY 'Total debitos : ' WS-SOMA-DEBITOS
        DISPLAY 'Aprovadas    : ' WS-TOT-APROVADAS
        DISPLAY 'Negadas      : ' WS-TOT-NEGADAS
        DISPLAY '=============================='
        STOP RUN.
```

## Saída esperada
```
==============================
Extrato DCV - JOAO DA SILVA
AG: 0001 CC: 0123456-9
==============================
Saldo final: 100.0
Total Creditos: 300.0
Total Debitos: -1700.0
Aprovadas: 5
Negadas: 0
==============================
```
