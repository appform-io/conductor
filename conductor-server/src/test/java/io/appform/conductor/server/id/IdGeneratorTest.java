package io.appform.conductor.server.id;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class IdGeneratorTest {

    @Test
    void generateSuccess(){
        IdGenerator.initialize(new Random().nextInt(999));
        Id id = IdGenerator.generate("TXN");
        assertNotNull(id);
    }
}
