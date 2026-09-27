package br.edu.produto1;

import static br.edu.produto1.TipoEntidade.*;
import static org.junit.jupiter.api.Assertions.*;
import com.mongodb.MongoWriteException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;

/** Testa o driver contra um servidor real. Cada teste cria e remove só seu próprio banco. */
class CadastroServiceIT {
    private ConexaoMongo conexao;
    private CadastroService service;
    private ObjectId curso, turma, periodo, aluno;

    @BeforeEach void preparar() {
        conexao = new ConexaoMongo(ConexaoMongo.ambiente("MONGODB_URI", "mongodb://localhost:27017"),
                "produto1_test_" + new ObjectId().toHexString());
        service = new CadastroService(conexao.banco());
        curso = service.criar(CURSO, new Document("nome", "SI")).getObjectId("_id");
        turma = service.criar(TURMA, new Document("nome", "2026.1")).getObjectId("_id");
        periodo = service.criar(PERIODO, new Document("nome", "2026/1")).getObjectId("_id");
        aluno = service.criar(ALUNO, dados(ALUNO)).getObjectId("_id");
    }

    @AfterEach void limpar() {
        if (conexao != null) {
            try { conexao.banco().drop(); } finally { conexao.close(); }
        }
    }

    private Document dados(TipoEntidade tipo) {
        Document doc = new Document("nome", "Registro");
        if (tipo == ALUNO) doc.append("ra", "0012356").append("turma", turma);
        if (tipo == DESAFIO) doc.append("curso", curso).append("periodo", periodo)
                .append("turma", turma).append("alunos", List.of(aluno));
        return doc;
    }

    @ParameterizedTest @EnumSource(TipoEntidade.class)
    void crudCompletoPersisteNoServidor(TipoEntidade tipo) {
        Document novo = dados(tipo);
        if (tipo == ALUNO) novo.put("ra", "00009");
        ObjectId id = service.criar(tipo, novo).getObjectId("_id");
        assertEquals("Registro", service.buscar(tipo, id).getString("nome"));
        assertTrue(service.listar(tipo).stream().anyMatch(d -> id.equals(d.getObjectId("_id"))));
        novo.put("nome", "Alterado");
        service.atualizar(tipo, id, novo);
        // Uma segunda conexão garante que a leitura não é apenas estado em memória.
        try (var outra = new ConexaoMongo(ConexaoMongo.ambiente("MONGODB_URI", "mongodb://localhost:27017"),
                conexao.banco().getName())) {
            assertEquals("Alterado", outra.banco().getCollection(tipo.colecao())
                    .find(new Document("_id", id)).first().getString("nome"));
        }
        service.excluir(tipo, id);
        assertThrows(IllegalArgumentException.class, () -> service.buscar(tipo, id));
    }

    @Test void consultaAlunosSomenteDaTurmaEDoDesafio() {
        ObjectId outra = service.criar(TURMA, new Document("nome", "Outra")).getObjectId("_id");
        service.criar(ALUNO, new Document("nome", "Outro").append("ra", "2").append("turma", outra));
        ObjectId desafio = service.criar(DESAFIO, dados(DESAFIO)).getObjectId("_id");
        assertEquals(List.of(aluno), service.alunosDaTurma(turma).stream().map(d -> d.getObjectId("_id")).toList());
        assertEquals(List.of(aluno), service.alunosDoDesafio(desafio).stream().map(d -> d.getObjectId("_id")).toList());
    }

    @Test void indiceImpedeRaDuplicadoNaCriacaoENaAtualizacao() {
        assertThrows(MongoWriteException.class, () -> service.criar(ALUNO, dados(ALUNO)));
        Document novo = dados(ALUNO).append("ra", "2");
        ObjectId id = service.criar(ALUNO, novo).getObjectId("_id");
        assertThrows(MongoWriteException.class, () -> service.atualizar(ALUNO, id, dados(ALUNO)));
        assertEquals("2", service.buscar(ALUNO, id).getString("ra"));
        assertEquals("0012356", service.buscar(ALUNO, aluno).getString("ra"));
    }

    @ParameterizedTest @EnumSource(TipoEntidade.class)
    void rejeitaNomeVazioEIdsInexistentes(TipoEntidade tipo) {
        assertThrows(IllegalArgumentException.class, () -> service.criar(tipo, dados(tipo).append("nome", "  ")));
        ObjectId ausente = new ObjectId();
        assertThrows(IllegalArgumentException.class, () -> service.buscar(tipo, ausente));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(tipo, ausente, dados(tipo)));
        assertThrows(IllegalArgumentException.class, () -> service.excluir(tipo, ausente));
    }

    @Test void rejeitaReferenciasInexistentes() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(ALUNO, dados(ALUNO).append("turma", new ObjectId())));
        for (String campo : List.of("curso", "periodo", "turma")) {
            assertThrows(IllegalArgumentException.class, () -> service.criar(DESAFIO, dados(DESAFIO).append(campo, new ObjectId())));
        }
        assertThrows(IllegalArgumentException.class, () -> service.criar(DESAFIO,
                dados(DESAFIO).append("alunos", List.of(new ObjectId()))));
    }

    @Test void rejeitaAlunoDeOutraTurmaEAlteracaoIncoerenteDoDesafio() {
        ObjectId outra = service.criar(TURMA, new Document("nome", "Outra")).getObjectId("_id");
        assertThrows(IllegalArgumentException.class, () -> service.criar(DESAFIO, dados(DESAFIO).append("turma", outra)));
        ObjectId desafio = service.criar(DESAFIO, dados(DESAFIO)).getObjectId("_id");
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(DESAFIO, desafio, dados(DESAFIO).append("turma", outra)));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(ALUNO, aluno, dados(ALUNO).append("turma", outra)));
        service.atualizar(DESAFIO, desafio, dados(DESAFIO).append("alunos", List.of()));
        assertEquals(outra, service.atualizar(ALUNO, aluno, dados(ALUNO).append("turma", outra)).getObjectId("turma"));
    }

    @Test void bloqueiaExclusoesReferenciadasELiberaAposRemoverVinculos() {
        ObjectId desafio = service.criar(DESAFIO, dados(DESAFIO)).getObjectId("_id");
        assertThrows(IllegalArgumentException.class, () -> service.excluir(CURSO, curso));
        assertThrows(IllegalArgumentException.class, () -> service.excluir(PERIODO, periodo));
        assertThrows(IllegalArgumentException.class, () -> service.excluir(TURMA, turma));
        assertThrows(IllegalArgumentException.class, () -> service.excluir(ALUNO, aluno));
        service.excluir(DESAFIO, desafio);
        assertThrows(IllegalArgumentException.class, () -> service.excluir(TURMA, turma));
        service.excluir(ALUNO, aluno);
        service.excluir(TURMA, turma);
        service.excluir(PERIODO, periodo);
        service.excluir(CURSO, curso);
        for (TipoEntidade tipo : TipoEntidade.values()) assertTrue(service.listar(tipo).isEmpty());
    }

    @Test void removeIdsRepetidosEAceitaDesafioSemAlunos() {
        ObjectId desafio = service.criar(DESAFIO, dados(DESAFIO).append("alunos", List.of(aluno, aluno))).getObjectId("_id");
        assertEquals(1, service.buscar(DESAFIO, desafio).getList("alunos", ObjectId.class).size());
        service.atualizar(DESAFIO, desafio, dados(DESAFIO).append("alunos", List.of()));
        assertTrue(service.alunosDoDesafio(desafio).isEmpty());
    }
}
