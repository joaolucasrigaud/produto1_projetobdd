package br.edu.produto1;

/** Cada entidade tem sua própria coleção e o mesmo contrato de CRUD. */
public enum TipoEntidade {
    CURSO("Curso", "cursos"), TURMA("Turma", "turmas"),
    PERIODO("Período", "periodos"), ALUNO("Aluno", "alunos"), DESAFIO("Desafio", "desafios");

    private final String rotulo;
    private final String colecao;

    TipoEntidade(String rotulo, String colecao) {
        this.rotulo = rotulo;
        this.colecao = colecao;
    }

    public String rotulo() { return rotulo; }
    public String colecao() { return colecao; }
}
