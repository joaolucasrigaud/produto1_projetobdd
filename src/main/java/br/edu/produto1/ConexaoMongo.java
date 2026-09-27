package br.edu.produto1;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.concurrent.TimeUnit;

public final class ConexaoMongo implements AutoCloseable {
    private final MongoClient cliente;
    private final MongoDatabase banco;

    public ConexaoMongo(String uri, String nomeBanco) {
        var config = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(b -> b.serverSelectionTimeout(5, TimeUnit.SECONDS))
                .build();
        cliente = MongoClients.create(config);
        banco = cliente.getDatabase(nomeBanco);
        try {
            banco.runCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            cliente.close();
            throw e;
        }
    }

    public static String ambiente(String chave, String padrao) {
        String valor = System.getenv(chave);
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    public MongoDatabase banco() { return banco; }
    @Override public void close() { cliente.close(); }
}
