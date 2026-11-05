# Análise e evolução para desktop

## Origem e escopo

Estas notas registram a passagem do sistema de terminal (`ed9b02f`, versão 1.1.0) para o aplicativo desktop (versão 1.2.0): o que existia, quais riscos a janela trazia e como foram tratados.

## Inventário do código original

Foram lidos os 17 arquivos Java de produção, a suíte existente, o `pom.xml`, o wrapper, o workflow de CI, `.gitignore` e a documentação. Os métodos e atributos foram examinados por responsabilidade:

| Classe | Estado e métodos relevantes | Resultado da análise |
|---|---|---|
| `Banco` | Mapa de contas, próximo número, relógio e repositório; abrir contas, buscar/listar, depositar, sacar, transferir, render, capturar/gravar/desfazer/restaurar | Já centraliza gravação e rollback em memória. É a entrada usada pela GUI. |
| `Conta` | Número, titular, saldo, relógio e histórico; operações internas, registrar, conferir disponível, capturar/restaurar estado e histórico; getters | Valores positivos e centavos validados antes das alterações; extrato imutável para consumidores. |
| `ContaCorrente` | Limite; sobrescreve disponível, identificação e registro persistido | Saque pode exceder saldo próprio até o limite. Regra preservada, com distinção visual. |
| `ContaPoupanca` | Taxa, mês do último rendimento; aplicar, capturar/restaurar e serializar estado | Rendimento de 0,5%, arredondamento `HALF_EVEN`; mês consumido apenas se houve crédito. |
| `Cliente` | Nome e CPF; construtores, getters e `toString` mascarado | Nome obrigatório; validação de CPF delegada. |
| `Cpf` | Dígitos; fábrica `de`, cálculo de verificadores, formatação, máscara, igualdade e hash | Validação matemática e rejeição de repetições. Não valida identidade. |
| `Moeda` | Escala, arredondamento, regex e localidade; converter, formatar e validar | Sem `double`; aceita vírgula/ponto decimal e rejeita separador de milhar. |
| `Transacao` | Tipo, valor, saldo, data e conta relacionada; `valorComSinal` | Registro imutável, usado diretamente pelo modelo de tabela. |
| `TipoTransacao` | Descrição e indicador de crédito | Permite diferenciar entradas e saídas sem duplicar listas de operações. |
| `TipoConta` | Corrente ou poupança | Identificação persistida preservada. |
| `EstadoConta` | Saldo, tamanho do histórico e mês do rendimento | Snapshot interno para desfazer gravações malsucedidas. |
| `ContaPersistida` | Identificação, titular, limite, saldo, mês e histórico | Estrutura usada para transportar dados até o repositório; copia o histórico. |
| `RepositorioBanco` | `carregar`, `salvar`, fábrica sem persistência | Contrato independente de SQL. Usado também em testes de falha. |
| `RepositorioSqlite` | Caminho, versão e esquema; carregar, salvar, preparar arquivo, abrir conexão, criar esquema, gravar cliente/conta/transações, converter centavos | Transações SQL, prepared statements, chaves estrangeiras e validação do esquema. Sem mudança de tabelas. |
| `SaldoInsuficienteException` | Mensagem de erro | Exceção verificada e tratada na camada de apresentação. |
| `FalhaPersistenciaException` | Mensagem e causa | Falha de armazenamento traduzida sem anunciar sucesso. |
| `Main` | Banco, scanner e saída; menu, leitura, criação, operações, extrato/listagem e caminho | Interface de terminal isolada; preservada para comparação e regressão. |

O build original já usava Maven Wrapper e Shade para um JAR de terminal com SQLite incluído. Não foi necessário reestruturar os pacotes do domínio nem migrar dados. A execução inicial de `./mvnw verify` passou com 135 casos, sem falhas.

## Riscos observados e tratamento

- **Saldo versus cheque especial:** retirar a regra do limite quebraria o sistema original. A GUI distingue saldo próprio e disponível, e abre novas correntes com limite zero inicialmente.
- **Caminho relativo do banco:** no Finder, o diretório de trabalho não é garantido. O desktop usa uma pasta estável do usuário e aceita `SISTEMA_BANCARIO_DB` para dados antigos.
- **Acesso simultâneo:** o modelo original carrega uma cópia das contas em memória. Duas aplicações poderiam sobrescrever saldos com estados antigos. O desktop adquire uma reserva de arquivo antes de carregar. O terminal antigo não participa da reserva e continua exigindo uso exclusivo.
- **Bloqueio da janela durante SQL:** as operações usam `SwingWorker`. Os controles ficam desabilitados e o fechamento aguarda a conclusão, evitando trabalhos sobrepostos.
- **Falha de persistência:** a GUI reutiliza o rollback já implementado e mantém o formulário para correção ou nova tentativa. Um teste com trigger SQLite comprova a recuperação.
- **JAR recompilado sem clean:** o Shade substitui o artefato de saída. `maven-jar-plugin` com `forceCreation` recria a base antes do empacotamento, evitando reprocessar o JAR já sombreado em builds incrementais.
- **Texto digitado tratado como HTML pelo Swing:** rótulos e renderizadores desativam a interpretação HTML. Mensagens usam `JTextArea` com quebra de linha.

Não foi identificado defeito nas regras exercitadas pela suíte original. Isso não equivale a provar ausência de bugs: concorrência com o terminal, grandes volumes e acessibilidade com leitor de tela continuam limites explícitos.

## Arquivos da atualização

Novos arquivos de produção:

- `DesktopMain.java`: entrada do JAR, com a janela como padrão e `--terminal` para o menu; tratamento de erro de inicialização.
- `ArquivoAplicacao.java`: caminho estável e reserva entre instâncias desktop.
- `ui/JanelaBanco.java`: navegação, seleção e coordenação dos eventos.
- `ui/PainelResumo.java`, `PainelOperacao.java`, `PainelNovaConta.java`, `PainelHistorico.java`: telas.
- `ui/ModeloHistorico.java`: adaptação do extrato para a tabela.
- `ui/TarefasUi.java`: execução assíncrona e controle de operação em andamento.
- `ui/Tema.java`, `Componentes.java`, `IconeLinha.java`, `PainelRolavel.java`: aparência e componentes compartilhados.

Novos testes: `ArquivoAplicacaoTest`, `DesktopMainTest`, `ui/ModeloHistoricoTest`, `ui/JanelaBancoTest`.

Build e entrega: `pom.xml` (FlatLaf, versão 1.2.0, manifesto gráfico, perfil UI, empacotamento), `.gitignore` (pacote e reserva), `scripts/package-macos.sh`, `src/packaging/` (ícone e instruções do pacote), `scripts/demo/` (gravação das demonstrações) e o CI com os fluxos Swing.

Documentação: READMEs em inglês e português, este inventário, atualização dos documentos de arquitetura/decisões/testes e imagens e demonstrações em `docs/assets/`.

Os 17 arquivos de produção originais e os testes originais não foram modificados. Binários, banco de dados, cache de build e `.app` não entram no Git.
