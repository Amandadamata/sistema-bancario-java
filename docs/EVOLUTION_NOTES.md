# Evolution notes: from the terminal to the desktop

These notes record the move from the terminal system (`ed9b02f`, version 1.1.0) to the desktop app (version 1.2.0): what already existed, which risks a window added, and how they were handled. Version 1.3.0 then translated everything the user sees into English.

## What already existed

| Class | State and relevant methods | Result of the review |
|---|---|---|
| `Banco` | Account map, next number, clock and repository; open, find and list accounts, deposit, withdraw, transfer, apply interest, snapshot, save, undo, restore | Already centralizes saving and in-memory rollback. It's the entry point the GUI uses. |
| `Conta` | Number, holder, balance, clock and history; internal operations, recording, available-balance check, state snapshot and restore | Positive amounts and cents are validated before any change; the statement is immutable for its consumers. |
| `ContaCorrente` | Limit; overrides available balance, identification and saved record | A withdrawal can go past the balance, up to the limit. The rule was kept and shown clearly in the UI. |
| `ContaPoupanca` | Rate and last month with interest; apply, snapshot, restore and serialize state | 0.5% interest with `HALF_EVEN` rounding; a month only counts as used if interest was credited. |
| `Cliente` | Name and CPF; constructors, getters and a masked `toString` | Name is required; CPF validation is delegated. |
| `Cpf` | Digits; `de` factory, check-digit calculation, formatting, masking, equality and hash | Mathematical validation that also rejects repeated digits. It doesn't prove identity. |
| `Moeda` | Scale, rounding, regex and locale; parse, format and validate | No `double`; accepts a comma or dot as the decimal separator and rejects thousands separators. |
| `Transacao` | Type, amount, balance, date and related account; `valorComSinal` | Immutable record, used directly by the table model. |
| `TipoTransacao` | Description and credit flag | Tells money in from money out without duplicating lists of operations. |
| `TipoConta` | Checking or savings | Stored identification kept as it was. |
| `EstadoConta` | Balance, history size and interest month | Internal snapshot used to undo failed saves. |
| `ContaPersistida` | Identification, holder, limit, balance, month and history | Carries data to the repository and copies the history. |
| `RepositorioBanco` | `carregar`, `salvar`, no-persistence factory | SQL-independent contract, also used in failure tests. |
| `RepositorioSqlite` | Path, version and schema; load, save, prepare file, open connection, create schema, write client, account and transactions, convert cents | SQL transactions, prepared statements, foreign keys and schema validation. No table changes. |
| `SaldoInsuficienteException` | Error message | Checked exception, handled in the presentation layer. |
| `FalhaPersistenciaException` | Message and cause | Storage failure reported without announcing success. |
| `Main` | Bank, scanner and output; menu, reading, account creation, operations, statement and listing | Isolated terminal interface, kept for comparison and regression. |

The build already used the Maven Wrapper and Shade for a terminal JAR with SQLite included. There was no need to restructure the domain packages or migrate data. The initial `./mvnw verify` passed with 135 cases and no failures.

## Risks and how they were handled

- **Balance versus overdraft:** removing the limit rule would break the original system. The GUI separates the account's own balance from what's available, and new checking accounts start with a zero limit in the form.
- **Relative database path:** when opened from Finder, the working directory isn't guaranteed. The desktop app uses a stable user folder and accepts `SISTEMA_BANCARIO_DB` for older data.
- **Concurrent access:** the original model loads a copy of the accounts into memory, so two running apps could overwrite balances with stale state. The desktop app takes a file lock before loading. The terminal menu doesn't take part in that lock and still needs exclusive use.
- **Window freezing during SQL:** operations use `SwingWorker`. Controls stay disabled and closing waits until they finish, which prevents overlapping work.
- **Save failure:** the GUI reuses the existing rollback and keeps the form for a correction or a retry. A test with a SQLite trigger proves the recovery.
- **JAR rebuilt without clean:** Shade replaces the output artifact. `maven-jar-plugin` with `forceCreation` recreates the base first, so incremental builds don't reprocess a shaded JAR.
- **Typed text treated as HTML by Swing:** labels and renderers turn off HTML interpretation. Messages use `JTextArea` with line wrapping.

No defect was found in the rules exercised by the original suite. That doesn't prove there are no bugs: concurrency with the terminal, large data volumes and screen-reader accessibility remain explicit limits.

## Files in this change

New production files:

- `DesktopMain.java`: JAR entry point, with the window as the default and `--terminal` for the menu; handles startup errors.
- `ArquivoAplicacao.java`: stable path and lock between desktop instances.
- `ui/JanelaBanco.java`: navigation, selection and event coordination.
- `ui/PainelResumo.java`, `PainelOperacao.java`, `PainelNovaConta.java`, `PainelHistorico.java`: screens.
- `ui/ModeloHistorico.java`: adapts the statement to the table.
- `ui/TarefasUi.java`: asynchronous execution and in-progress control.
- `ui/Tema.java`, `Componentes.java`, `IconeLinha.java`, `PainelRolavel.java`: look and shared components.

New tests: `ArquivoAplicacaoTest`, `DesktopMainTest`, `ui/ModeloHistoricoTest`, `ui/JanelaBancoTest`.

Build and delivery: `pom.xml` (FlatLaf, graphical manifest, UI profile, packaging), `.gitignore` (package and lock), `scripts/package-macos.sh`, `src/packaging/` (icon and package notes), `scripts/demo/` (demo recording) and CI with the Swing flows.

The original production classes and tests kept their logic. Binaries, databases, build cache and the `.app` stay out of Git.
