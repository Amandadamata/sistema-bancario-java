# Records docs/assets/demo.gif. Run from the repository root after ./mvnw package:
#   vhs docs/assets/demo.tape
Output docs/assets/demo.gif
Require java
Set Shell bash
Set FontSize 15
Set Width 1100
Set Height 760
Set Padding 24
Set TypingSpeed 35ms
Set Theme { "name": "VetGreen", "black": "#1B4332", "red": "#F4A6A6", "green": "#A8D5BA", "yellow": "#F6E3A1", "blue": "#9CC9E6", "magenta": "#D7B8E8", "cyan": "#A6E3D7", "white": "#D8F3DC", "brightBlack": "#52796F", "brightRed": "#F7B9B9", "brightGreen": "#B7E4C7", "brightYellow": "#F9EBB8", "brightBlue": "#B3D7EE", "brightMagenta": "#E2CCEF", "brightCyan": "#BDEBE2", "brightWhite": "#FFFFFF", "background": "#1B4332", "foreground": "#D8F3DC", "selection": "#2D6A4F", "cursor": "#A8D5BA" }

Hide
Type "export PS1='$ ' SISTEMA_BANCARIO_DB=/tmp/banco-demo.db && rm -f /tmp/banco-demo.db" Enter
Type "printf '1\nBruno Lima\n246.813.579-28\n1\n200\n1\nAna Souza\n135.792.468-28\n2\n0\n' | java -jar target/sistema-bancario-java-1.1.0.jar > /dev/null && clear" Enter
Sleep 2s
Show

Type "java -jar target/sistema-bancario-java-1.1.0.jar" Sleep 400ms Enter
Sleep 1.5s
# Deposit into checking account 1000 (overdraft limit R$ 200)
Type "2" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "100,00" Enter
Sleep 1.5s
# Withdraw more than the balance: the overdraft covers it
Type "3" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "250" Enter
Sleep 1.5s
# Beyond balance + limit: refused, nothing changes
Type "3" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "100" Enter
Sleep 2.5s
# Transfer to the savings account 1001
Type "4" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "1001" Enter Sleep 300ms
Type "50" Enter
Sleep 1.5s
# Statement
Type "5" Enter Sleep 400ms
Type "1000" Enter
Sleep 4.5s
Type "0" Enter
Sleep 1.5s
