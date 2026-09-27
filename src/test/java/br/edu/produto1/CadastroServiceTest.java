package br.edu.produto1;

import static org.junit.jupiter.api.Assertions.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class CadastroServiceTest {
    @Test void converteIdComEspacos() {
        ObjectId esperado = new ObjectId();
        assertEquals(esperado, CadastroService.id("  " + esperado.toHexString() + "  "));
    }

    @Test void rejeitaIdsInvalidos() {
        for (String entrada : new String[] {null, "", "123", "zzzzzzzzzzzzzzzzzzzzzzzz"}) {
            assertThrows(IllegalArgumentException.class, () -> CadastroService.id(entrada));
        }
    }
}
