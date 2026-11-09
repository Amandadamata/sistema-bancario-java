# Records the terminal demo. From the repository root, after ./mvnw package:
#   vhs docs/assets/demo.tape
# Output is 1080p for video; docs/assets/demo-terminal.gif is a smaller copy made with ffmpeg.
Output target/demo-terminal.mp4
Require java
Set Shell bash
Set FontSize 26
Set Width 1920
Set Height 1080
Set Padding 36
Set Framerate 30
Set TypingSpeed 45ms
Set Theme { "name": "VetGreen", "black": "#1B4332", "red": "#F4A6A6", "green": "#A8D5BA", "yellow": "#F6E3A1", "blue": "#9CC9E6", "magenta": "#D7B8E8", "cyan": "#A6E3D7", "white": "#D8F3DC", "brightBlack": "#52796F", "brightRed": "#F7B9B9", "brightGreen": "#B7E4C7", "brightYellow": "#F9EBB8", "brightBlue": "#B3D7EE", "brightMagenta": "#E2CCEF", "brightCyan": "#BDEBE2", "brightWhite": "#FFFFFF", "background": "#1B4332", "foreground": "#D8F3DC", "selection": "#2D6A4F", "cursor": "#A8D5BA" }

Hide
Type "export PS1='$ ' LINHA_DB=/tmp/bank-demo.db && rm -f /tmp/bank-demo.db /tmp/bank-demo.db.lock && clear" Enter
Sleep 500ms
Show

Type "java -jar target/linha-banking-system-1.4.0.jar --terminal" Sleep 500ms Enter
Sleep 2s
# Open a checking account with an R$ 200 overdraft limit
Type "1" Enter Sleep 500ms
Type "Bruno Lima" Enter Sleep 400ms
Type "246.813.579-00" Enter Sleep 2s
Type "246.813.579-28" Enter Sleep 400ms
Type "1" Enter Sleep 400ms
Type "200" Enter Sleep 1.8s
# Open a savings account
Type "1" Enter Sleep 500ms
Type "Ana Souza" Enter Sleep 400ms
Type "135.792.468-28" Enter Sleep 400ms
Type "2" Enter Sleep 1.8s
# Deposit
Type "2" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "100.00" Enter Sleep 1.8s
# Withdraw more than the balance: the overdraft covers it
Type "3" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "250" Enter Sleep 1.8s
# Beyond balance + limit: refused, nothing changes
Type "3" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "100" Enter Sleep 2.8s
# Transfer to the savings account
Type "4" Enter Sleep 400ms
Type "1000" Enter Sleep 300ms
Type "1001" Enter Sleep 300ms
Type "50" Enter Sleep 1.8s
# Statement
Type "5" Enter Sleep 400ms
Type "1000" Enter Sleep 5s
# Quit, reopen on the same file: the data is still there
Type "0" Enter Sleep 1.2s
Type "clear" Enter Sleep 300ms
Type "java -jar target/linha-banking-system-1.4.0.jar --terminal" Sleep 400ms Enter
Sleep 1.8s
Type "6" Enter Sleep 4s
Type "0" Enter Sleep 2s
