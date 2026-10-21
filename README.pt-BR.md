# Sistema Bancário em Java

[![Java CI](https://github.com/Amandadamata/sistema-bancario-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Amandadamata/sistema-bancario-java/actions/workflows/ci.yml)

[English](README.md) · **Português (Brasil)**

Aplicação de linha de comando que modela contas corrente e poupança com aritmética monetária exata,
cheque especial, transferências, rendimento mensal da poupança e extrato com movimentações tipadas.
É um projeto de fundamentos de Java focado em orientação a objetos, regras de negócio e testes.

## Problema resolvido

Regras bancárias são um bom teste de correção: dinheiro não pode se perder em arredondamentos, uma
transferência recusada precisa deixar as duas contas inalteradas e toda movimentação precisa ser
rastreável. Este projeto implementa essas regras em Java puro e as verifica com testes automatizados.

## Funcionalidades

- Abertura de contas corrente (com limite de cheque especial) e poupança
- Depósitos, saques e transferências entre contas, inclusive usando o cheque especial
- Valores monetários exatos com `BigDecimal` (duas casas decimais, arredondamento explícito)
- Rendimento mensal de 0,5% na poupança, aplicado no máximo uma vez por mês-calendário
- Extrato com tipo, valor com sinal, saldo resultante, data/hora e conta relacionada
- Entrada e saída no padrão brasileiro (`100,50` ou `100.50` na entrada; `R$ 1.234,56` na saída)
- Menu interativo que se recupera de entradas inválidas

## Tecnologias

- Java 17 (também verificado com Java 21 e 25 na CI)
- Maven com Maven Wrapper
- JUnit 6
- GitHub Actions

## Arquitetura

Todas as classes ficam no pacote `com.github.amandadamata.sistemabancario`. Os identificadores do
código estão em português porque o projeto modela termos bancários brasileiros.

| Classe | Responsabilidade |
|---|---|
| `Banco` | Abre contas, controla a numeração e busca contas pelo número |
| `Conta` | Base abstrata: saldo, depósitos, saques, transferências e histórico |
| `ContaCorrente` | Sobrescreve o saldo disponível para incluir o limite do cheque especial |
| `ContaPoupanca` | Acrescenta a regra de rendimento mensal |
| `Transacao` / `TipoTransacao` | Lançamento imutável do extrato e seu tipo (crédito/débito) |
| `Moeda` | Conversão, validação, arredondamento e formatação em `R$` |
| `Cliente` | Titular com nome e CPF obrigatórios |
| `Main` | Apenas o menu do console, sem regras de negócio |

Principais decisões de projeto (detalhes em [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md), em inglês):

- **`BigDecimal` em vez de `double`**, porque em `double` `0.1 + 0.2` é `0.30000000000000004`.
  Valores com mais de duas casas decimais são recusados em vez de arredondados silenciosamente.
- **Arredondamento bancário (`HALF_EVEN`)** somente onde um valor é calculado (o rendimento).
- **Polimorfismo no cheque especial:** `ContaCorrente` sobrescreve `calcularSaldoDisponivel()`,
  então o código de saque e transferência é compartilhado.
- **Validar antes de alterar o estado:** uma transferência recusada não altera nenhuma das contas.
- **`Clock` injetado:** a regra de rendimento mensal é testada avançando um relógio de teste.
- **O domínio não imprime:** as contas devolvem dados e a `Main` os formata, o que torna o extrato testável.

## Estrutura do projeto

```text
.
├── .github/
│   ├── dependabot.yml              # Verificação mensal de atualizações de dependências
│   └── workflows/ci.yml            # Build e testes com Java 17, 21 e 25
├── docs/                           # Arquitetura, decisões de projeto, testes
├── .mvn/wrapper/                   # Configuração do Maven Wrapper
├── mvnw, mvnw.cmd                  # Scripts do Maven Wrapper
├── pom.xml
└── src/
    ├── main/java/com/github/amandadamata/sistemabancario/
    │   ├── Banco.java
    │   ├── Cliente.java
    │   ├── Conta.java
    │   ├── ContaCorrente.java
    │   ├── ContaPoupanca.java
    │   ├── Main.java
    │   ├── Moeda.java
    │   ├── SaldoInsuficienteException.java
    │   ├── TipoTransacao.java
    │   └── Transacao.java
    └── test/java/com/github/amandadamata/sistemabancario/
        ├── BancoTest.java
        ├── ClienteTest.java
        ├── ContaPoupancaTest.java
        ├── ContaTest.java
        ├── MainTest.java
        ├── MoedaTest.java
        └── RelogioAjustavel.java       # Relógio de teste que avança sob demanda
```

## Pré-requisitos

- JDK 17 ou superior (`java -version`)
- Acesso à internet no primeiro build, para o Maven Wrapper baixar o Maven e as dependências

Não é necessário ter o Maven instalado.

## Build e execução (macOS / Linux)

```bash
git clone https://github.com/Amandadamata/sistema-bancario-java.git
cd sistema-bancario-java
./mvnw clean package
java -jar target/sistema-bancario-java-1.0.0.jar
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

## Exemplo de uso

Saída real da aplicação empacotada (linhas do menu omitidas). A conta 1000 é uma poupança que recebe
um depósito, envia uma transferência, recebe o rendimento do mês e recusa um segundo rendimento no
mesmo mês:

```text
Rendimento de R$ 4,25 aplicado à conta 1000.
Erro: O rendimento de 10/2026 já foi aplicado a esta conta.

===== EXTRATO — Conta 1000 (Conta Poupança) — Titular: Ana Souza =====
[20/10/2026 19:21:04] Depósito                                   +R$ 1.000,00 | Saldo após: R$ 1.000,00
[20/10/2026 19:21:04] Transferência enviada (para a conta 1001)  -R$ 150,50 | Saldo após: R$ 849,50
[20/10/2026 19:21:04] Rendimento                                 +R$ 4,25 | Saldo após: R$ 853,75
Saldo atual: R$ 853,75
```

## Testes

```bash
./mvnw test
```

A suíte cobre:

- aritmética exata de centavos e recusa de valores zero, negativos, nulos ou com casas demais
- saques e transferências no limite, dentro e além do saldo disponível (com e sem cheque especial)
- saldos e histórico inalterados após qualquer operação recusada
- lançamentos do extrato: tipo, valor, saldo resultante, data/hora, conta relacionada e imutabilidade
- rendimento da poupança: cálculo, regra de uma vez por mês, liberação no mês seguinte e empates `HALF_EVEN`
- numeração das contas, inclusive aberturas recusadas que não consomem número
- conversão de valores digitados e formatação em `R$`
- fluxos completos do menu com entrada simulada, incluindo entradas inválidas e fim da entrada

## Integração contínua

O GitHub Actions executa `./mvnw --batch-mode verify` com Java 17, 21 e 25 a cada push e pull request
para a `main`. O workflow tem permissão somente de leitura no repositório.

## Segurança e privacidade

- Os dados existem apenas em memória; nada é gravado em disco ou enviado pela rede.
- Os testes usam o CPF fictício `000.000.000-00`.
- O CPF é obrigatório, mas não é validado (formato ou dígitos verificadores).

## Limitações conhecidas

- Sem persistência: as contas são perdidas ao encerrar a aplicação.
- O rendimento da poupança é uma regra educacional (0,5% por mês-calendário sobre o saldo atual). Não
  reproduz a regra oficial da poupança (data de aniversário, TR, Selic).
- Sem juros ou tarifas de cheque especial.
- Um usuário e um processo; sem autenticação.

## Próximos passos possíveis

- Validação do CPF com dígitos verificadores e mascaramento na exibição
- Persistência (por exemplo, arquivo ou banco de dados relacional)

## Documentação (em inglês)

- [Arquitetura](docs/ARCHITECTURE.md): diagrama de classes, responsabilidades, fluxo de execução, tratamento de erros
- [Decisões de projeto](docs/DESIGN_DECISIONS.md): o que foi escolhido, por quê, alternativas e os testes que verificam
- [Testes](docs/TESTING.md): como executar, o que cada classe verifica, cobertura

## Licença

[MIT](LICENSE)
