package com.vijay.todo_management.dto;

/** Preserve the existing Jackson 2 persistence/Java contract at the Jackson 3 HTTP boundary. */
public final class TodoJsonBridge {
    private TodoJsonBridge() {}
    private static final com.fasterxml.jackson.databind.ObjectMapper STORAGE =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private static final tools.jackson.databind.json.JsonMapper HTTP =
            tools.jackson.databind.json.JsonMapper.builder().build();

    public static class Reader extends tools.jackson.databind.ValueDeserializer<com.fasterxml.jackson.databind.JsonNode> {
        @Override
        public com.fasterxml.jackson.databind.JsonNode deserialize(tools.jackson.core.JsonParser parser,
                tools.jackson.databind.DeserializationContext context) {
            try {
                return STORAGE.readTree(context.readTree(parser).toString());
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new com.vijay.todo_management.exception.BadRequestException("Invalid description JSON");
            }
        }
    }
    public static class Writer extends tools.jackson.databind.ValueSerializer<com.fasterxml.jackson.databind.JsonNode> {
        @Override
        public void serialize(com.fasterxml.jackson.databind.JsonNode value, tools.jackson.core.JsonGenerator generator,
                tools.jackson.databind.SerializationContext context) {
            context.writeTree(generator, HTTP.readTree(value.toString()));
        }
    }
}
