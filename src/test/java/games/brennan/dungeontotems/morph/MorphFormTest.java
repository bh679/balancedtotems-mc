package games.brennan.dungeontotems.morph;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphFormTest {

    private static MorphForm parse(String json) {
        return MorphForm.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void aFormWithoutSecondsLastsTheUsualThirty() {
        MorphForm form = parse("{\"pool\": \"land\", \"weight\": 5}");
        assertEquals(MorphForm.DEFAULT_SECONDS, form.seconds());
        assertEquals(600, form.ticks());
    }

    @Test
    void aFormKeepsItsOwnSeconds() {
        MorphForm form = parse("{\"pool\": \"water\", \"weight\": 3, \"seconds\": 45}");
        assertEquals(45, form.seconds());
        assertEquals(900, form.ticks());
    }

    @Test
    void secondsOutOfRangeAreRefused() {
        String zero = "{\"pool\": \"land\", \"weight\": 1, \"seconds\": 0}";
        assertTrue(MorphForm.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(zero)).error().isPresent());
    }

    @Test
    void everyShippedFormParses() {
        var stream = MorphFormTest.class.getResourceAsStream("/data/dungeontotems/data_maps/entity_type/morph_forms.json");
        var values = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("values");
        values.entrySet().forEach(entry -> {
            MorphForm form = MorphForm.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
            assertTrue(form.seconds() >= 1 && form.seconds() <= MorphForm.MAX_SECONDS, entry.getKey());
        });
    }
}
