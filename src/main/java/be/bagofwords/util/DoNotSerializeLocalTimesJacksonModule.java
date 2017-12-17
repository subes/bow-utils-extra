/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-12-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DoNotSerializeLocalTimesJacksonModule extends SimpleModule {

    public DoNotSerializeLocalTimesJacksonModule() {
        addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                throw new RuntimeException("LocalDate should be converted to actual date before sending it to the client");
            }
        });
        addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                throw new RuntimeException("LocalDateTime should be converted to actual date before sending it to the client");
            }
        });
    }
}
