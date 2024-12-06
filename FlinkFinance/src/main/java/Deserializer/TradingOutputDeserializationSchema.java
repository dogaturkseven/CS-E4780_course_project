package Deserializer;

import Dto.TradeData;
import Dto.TradingOutput;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TradingOutputDeserializationSchema implements DeserializationSchema<TradingOutput> {


    private final ObjectMapper objectMapper;

    public TradingOutputDeserializationSchema() {
        this.objectMapper = new ObjectMapper();
    }

    public TradingOutput deserialize(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            System.err.println("Received empty message.");
            return null; // Return null for empty messages
        }
        String rawJson = new String(message);
        try {
            TradingOutput tradingOutput = objectMapper.readValue(rawJson, TradingOutput.class);
            return tradingOutput;
        } catch (Exception e) {
            System.err.println("Failed to deserialize message: " + rawJson);
            e.printStackTrace();
            return null; // Return null for invalid messages
        }
    }

    @Override
    public boolean isEndOfStream(TradingOutput tradeData) {
        return false;
    }

    @Override
    public TypeInformation<TradingOutput> getProducedType() {
        return TypeInformation.of(TradingOutput.class);
    }
}
