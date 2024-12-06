package Deserializer;

import Dto.TradeData;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JSONValueDeserializationSchema implements DeserializationSchema<TradeData> {

    private final ObjectMapper objectMapper;

    public JSONValueDeserializationSchema() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TradeData deserialize(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            System.err.println("Received empty message.");
            return null; // Return null for empty messages
        }
        String rawJson = new String(message);
        try {
            TradeData tradeData = objectMapper.readValue(rawJson, TradeData.class);
            return tradeData;
        } catch (Exception e) {
            // Improved logging for debugging
            System.err.println("Failed to deserialize message: " + rawJson);
            e.printStackTrace();
            return null; // Return null for invalid messages
        }
    }

    @Override
    public boolean isEndOfStream(TradeData tradeData) {
        return false;
    }

    @Override
    public TypeInformation<TradeData> getProducedType() {
        return TypeInformation.of(TradeData.class);
    }
}
