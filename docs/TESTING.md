# Testing

## Running the tests

```bash
./mvnw test      # compile and run all tests
./mvnw verify    # tests plus packaging, the same command CI runs
```

A single class or method:

```bash
./mvnw test -Dtest=ContaPoupancaTest
./mvnw test -Dtest=ContaTest#somaDeCentavosEExata
```

## Test classes

| Class | What it verifies |
|---|---|
| `ContaTest` | Deposits, withdrawals, transfers, overdraft, exact cents, rejected amounts, statement contents, immutability |
| `ContaPoupancaTest` | Yield calculation, once-per-month rule, next-month availability, `HALF_EVEN` ties, tiny balances |
| `BancoTest` | Sequential numbering, independent banks, rejected accounts not consuming numbers, lookup, listing |
| `MoedaTest` | Parsing `100,50` / `100.50`, rejecting ambiguous or invalid text, `R$` formatting |
| `ClienteTest` | Required name and CPF, trimming, CPF hidden in `toString` |
| `MainTest` | Full menu sessions with simulated keyboard input, including invalid input and end of input |

## Techniques used

- **Behavior over methods.** Tests are named after the behavior they protect
  (`transferenciaSemSaldoNaoAlteraNenhumaConta`), and failed operations always assert that the
  state did **not** change.
- **Parameterized tests.** `@ParameterizedTest` with `@ValueSource`, `@NullSource` and
  `@CsvSource` runs one rule against many inputs (zero, negative, `null`, too many decimals).
- **Exact comparisons.** Money is compared with `assertEquals(new BigDecimal("0.30"), ...)`. There
  is no tolerance margin; the previous `double` tests used `0.001`, which hid rounding errors.
- **Controlled time.** `RelogioAjustavel` is a test `Clock` that only moves when the test calls
  `avancar(...)`, so month boundaries are deterministic.
- **Simulated console.** `MainTest` builds `Main` with a `ByteArrayInputStream` and captures a
  `PrintStream`, so the real menu runs exactly as a user would drive it.

## Coverage

Coverage is measured occasionally to find untested code, not as a target. To generate a local
report without changing `pom.xml`:

```bash
./mvnw org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test \
       org.jacoco:jacoco-maven-plugin:0.8.13:report
open target/site/jacoco/index.html
```

In the last measurement, the only domain lines not executed were defensive guards that the
normal flow cannot reach: an account number of zero or less (accounts are numbered by `Banco`
from 1000), a debit type passed to `creditar`, and a CPF shorter than three characters in
`toString`. The remaining uncovered lines are in `Main`: the `main` method that wires real
`System.in`/`System.out`, and a few retry and "account not found" branches.

## Continuous integration

`.github/workflows/ci.yml` runs `./mvnw --batch-mode verify` on Java 17, 21 and 25 for every push and
pull request to `main`.
