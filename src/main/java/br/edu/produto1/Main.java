package br.edu.produto1;

import com.mongodb.MongoException;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args.length == 1 && args[0].equals("--demo")) Demonstracao.main(new String[0]);
            else throw new IllegalArgumentException("Uso: Main [--demo]");
            return;
        }
        String uri = ConexaoMongo.ambiente("MONGODB_URI", "mongodb://localhost:27017");
        String banco = ConexaoMongo.ambiente("MONGODB_DATABASE", "produto1_academico");
        try (var conexao = new ConexaoMongo(uri, banco)) {
            var service = new CadastroService(conexao.banco());
            System.out.println("Conectado ao banco: " + banco);
            new ConsoleApp(service).executar();
        } catch (MongoException | IllegalArgumentException e) {
            System.err.println("Não foi possível iniciar. Verifique se o MongoDB está ativo, MONGODB_URI,"
                    + " MONGODB_DATABASE e as permissões de acesso/índices.");
            System.exit(1);
        }
    }
}
