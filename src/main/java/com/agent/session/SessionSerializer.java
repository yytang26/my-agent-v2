package com.agent.session;

import com.agent.memory.Message;
import com.agent.memory.MessageRole;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class SessionSerializer {

    private final ObjectMapper objectMapper;

    public SessionSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // 注册 Message record 的自定义序列化/反序列化模块
        SimpleModule messageModule = new SimpleModule();
        messageModule.addSerializer(Message.class, new JsonSerializer<>() {
            @Override
            public void serialize(Message value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                gen.writeStartObject();
                gen.writeStringField("role", value.role().name());
                gen.writeStringField("content", value.content());
                gen.writeNumberField("timestamp", value.timestamp());
                gen.writeEndObject();
            }
        });
        messageModule.addDeserializer(Message.class, new com.fasterxml.jackson.databind.JsonDeserializer<>() {
            @Override
            public Message deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                JsonNode node = mapper.readTree(p);
                String roleStr = node.get("role").asText();
                String content = node.get("content").asText();
                long timestamp = node.has("timestamp") ? node.get("timestamp").asLong() : System.currentTimeMillis();
                return new Message(MessageRole.valueOf(roleStr), content, timestamp);
            }
        });
        this.objectMapper.registerModule(messageModule);
    }

    public void save(Session session, Path filePath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), session);
    }

    public Session load(Path filePath) throws IOException {
        return objectMapper.readValue(filePath.toFile(), Session.class);
    }
}
