package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.json.Null;
import dev.vality.damsel.json.Value;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {
        MetadataDeserializer.class,
        ObjectMapper.class
})
@SpringBootTest
public class MetadataDeserializerTest {

    @Autowired
    private MetadataDeserializer metadataDeserializer;

    @Test
    public void deserialize() {
        Value value = Value.obj(Map.of(
                "bool", Value.b(true),
                "int", Value.i(1),
                "float", Value.flt(1.2),
                "string", Value.str("keksik"),
                "array", Value.arr(List.of(Value.i(12), Value.nl(new Null()))),
                "nested", Value.obj(Map.of("nestedStr", Value.str("nestedKeks")))));
        Object object = metadataDeserializer.deserialize(value);
        assertTrue(object instanceof Map);
        Map map = (Map) object;
        assertEquals(true, map.get("bool"));
        assertEquals(1, map.get("int"));
        assertEquals("keksik", map.get("string"));
        Object array = map.get("array");
        assertTrue(array instanceof List);
        assertEquals(12, ((List) array).get(0));
        Object nestedObject = map.get("nested");
        assertTrue(nestedObject instanceof Map);
        assertEquals("nestedKeks", ((Map) nestedObject).get("nestedStr"));
    }
}
