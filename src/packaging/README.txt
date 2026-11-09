LINHA — BANKING SYSTEM (STUDY PROJECT)

Requires Java 17 or newer. Operations only change local, made-up balances;
no real money moves.

Open the desktop app (in a terminal, in this folder):
  java -jar Linha.jar

Open the terminal menu:
  java -jar Linha.jar --terminal

Where the data goes:
  Desktop on macOS:  ~/Library/Application Support/Linha/linha.db
  Desktop elsewhere: ~/.linha/linha.db
  Terminal menu:     data/linha.db (relative to the current folder)

The package has no accounts in it. Use test data only. CPF is Brazil's tax ID;
this synthetic one passes the validation: 246.813.579-28.
There is no login or encryption.

Quick tour:
  1. Open a checking account with a zero limit.
  2. Deposit 100.50.
  3. Try to withdraw 101.00: it is refused.
  4. Open another account, transfer 20.00 and check both statements.
  5. Close the app, open it again and see that everything is still saved.

Code and docs: https://github.com/Amandadamata/sistema-bancario-java
