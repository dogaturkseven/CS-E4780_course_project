package Dto;

import java.io.Serializable;

public class TradingOutput implements Serializable {
    private String symbol;
    private String tradingTime;
    private String tradingDate;
    private double lastTradePrice;
    private double ema38;
    private double ema100;
    private double prevEma38; 
    private double prevEma100; 
    private Integer eventCode;
    private String advisoryReason;

    public TradingOutput(String symbol, String tradingTime, String tradingDate, double lastTradePrice, double ema38, double ema100,
                         double prevEma38, double prevEma100, Integer eventCode, String advisoryReason) {
        this.symbol = symbol;
        this.tradingTime = tradingTime;
        this.tradingDate = tradingDate;
        this.lastTradePrice = lastTradePrice;
        this.ema38 = ema38;
        this.ema100 = ema100;
        this.prevEma38 = prevEma38;
        this.prevEma100 = prevEma100;
        this.eventCode = eventCode;
        this.advisoryReason = advisoryReason;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradingTime() {
        return tradingTime;
    }

    public void setTradingTime(String tradingTime) {
        this.tradingTime = tradingTime;
    }

    public String getTradingDate() {
        return tradingDate;
    }

    public void setTradingDate(String tradingDate) {
        this.tradingDate = tradingDate;
    }

    public double getLastTradePrice() {
        return lastTradePrice;
    }

    public void setLastTradePrice(double lastTradePrice) {
        this.lastTradePrice = lastTradePrice;
    }

    public double getEma38() {
        return ema38;
    }

    public void setEma38(double ema38) {
        this.ema38 = ema38;
    }

    public double getPrevEma38() {
        return prevEma38;
    }

    public void setPrevEma38(double ema38) {
        this.prevEma38 = prevEma38;
    }

    public double getEma100() {
        return ema100;
    }

    public void setEma100(double ema100) {
        this.ema100 = ema100;
    }

    public double getPrevEma100() {
        return prevEma100;
    }

    public void setPrevEma100(double ema100) {
        this.prevEma100 = prevEma100;
    }

    public Integer getEventCode() {
        return eventCode;
    }

    public void setEventCode(Integer eventCode) {
        this.eventCode = eventCode;
    }

    public String getAdvisoryReason() {
        return advisoryReason;
    }

    public void setAdvisoryReason(String advisoryReason) {
        this.advisoryReason = advisoryReason;
    }

    /**
     * Converts the TradingOutput object to InfluxDB Line Protocol format.
     * @return A string representing the object in Line Protocol.
     */
    public String toLineProtocol() {
        return String.format(
                "trading_output,symbol=%s alert=\"%s\",advisoryReason=\"%s\",lastTradePrice=%.2f,ema38=%.2f,ema100=%.2f %d",
                symbol,
                eventCode != null ? eventCode : 0,
                advisoryReason != null ? advisoryReason : "",
                lastTradePrice,
                ema38,
                ema100,
                System.currentTimeMillis() * 1000000L // Current timestamp in nanoseconds for InfluxDB
        );
    }
}
