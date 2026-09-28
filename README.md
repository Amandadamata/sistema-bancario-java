# Sistema Bancário em Java

Aplicação de linha de comando que simula operações bancárias básicas. O projeto foi desenvolvido
para praticar fundamentos de Java, orientação a objetos, validações, tratamento de exceções e
testes unitários.

## Funcionalidades

- Cadastro de contas corrente e poupança
- Depósitos e saques com validação de valores
- Transferências entre contas
- Consulta de saldo e extrato de transações
- Listagem de contas cadastradas
- Aplicação de rendimento em conta poupança

## Tecnologias

- Java 17
- Maven
- JUnit 5

## Conceitos praticados

- Encapsulamento, herança, abstração e polimorfismo
- Exceção personalizada para saldo insuficiente
- Validação de entradas no terminal
- Coleções e histórico de transações
- Testes unitários de regras de negócio

## Estrutura

```text
src/main/java/     Código-fonte da aplicação
src/test/java/     Testes unitários
pom.xml            Configuração do Maven
```

## Como executar

Requisitos: JDK 17 ou superior e Maven.

```bash
mvn clean package
java -jar target/sistema-bancario-java-1.0.0.jar
```

## Testes

```bash
mvn test
```

Os testes cobrem depósitos, saques, cheque especial, transferências, rendimento, tipos de conta e
validações de dados e valores.

## Status

Funcional para o escopo atual. Os dados são mantidos somente em memória e reiniciados a cada execução.

Nesta versão introdutória, os valores monetários usam `double`. Uma evolução planejada é adotar
`BigDecimal` com regras explícitas de arredondamento.
