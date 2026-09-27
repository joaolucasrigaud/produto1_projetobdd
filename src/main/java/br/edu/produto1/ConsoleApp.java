package br.edu.produto1;

import static br.edu.produto1.TipoEntidade.*;
import com.mongodb.MongoException;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class ConsoleApp {
    private final CadastroService service;
    private final Scanner entrada = new Scanner(System.in);

    public ConsoleApp(CadastroService service) { this.service = service; }

    public void executar() {
        while (true) {
            try {
                System.out.println("\n=== Gestão Acadêmica ===\n1 Curso\n2 Turma\n3 Período\n4 Aluno\n5 Desafio"
                        + "\n6 Alunos de uma turma\n7 Alunos de um desafio\n0 Sair");
                String opcao = ler("Opção: ");
                if (opcao.equals("0")) return;
                switch (opcao) {
                    case "1", "2", "3", "4", "5" -> menuEntidade(TipoEntidade.values()[Integer.parseInt(opcao) - 1]);
                    case "6" -> mostrar(service.alunosDaTurma(escolher(TURMA)));
                    case "7" -> mostrar(service.alunosDoDesafio(escolher(DESAFIO)));
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("Entrada encerrada.");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Atenção: " + e.getMessage());
            } catch (MongoException e) {
                erroMongo(e);
            }
        }
    }

    private void menuEntidade(TipoEntidade tipo) {
        while (true) {
            try {
                System.out.println("\n--- " + tipo.rotulo() + " ---\n1 Cadastrar\n2 Listar todos\n3 Consultar por ID"
                        + "\n4 Atualizar\n5 Excluir\n0 Voltar");
                switch (ler("Opção: ")) {
                    case "0" -> { return; }
                    case "1" -> { mostrar(service.criar(tipo, dados(tipo))); System.out.println("Cadastro gravado no MongoDB."); }
                    case "2" -> mostrar(service.listar(tipo));
                    case "3" -> mostrar(service.buscar(tipo, lerId()));
                    case "4" -> {
                        ObjectId id = lerId();
                        mostrar(service.buscar(tipo, id));
                        System.out.println("Informe todos os campos atualizados (o ID será mantido).");
                        mostrar(service.atualizar(tipo, id, dados(tipo)));
                        System.out.println("Atualização gravada no MongoDB.");
                    }
                    case "5" -> {
                        ObjectId id = lerId();
                        mostrar(service.buscar(tipo, id));
                        if (ler("Confirma excluir? (s/N): ").equalsIgnoreCase("s")) {
                            service.excluir(tipo, id);
                            System.out.println("Excluído do MongoDB: " + id);
                        } else System.out.println("Exclusão cancelada.");
                    }
                    default -> System.out.println("Opção inválida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Atenção: " + e.getMessage());
            } catch (MongoException e) {
                erroMongo(e);
            }
        }
    }

    private Document dados(TipoEntidade tipo) {
        Document dados = new Document("nome", ler("Nome: "));
        if (tipo == ALUNO) {
            dados.append("ra", ler("RA (preserva zeros à esquerda): "));
            dados.append("turma", escolher(TURMA));
        } else if (tipo == DESAFIO) {
            dados.append("curso", escolher(CURSO));
            dados.append("periodo", escolher(PERIODO));
            ObjectId turma = escolher(TURMA);
            dados.append("turma", turma);
            System.out.println("Alunos disponíveis nesta turma:");
            mostrar(service.alunosDaTurma(turma));
            String texto = ler("IDs dos alunos separados por vírgula (Enter para nenhum): ");
            List<ObjectId> alunos = new ArrayList<>();
            if (!texto.isBlank()) for (String parte : texto.split(",", -1)) alunos.add(CadastroService.id(parte));
            dados.append("alunos", alunos);
        }
        return dados;
    }

    private ObjectId escolher(TipoEntidade tipo) {
        System.out.println("Selecione " + tipo.rotulo() + ":");
        List<Document> registros = service.listar(tipo);
        mostrar(registros);
        if (registros.isEmpty()) throw new IllegalArgumentException("Cadastre " + tipo.rotulo() + " primeiro.");
        ObjectId id = lerId();
        service.buscar(tipo, id);
        return id;
    }

    private ObjectId lerId() { return CadastroService.id(ler("ID: ")); }
    private String ler(String mensagem) { System.out.print(mensagem); return entrada.nextLine().trim(); }

    public static void mostrar(Document documento) {
        System.out.println(documento.toJson(JsonWriterSettings.builder().indent(true).build()));
    }

    public static void mostrar(List<Document> documentos) {
        if (documentos.isEmpty()) System.out.println("Nenhum registro encontrado.");
        documentos.forEach(ConsoleApp::mostrar);
        System.out.println("Total: " + documentos.size());
    }

    private void erroMongo(MongoException e) {
        System.out.println(e.getCode() == 11000 ? "Este RA já está cadastrado. Use um RA único."
                : "Falha ao acessar o MongoDB. Verifique a conexão e tente novamente.");
    }
}
