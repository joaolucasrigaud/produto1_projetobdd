package br.edu.produto1;

import static br.edu.produto1.TipoEntidade.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executável independente no Eclipse; mantém os dados para inspeção no Compass. */
public final class Demonstracao {
    private Demonstracao() { }

    public static void main(String[] args) {
        String uri = ConexaoMongo.ambiente("MONGODB_URI", "mongodb://localhost:27017");
        String nomeBanco = "produto1_demo_" + new ObjectId().toHexString();
        try (var conexao = new ConexaoMongo(uri, nomeBanco)) {
            System.out.println("=== Demonstração com MongoDB real ===\nBanco: " + nomeBanco);
            CadastroService service = new CadastroService(conexao.banco());
            Map<TipoEntidade, Document> cadastros = new LinkedHashMap<>();
            cadastros.put(CURSO, service.criar(CURSO, new Document("nome", "Sistemas de Informação")));
            cadastros.put(TURMA, service.criar(TURMA, new Document("nome", "2026.1")));
            cadastros.put(PERIODO, service.criar(PERIODO, new Document("nome", "2026/1")));
            cadastros.put(ALUNO, service.criar(ALUNO, new Document("nome", "Yuri Anônimo")
                    .append("ra", "12356").append("turma", id(cadastros, TURMA))));
            cadastros.put(DESAFIO, service.criar(DESAFIO, new Document("nome", "Estrutura de Banco de Dados")
                    .append("curso", id(cadastros, CURSO)).append("periodo", id(cadastros, PERIODO))
                    .append("turma", id(cadastros, TURMA)).append("alunos", List.of(id(cadastros, ALUNO)))));

            for (var item : cadastros.entrySet()) {
                TipoEntidade tipo = item.getKey();
                ObjectId id = item.getValue().getObjectId("_id");
                System.out.println("\nCREATE / READ por ID: " + tipo.rotulo());
                ConsoleApp.mostrar(service.buscar(tipo, id));
                Document alterado = new Document(item.getValue());
                alterado.put("nome", alterado.getString("nome") + " (atualizado)");
                System.out.println("UPDATE: " + tipo.rotulo());
                ConsoleApp.mostrar(service.atualizar(tipo, id, alterado));
                verificar(service.buscar(tipo, id).getString("nome").endsWith("(atualizado)"), "UPDATE " + tipo);

                // A exclusão usa uma cópia descartável, mantendo o cenário principal consultável.
                Document copia = new Document(item.getValue());
                copia.put("nome", "Registro temporário para exclusão");
                if (tipo == ALUNO) copia.put("ra", "TEMP-12356");
                ObjectId temporario = service.criar(tipo, copia).getObjectId("_id");
                service.excluir(tipo, temporario);
                verificar(conexao.banco().getCollection(tipo.colecao())
                        .countDocuments(new Document("_id", temporario)) == 0, "DELETE " + tipo);
                System.out.println("DELETE confirmado no MongoDB: " + temporario);
                System.out.println("READ / listagem: " + tipo.rotulo());
                ConsoleApp.mostrar(service.listar(tipo));
            }

            System.out.println("\nAlunos da turma:");
            ConsoleApp.mostrar(service.alunosDaTurma(id(cadastros, TURMA)));
            System.out.println("\nAlunos do desafio:");
            ConsoleApp.mostrar(service.alunosDoDesafio(id(cadastros, DESAFIO)));
            verificar(service.alunosDaTurma(id(cadastros, TURMA)).size() == 1, "Alunos por turma");
            verificar(service.alunosDoDesafio(id(cadastros, DESAFIO)).size() == 1, "Alunos por desafio");
            System.out.println("\nSUCESSO: CRUD das cinco entidades e consultas de relacionamentos verificados.");
            System.out.println("Dados preservados para inspeção no banco: " + nomeBanco);
        }
    }

    private static ObjectId id(Map<TipoEntidade, Document> dados, TipoEntidade tipo) {
        return dados.get(tipo).getObjectId("_id");
    }

    private static void verificar(boolean condicao, String operacao) {
        if (!condicao) throw new IllegalStateException("Falha na verificação: " + operacao);
    }
}
