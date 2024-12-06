package Dto;

public class TradeData {

    private String id;

    private String secType;

    private String lastTradePrice;

    private String tradingTime;

    private String tradingDate;

    public TradeData(String id, String secType, String lastTradePrice, String tradingTime, String tradingDate) {
        this.id = handleEmptyString(id);
        this.secType = handleEmptyString(secType);
        this.lastTradePrice = handleEmptyString(lastTradePrice);
        this.tradingTime = handleEmptyString(tradingTime);
        this.tradingDate = handleEmptyString(tradingDate);
    }

    public TradeData() {

    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = handleEmptyString(id);
    }

    public String getSecType() {
        return secType;
    }

    public void setSecType(String secType) {
        this.secType = handleEmptyString(secType);
    }

    public String getLastTradePrice() {
        return lastTradePrice;
    }

    public void setLastTradePrice(String lastTradePrice) {
        this.lastTradePrice = handleEmptyString(lastTradePrice);
    }

    public String getTradingTime() {
        return tradingTime;
    }

    public void setTradingTime(String tradingTime) {
        this.tradingTime = handleEmptyString(tradingTime);
    }

    public String getTradingDate() {
        return tradingDate;
    }

    public void setTradingDate(String tradingDate) {
        this.tradingDate = handleEmptyString(tradingDate);
    }

    private String handleEmptyString(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    @Override
    public String toString() {
        return "TradeData{" +
                "id='" + id + '\'' +
                ", secType='" + secType + '\'' +
                ", lastTradePrice='" + lastTradePrice + '\'' +
                ", tradingTime='" + tradingTime + '\'' +
                ", tradingDate='" + tradingDate + '\'' +
                '}';
    }
}
