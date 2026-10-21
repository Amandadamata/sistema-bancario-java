# Design decisions

Each entry records what was decided, why, what else was considered, and how the decision is
verified. Entries are short on purpose; the code and tests are the source of truth.

## 1. `BigDecimal` for money

- **Decision:** store balances and amounts as `BigDecimal` with scale 2.
- **Why:** `double` stores binary fractions. `0.1` has no exact binary representation, so
  `0.1 + 0.2` evaluates to `0.30000000000000004`. In a balance, these errors accumulate and
  comparisons such as "is the balance enough?" can give the wrong answer at the boundary.
- **Alternatives:** `long` amounts in cents (exact and fast, but every input, output and
  percentage calculation needs manual conversion); `double` with rounding on display (hides the
  error instead of removing it).
- **Verified by:** `ContaTest.somaDeCentavosEExata`, `ContaTest.saldoSempreTemDuasCasasDecimais`.

## 2. Reject amounts with more than two decimal places

- **Decision:** `Moeda.validarValorPositivo` rejects `10.005` instead of rounding it.
- **Why:** an amount typed by a person should be exactly what is moved. Rounding silently would
  change the operation without the user noticing.
- **Alternative:** round every input with `HALF_EVEN`. Rejected because rounding belongs to
  calculations, not to user intent.
- **Verified by:** the `0.001` and `10.005` cases in
  `ContaTest.operacoesComValorInvalidoSaoRecusadasSemAlterarSaldos` and `MoedaTest`.

## 3. Banker's rounding (`HALF_EVEN`) for calculated values

- **Decision:** the savings yield is rounded to cents with `RoundingMode.HALF_EVEN`.
- **Why:** with `HALF_UP`, every exact tie (`x.xx5`) rounds up, which over many operations adds a
  small systematic gain. `HALF_EVEN` sends ties to the even cent, so ties round up and down about
  equally often.
- **Example:** 0.5% of 1,005.00 is 5.025 → **5.02**; 0.5% of 101.00 is 0.505 → **0.50**.
- **Verified by:** `ContaPoupancaTest.empateNoArredondamentoVaiParaOCentavoPar` and
  `rendimentoVoltaAFicarDisponivelNoMesSeguinte`.

## 4. Inheritance for account types

- **Decision:** `ContaCorrente` and `ContaPoupanca` extend the abstract class `Conta`.
- **Why:** both are accounts and share almost all behavior (balance, history, deposits,
  withdrawals, transfers). The only difference in spending rules is how much is available, which
  `ContaCorrente` changes by overriding one method, `calcularSaldoDisponivel()`. This is the
  template-method pattern in its simplest form.
- **Alternative:** a single `Conta` class with a pluggable "spending policy" object (composition).
  More flexible if account types multiply or change at runtime, but more classes than two fixed
  account types need.
- **Verified by:** `ContaTest.contaCorrentePermiteUsarChequeEspecialAteOLimite` and
  `transferenciaPodeUsarChequeEspecial` (the same `sacar`/`transferir` code respects the limit).

## 5. Checked exception for insufficient funds

- **Decision:** `SaldoInsuficienteException extends Exception` (checked); invalid arguments use
  `IllegalArgumentException` (unchecked).
- **Why:** insufficient funds is an expected business outcome that every caller must handle, and
  a checked exception makes the compiler enforce that. Invalid arguments are programming or input
  errors that callers should prevent, which is what unchecked exceptions are for.
- **Alternative:** return a boolean or a result object. Rejected because it is easy to ignore a
  return value, and the message with the available balance would need another channel.
- **Verified by:** `ContaTest.saqueComSaldoInsuficienteMantemSaldoEInformaODisponivel`.

## 6. Validate everything before changing state

- **Decision:** `transferir` checks destination, amount and available balance before calling
  `registrar` on either account.
- **Why:** if the origin were debited and a later check failed, money would disappear. With all
  checks first, an operation either completes fully or changes nothing.
- **Verified by:** `ContaTest.transferenciaSemSaldoNaoAlteraNenhumaConta` (balances and history
  sizes unchanged).

## 7. `Banco` owns account numbering

- **Decision:** accounts are created only through `Banco.abrirContaCorrente` and
  `abrirContaPoupanca`; account constructors are not public; the number is consumed only after
  the account was created successfully.
- **Why:** the earlier version used a `static` counter in `Conta`. Static state is shared by the
  whole program, so test results depended on test order, and a failed construction skipped a
  number.
- **Verified by:** `BancoTest.aberturaRecusadaNaoConsomeNumero`,
  `cadaBancoTemSuaPropriaNumeracao`.

## 8. Injected `Clock`

- **Decision:** time comes from a `Clock` passed to `Banco`.
- **Why:** the yield rule depends on the current month. With `LocalDate.now()` inside the class,
  a test could only check "next month" by waiting a month.
- **Verified by:** `ContaPoupancaTest` uses `RelogioAjustavel` to move the clock forward 12 days.

## 9. Immutable statement entries

- **Decision:** `Transacao` is a `record`; `getHistorico()` returns `List.copyOf(...)`.
- **Why:** callers can read the history but cannot alter the past. A record also gives correct
  `equals`, so a test can compare a whole expected statement in one assertion.
- **Note:** `contaRelacionada` is an `OptionalInt` to make "no related account" explicit. Using
  `Optional` types as fields is debated in Java; here the record is a simple value carrier and the
  explicitness was preferred over a nullable `Integer`.
- **Verified by:** `ContaTest.historicoRegistraTipoValorSaldoDataEContaRelacionada`,
  `historicoRetornadoNaoPodeSerAlterado`.

## 10. No printing in the domain

- **Decision:** the statement is built by `Main` from `getHistorico()`.
- **Why:** a method that only prints cannot be checked by a test, and it ties business classes to
  one kind of interface.
- **Verified by:** `MainTest` checks printed output; `ContaTest` checks the data.

## 11. Portuguese identifiers

- **Decision:** class and method names stay in Portuguese.
- **Why:** the code was written in Portuguese and models Brazilian banking terms. Translating
  every identifier would change almost every line without improving behavior. Public
  documentation is in English and explains the terms.

## 12. No persistence

- **Decision:** data lives only in memory.
- **Why:** the project focuses on object-oriented modeling and business rules. Persistence is a
  possible next step and is listed as a limitation.
