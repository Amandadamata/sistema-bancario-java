# Design decisions

Each entry records what was decided, why, what else was considered, and how the decision is
verified.

## 1. `BigDecimal` for money

- **Decision:** store balances and amounts as `BigDecimal` with scale 2.
- **Why:** `double` stores binary fractions; `0.1 + 0.2` evaluates to `0.30000000000000004`. In the
  first version of this project, depositing 0.10 and 0.70 and withdrawing 0.80 was refused because
  the balance was `0.7999999999999999`.
- **Alternatives:** `long` cents everywhere; `double` with rounding on display (hides the error).
- **Verified by:** `ContaTest.somaDeCentavosEExata`.

## 2. Reject amounts with more than two decimal places

- **Decision:** `10.005` is rejected instead of rounded.
- **Why:** an amount typed by a person should be exactly what is moved.
- **Verified by:** `ContaTest.operacoesComValorInvalidoSaoRecusadasSemAlterarSaldos`, `MoedaTest`.

## 3. Banker's rounding (`HALF_EVEN`) for calculated values

- **Decision:** the yield is rounded to cents with `HALF_EVEN` (5.025 → 5.02, 0.505 → 0.50).
- **Why:** avoids a systematic upward bias on ties.
- **Verified by:** `ContaPoupancaTest.empateNoArredondamentoVaiParaOCentavoPar`.

## 4. Inheritance for account types

- **Decision:** `ContaCorrente` and `ContaPoupanca` extend the abstract `Conta`; the checking account
  overrides `calcularSaldoDisponivel()`.
- **Alternative:** one `Conta` with a pluggable spending policy (composition), better if account
  types multiplied.
- **Verified by:** `ContaTest.contaCorrentePermiteUsarChequeEspecialAteOLimite`.

## 5. Checked exception for insufficient funds

- **Decision:** `SaldoInsuficienteException` is checked; invalid arguments use unchecked exceptions.
- **Why:** insufficient funds is an expected outcome every caller must handle.

## 6. Validate everything before changing state

- **Decision:** operations check all rules before touching a balance.
- **Verified by:** `ContaTest.transferenciaSemSaldoNaoAlteraNenhumaConta`.

## 7. SQLite with JDBC for persistence

- **Decision:** store data in a local SQLite file through plain JDBC.
- **Why:**
  - a single file and no server to install, which suits a command-line application;
  - real SQL tables, constraints, and **ACID transactions**, which the transfer rule needs;
  - JDBC is the standard Java database API; using it directly shows what frameworks such as JPA or
    Spring Data do underneath.
- **Alternatives considered:**
  - *JSON file* (as in the VetCare project): simple, but rewrites the whole file on every change and
    does not demonstrate SQL;
  - *H2 embedded database*: equally suitable, but specific to the Java ecosystem;
  - *PostgreSQL*: needs a server; too heavy for a local CLI;
  - *JPA/Hibernate*: hides the SQL this project wants to show and adds configuration.
- **Costs:** the JAR grows to about 12 MB because the driver bundles native libraries for each
  operating system; the driver loads native code (a manifest entry avoids the JDK 24+ warning).
- **Verified by:** `RepositorioSqliteTest`, `MainTest.dadosSobrevivemAoReinicioDoPrograma`.

## 8. Integer cents in the database

- **Decision:** money columns are `INTEGER` cents.
- **Why:** SQLite `REAL` is binary floating point and would bring back the rounding errors removed in
  decision 1. SQLite has no exact decimal type. Integers are exact and allow `CHECK (valor > 0)`.
- **Alternative:** `TEXT` with the decimal string; exact, but SQL comparisons and sums would be
  textual.
- **Verified by:** `RepositorioSqliteTest.valoresSaoGravadosComoCentavosInteiros`,
  `conversaoDeCentavosEExataEmAmbosOsSentidos`.

## 9. `Banco` as the single entry point, with capture → apply → save or undo

- **Decision:** operations that change balances are package-private on `Conta`; `Main` calls `Banco`.
  `Banco` captures the state of the accounts involved (`EstadoConta`, a *memento*), applies the rule,
  saves all of them in one repository call, and restores the captured state if saving fails.
- **Why:** without this, memory could show a deposit that was never saved. Restoring from the
  captured state does not depend on the database being readable at that moment.
- **Alternatives:** reload everything from the database after a failure (fails if the database is
  unavailable); save each change from inside `Conta` (couples the domain to storage).
- **Verified by:** `BancoPersistenciaTest.falhaAoGravarDepositoDesfazAAlteracaoNaMemoria`,
  `falhaAoGravarTransferenciaDesfazAsDuasContas`, `falhaAoGravarRendimentoNaoConsomeOMes`.

## 10. One SQL transaction per operation

- **Decision:** `RepositorioSqlite.salvar` disables auto-commit, writes every account involved, and
  commits; any error triggers a rollback.
- **Why:** a transfer must be stored on both sides or on neither.
- **Verified by:** `RepositorioSqliteTest.transferenciaInterrompidaNoMeioDaGravacaoNaoDeixaNenhumLadoGravado`.
  A database trigger rejects the destination entry after the origin was already written in the same
  transaction. With the transaction removed, this test fails: the reload finds a half-written
  transfer.

## 11. Append-only statement rows and integrity check on load

- **Decision:** statement rows have a sequence number per account and are only inserted, never
  updated. On load, `Conta.restaurarHistorico` replays every entry and compares it with the stored
  balances.
- **Why:** the statement is the source of truth for the balance; any divergence means the file was
  damaged or edited outside the application, and using it could create money.
- **Verified by:** `RepositorioSqliteTest.saldoAlteradoForaDaAplicacaoEDetectadoAoCarregar`,
  `BancoPersistenciaTest.carregarRecusaSaldoQueNaoCorrespondeAoHistorico`.

## 12. Schema version and refusing newer files

- **Decision:** `PRAGMA user_version` stores the schema version. A newer version is refused without
  changes.
- **Why:** an older program must not misread or overwrite data written by a newer one. Future schema
  changes can be migrations from one version to the next.
- **Verified by:** `RepositorioSqliteTest.esquemaDeUmaVersaoMaisNovaERecusado`.

## 13. CPF as a value object with check digits

- **Decision:** `Cpf.de(text)` accepts digits with optional dots, hyphen, and spaces; requires 11
  ASCII digits; rejects sequences of one repeated digit; and checks both check digits (weights 10..2
  and 11..2, remainder rule). It stores the 11 digits; `toString()` is masked.
- **Why:** invalid documents are rejected at the edge; a value object keeps the rule in one place.
  Repeated sequences such as `111.111.111-11` satisfy the arithmetic but are not issued, so they are
  rejected explicitly.
- **Limits:** the check is mathematical; it does not prove that the CPF exists or belongs to the
  person.
- **Privacy:** messages never repeat the number typed.
- **Verified by:** `CpfTest` (valid synthetic CPFs, null, length, characters, repeated digits, wrong
  check digits, masking).

## 14. One CPF per account holder

- **Decision:** opening an account with a CPF already registered under a different name is refused.
  The same person can have several accounts.
- **Why:** the `clientes` table uses the CPF as its primary key.
- **Verified by:** `BancoPersistenciaTest.cpfNaoPodePertencerADoisTitulares`.

## 15. Injected `Clock` and no printing in the domain

- **Decision:** time comes from a `Clock`; accounts return data and `Main` formats it.
- **Why:** time-dependent rules and the statement can be tested.

## 16. Portuguese identifiers

- **Decision:** class and method names stay in Portuguese; public documentation is in English.
- **Why:** the code models Brazilian banking terms; translating would change almost every line
  without improving behavior.
