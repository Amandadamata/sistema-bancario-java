# Banking System in Java

[![Java CI](https://github.com/Amandadamata/sistema-bancario-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Amandadamata/sistema-bancario-java/actions/workflows/ci.yml)

**English** · [Português (Brasil)](README.pt-BR.md)

A command-line banking application that models checking and savings accounts with exact money
arithmetic, overdraft limits, transfers, monthly savings yield, and a typed transaction statement.
It is a Java fundamentals project focused on object-oriented design, business rules, and tests.

## Problem it solves

Banking rules are a good test of correctness: money must never be lost to rounding, a failed
transfer must leave both accounts unchanged, and every movement must be traceable. This project
implements those rules in plain Java and verifies them with automated tests.

## Features

- Open checking accounts (with an overdraft limit) and savings accounts
- Deposits, withdrawals, and transfers between accounts, including overdraft use
- Exact monetary values with `BigDecimal` (two decimal places, explicit rounding)
- Monthly savings yield of 0.5%, applied at most once per calendar month
- Statement with type, signed amount, resulting balance, timestamp, and related account
- Brazilian number input and output (`100,50` or `100.50` in; `R$ 1.234,56` out)
- Interactive menu that recovers from invalid input

## Technologies

- Java 17 (also verified on Java 21 in CI)
- Maven with the Maven Wrapper
- JUnit 5
- GitHub Actions

## Architecture

All classes live in one package, `com.github.amandadamata.sistemabancario`. Code identifiers are in
Portuguese because the project models Brazilian banking terms; this documentation is in English.

| Class | Responsibility |
|---|---|
| `Banco` (Bank) | Opens accounts, owns account numbering, finds accounts by number |
| `Conta` (Account) | Abstract base: balance, deposits, withdrawals, transfers, history |
| `ContaCorrente` (Checking) | Overrides the available balance to include the overdraft limit |
| `ContaPoupanca` (Savings) | Adds the monthly yield rule |
| `Transacao` / `TipoTransacao` | Immutable statement entry and its credit/debit type |
| `Moeda` (Money) | Money parsing, validation, rounding, and `R$` formatting |
| `Cliente` (Customer) | Account holder with required name and CPF |
| `Main` | Console menu only; no business rules |

Key design decisions (details in [docs/DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)):

- **`BigDecimal` instead of `double`**, because `0.1 + 0.2` in `double` is `0.30000000000000004`.
  Amounts with more than two decimal places are rejected instead of silently rounded.
- **Banker's rounding (`HALF_EVEN`)** only where a value is calculated (the yield).
- **Polymorphism for the overdraft:** `ContaCorrente` overrides `calcularSaldoDisponivel()`, so
  withdrawal and transfer code is shared.
- **Validate first, then change state:** a rejected transfer changes neither account.
- **Injected `Clock`:** the monthly yield rule is tested by moving a test clock forward.
- **No printing in the domain:** accounts return data; `Main` formats it, so the statement is testable.

## Project structure

```text
.
├── .github/
│   ├── dependabot.yml              # Monthly dependency update checks
│   └── workflows/ci.yml            # Build and test on Java 17 and 21
├── docs/                           # Architecture, design decisions, testing
├── .mvn/wrapper/                   # Maven Wrapper configuration
├── mvnw, mvnw.cmd                  # Maven Wrapper scripts
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
        └── RelogioAjustavel.java       # Test clock that advances on demand
```

## Prerequisites

- JDK 17 or newer (`java -version`)
- Internet access on the first build, so the Maven Wrapper can download Maven and dependencies

A global Maven installation is not required.

## Build and run (macOS / Linux)

```bash
git clone https://github.com/Amandadamata/sistema-bancario-java.git
cd sistema-bancario-java
./mvnw clean package
java -jar target/sistema-bancario-java-1.0.0.jar
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Usage example

Real output from the packaged application (menu lines omitted). Account 1000 is a savings account
that receives a deposit, sends a transfer, earns its monthly yield, and rejects a second yield in
the same month:

```text
Rendimento de R$ 4,25 aplicado à conta 1000.
Erro: O rendimento de 10/2026 já foi aplicado a esta conta.

===== EXTRATO — Conta 1000 (Conta Poupança) — Titular: Ana Souza =====
[20/10/2026 19:21:04] Depósito                                   +R$ 1.000,00 | Saldo após: R$ 1.000,00
[20/10/2026 19:21:04] Transferência enviada (para a conta 1001)  -R$ 150,50 | Saldo após: R$ 849,50
[20/10/2026 19:21:04] Rendimento                                 +R$ 4,25 | Saldo após: R$ 853,75
Saldo atual: R$ 853,75
```

The user interface is in Portuguese.

## Tests

```bash
./mvnw test
```

The suite covers:

- exact cent arithmetic and rejection of zero, negative, null, and over-precise amounts
- withdrawals and transfers at, within, and beyond the available balance (with and without overdraft)
- unchanged balances and history after any rejected operation
- statement entries: type, amount, resulting balance, timestamp, related account, immutability
- savings yield: calculation, once-per-month rule, next-month availability, `HALF_EVEN` ties
- account numbering, including rejected accounts not consuming numbers
- money parsing and `R$` formatting
- complete menu flows driven by simulated keyboard input, including invalid input and end of input

## Continuous integration

GitHub Actions runs `./mvnw --batch-mode verify` on Java 17 and 21 for every push and pull request
to `main`. The workflow has read-only repository permissions.

## Security and privacy

- Data exists only in memory; nothing is written to disk or sent over a network.
- Tests use the fictional CPF `000.000.000-00`.
- The CPF is required but not validated (format or check digits).

## Known limitations

- No persistence: accounts are lost when the application exits.
- The savings yield is an educational rule (0.5% per calendar month on the current balance). It does
  not reproduce the official Brazilian savings rule (anniversary date, TR, Selic).
- No overdraft interest or fees.
- Single user, single process; no authentication.

## Possible next steps

- CPF validation with check digits and masking in displayed output
- Persistence (for example, a file or a relational database)

## Documentation

- [Architecture](docs/ARCHITECTURE.md): class diagram, responsibilities, execution flow, error handling
- [Design decisions](docs/DESIGN_DECISIONS.md): what was chosen, why, alternatives, and the tests that verify it
- [Testing](docs/TESTING.md): how to run tests, what each class verifies, coverage

## License

[MIT](LICENSE)
