package br.edu.produto1;

import static br.edu.produto1.TipoEntidade.*;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Regras de negócio e persistência usando documentos BSON do driver oficial. */
public final class CadastroService {
    private final MongoDatabase banco;

    public CadastroService(MongoDatabase banco) {
        this.banco = banco;
        colecao(ALUNO).createIndex(Indexes.ascending("ra"), new IndexOptions().unique(true));
        colecao(ALUNO).createIndex(Indexes.ascending("turma"));
        for (String campo : List.of("curso", "periodo", "turma", "alunos")) {
            colecao(DESAFIO).createIndex(Indexes.ascending(campo));
        }
    }

    private MongoCollection<Document> colecao(TipoEntidade tipo) {
        return banco.getCollection(tipo.colecao());
    }

    public static ObjectId id(String texto) {
        if (texto == null || !ObjectId.isValid(texto.trim())) {
            throw new IllegalArgumentException("ID inválido: use os 24 caracteres hexadecimais exibidos no cadastro.");
        }
        return new ObjectId(texto.trim());
    }

    public Document criar(TipoEntidade tipo, Document dados) {
        Document novo = validar(tipo, dados);
        novo.put("_id", new ObjectId());
        colecao(tipo).insertOne(novo);
        return buscar(tipo, novo.getObjectId("_id"));
    }

    public List<Document> listar(TipoEntidade tipo) {
        return colecao(tipo).find().sort(Sorts.ascending("nome", "_id")).into(new ArrayList<>());
    }

    public Document buscar(TipoEntidade tipo, ObjectId id) {
        if (id == null) throw new IllegalArgumentException("Informe o ID.");
        Document encontrado = colecao(tipo).find(eq("_id", id)).first();
        if (encontrado == null) throw new IllegalArgumentException(tipo.rotulo() + " não encontrado: " + id);
        return encontrado;
    }

    /** Atualização completa dos campos; o identificador é preservado. */
    public Document atualizar(TipoEntidade tipo, ObjectId id, Document dados) {
        Document anterior = buscar(tipo, id);
        Document novo = validar(tipo, dados);
        if (tipo == ALUNO && !anterior.getObjectId("turma").equals(novo.getObjectId("turma"))
                && colecao(DESAFIO).countDocuments(eq("alunos", id)) > 0) {
            throw new IllegalArgumentException("Remova o aluno dos desafios antes de mudar sua turma.");
        }
        novo.put("_id", id);
        if (colecao(tipo).replaceOne(eq("_id", id), novo).getMatchedCount() == 0) {
            throw new IllegalArgumentException("Registro não encontrado para atualização.");
        }
        return buscar(tipo, id);
    }

    public void excluir(TipoEntidade tipo, ObjectId id) {
        buscar(tipo, id);
        boolean utilizado = switch (tipo) {
            case CURSO -> colecao(DESAFIO).countDocuments(eq("curso", id)) > 0;
            case PERIODO -> colecao(DESAFIO).countDocuments(eq("periodo", id)) > 0;
            case TURMA -> colecao(ALUNO).countDocuments(eq("turma", id)) > 0
                    || colecao(DESAFIO).countDocuments(eq("turma", id)) > 0;
            case ALUNO -> colecao(DESAFIO).countDocuments(eq("alunos", id)) > 0;
            case DESAFIO -> false;
        };
        if (utilizado) throw new IllegalArgumentException("Exclusão bloqueada: remova ou altere os vínculos deste registro primeiro.");
        if (colecao(tipo).deleteOne(eq("_id", id)).getDeletedCount() == 0) {
            throw new IllegalArgumentException("Registro não encontrado para exclusão.");
        }
    }

    public List<Document> alunosDaTurma(ObjectId turma) {
        buscar(TURMA, turma);
        return colecao(ALUNO).find(eq("turma", turma)).sort(Sorts.ascending("nome")).into(new ArrayList<>());
    }

    public List<Document> alunosDoDesafio(ObjectId desafio) {
        List<ObjectId> ids = buscar(DESAFIO, desafio).getList("alunos", ObjectId.class);
        return colecao(ALUNO).find(in("_id", ids)).sort(Sorts.ascending("nome")).into(new ArrayList<>());
    }

    private Document validar(TipoEntidade tipo, Document dados) {
        if (dados == null) throw new IllegalArgumentException("Informe os dados do cadastro.");
        Document resultado = new Document("nome", texto(dados, "nome"));
        if (tipo == ALUNO) {
            resultado.append("ra", texto(dados, "ra"));
            resultado.append("turma", referencia(dados, "turma", TURMA));
        } else if (tipo == DESAFIO) {
            resultado.append("curso", referencia(dados, "curso", CURSO));
            resultado.append("periodo", referencia(dados, "periodo", PERIODO));
            ObjectId turma = referencia(dados, "turma", TURMA);
            resultado.append("turma", turma);
            if (!(dados.get("alunos") instanceof List<?> alunos)) {
                throw new IllegalArgumentException("Informe a lista de alunos, mesmo que vazia.");
            }
            var ids = new LinkedHashSet<ObjectId>();
            for (Object valor : alunos) {
                if (!(valor instanceof ObjectId alunoId)) throw new IllegalArgumentException("ID de aluno inválido.");
                Document aluno = buscar(ALUNO, alunoId);
                if (!turma.equals(aluno.getObjectId("turma"))) {
                    throw new IllegalArgumentException("Todos os alunos do desafio devem pertencer à turma informada.");
                }
                ids.add(alunoId);
            }
            resultado.append("alunos", new ArrayList<>(ids));
        }
        return resultado;
    }

    private ObjectId referencia(Document dados, String campo, TipoEntidade tipo) {
        if (!(dados.get(campo) instanceof ObjectId valor)) {
            throw new IllegalArgumentException("Informe um ID válido para " + campo + ".");
        }
        buscar(tipo, valor);
        return valor;
    }

    private String texto(Document dados, String campo) {
        if (!(dados.get(campo) instanceof String valor) || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        }
        return valor.trim();
    }
}
