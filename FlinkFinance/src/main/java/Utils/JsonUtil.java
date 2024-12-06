package Utils;

import Dto.TradeData;
import Dto.TradingOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String convertTransactionToJson(TradeData tradedata) {
        try {
            return objectMapper.writeValueAsString(tradedata);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

}