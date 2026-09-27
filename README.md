# Produto 1 — Projeto de Banco de Dados

João Lucas Rigaud Silva

Aplicação Java de console para gerenciar cursos, turmas, períodos, alunos e desafios acadêmicos em MongoDB. Executa diretamente no Eclipse, sem interface gráfica ou API. Todas as entidades possuem cadastro, listagem, consulta por identificador, atualização e exclusão.

## Tecnologias e estrutura

- Java 17
- MongoDB Community 8.0 
- Driver oficial MongoDB Java Sync 5.11.1.
- Maven 3.9.9
- JUnit 5

src/main/java/br/edu/produto1/
  Main.java               Entrada do menu interativo
  ConsoleApp.java         Entrada de dados e exibição dos documentos
  TipoEntidade.java       Entidades e nomes das coleções
  ConexaoMongo.java       Configuração e encerramento da conexão
  CadastroService.java    CRUD, consultas e regras dos relacionamentos
  Demonstracao.java       Demonstração automática com dados de exemplo
src/test/java/br/edu/produto1/
  CadastroServiceTest.java  Testes sem servidor
  CadastroServiceIT.java    Testes contra MongoDB real
scripts/iniciar-mongodb.ps1  MongoDB portátil opcional para Windows
docs/consultas-mongosh.js    Consultas para conferir o banco
compose.yaml                MongoDB local opcional via Docker
```

As entidades são representadas diretamente por `org.bson.Document`, estrutura de chave/valor do driver. `TipoEntidade` seleciona a coleção e `CadastroService` centraliza as regras, evitando repetir o mesmo CRUD para cinco coleções. `ConsoleApp` cuida da interação, sem misturar leitura de teclado com persistência.

## Executar pelo terminal

Requer JDK no PATH (ou `JAVA_HOME`). O Maven Wrapper baixa o Maven na primeira execução, sem instalação global. No Windows:

powershell
.\mvnw.cmd compile exec:java


Se aparecer `release version 17 not supported`, confira `JAVA_HOME`: o Maven dá preferência a essa variável, mesmo se `java -version` mostrar um JDK mais novo no PATH. Nesta máquina, o JDK 25 está instalado e pode ser selecionado apenas para o terminal atual:

powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
.\mvnw.cmd -v


Em outro computador, substitua pelo caminho real de um JDK 17 ou superior. No Eclipse, selecione esse JDK nas configurações de Java e da execução.

## Uso e funcionalidades

O menu principal oferece Curso, Turma, Período, Aluno, Desafio, alunos de uma turma e alunos de um desafio. Cada submenu de entidade tem as cinco ações: cadastrar, listar todos, consultar por ID, atualizar e excluir.

Uma sequência de cadastro útil é:

1. Curso: `Sistemas de Informação`.
2. Turma: `2026.1`.
3. Período: `2026/1`.
4. Aluno: `Yuri Anônimo`, RA `12356`, selecionando a turma.
5. Desafio: `Estrutura de Banco de Dados`, selecionando curso, período, turma e alunos.
