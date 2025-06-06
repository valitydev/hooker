package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.damsel.json.Null;
import dev.vality.damsel.json.Value;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


class MetadataDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final MetadataDeserializer metadataDeserializer = new MetadataDeserializer(objectMapper);

    @Test
    void deserialize() {
        Value value = Value.obj(Map.of(
                "bool", Value.b(true),
                "int", Value.i(1),
                "float", Value.flt(1.2),
                "string", Value.str("keksik"),
                "array", Value.arr(List.of(Value.i(12), Value.nl(new Null()))),
                "nested", Value.obj(Map.of("nestedStr", Value.str("nestedKeks")))));
        Object object = metadataDeserializer.deserialize(value);
        assertInstanceOf(Map.class, object);
        Map map = (Map) object;
        assertEquals(true, map.get("bool"));
        assertEquals(1, map.get("int"));
        assertEquals("keksik", map.get("string"));
        Object array = map.get("array");
        assertInstanceOf(List.class, array);
        assertEquals(12, ((List) array).get(0));
        Object nestedObject = map.get("nested");
        assertInstanceOf(Map.class, nestedObject);
        assertEquals("nestedKeks", ((Map) nestedObject).get("nestedStr"));
    }
}
