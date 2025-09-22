package com.studytracker.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * Custom deserializer for Canvas API submission field.
 * Handles cases where Canvas returns either a submission object or boolean false.
 */
public class SubmissionDeserializer extends JsonDeserializer<PlannerItemDto.SubmissionDto> {

    @Override
    public PlannerItemDto.SubmissionDto deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException, JsonProcessingException {
        
        JsonNode node = p.getCodec().readTree(p);
        
        // If the node is a boolean (false), return null
        if (node.isBoolean()) {
            return null;
        }
        
        // If it's an object, deserialize normally
        if (node.isObject()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.treeToValue(node, PlannerItemDto.SubmissionDto.class);
        }
        
        // For any other case, return null
        return null;
    }
}