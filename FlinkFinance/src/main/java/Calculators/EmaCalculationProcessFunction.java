package Calculators;

import Dto.TradeData;
import Dto.TradingOutput;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class EmaCalculationProcessFunction extends KeyedProcessFunction<String, TradeData, TradingOutput> {

    private ValueState<Double> ema38State; // Current EMA38
    private ValueState<Double> ema100State; // Current EMA100
    private ValueState<Double> latestPriceState; // Latest lastTradePrice in the window
    private ValueState<Long> currentWindowState; // Current window start time
    private ValueState<String> tradingDateState; 

    @Override
    public void open(Configuration parameters) throws Exception {
        ema38State = getRuntimeContext().getState(new ValueStateDescriptor<>("ema38", Double.class, 0.0));
        ema100State = getRuntimeContext().getState(new ValueStateDescriptor<>("ema100", Double.class, 0.0));
        latestPriceState = getRuntimeContext().getState(new ValueStateDescriptor<>("latestPrice", Double.class));
        currentWindowState = getRuntimeContext().getState(new ValueStateDescriptor<>("currentWindow", Long.class));
        // Initialize the tradingDateState
        tradingDateState = getRuntimeContext().getState(new ValueStateDescriptor<>("tradingDate", String.class));
    }

    @Override
    public void processElement(TradeData value, Context ctx, Collector<TradingOutput> out) throws Exception {
        long windowDuration = Time.minutes(5).toMilliseconds();
        long currentEventTime = parseTimeToMillis(value.getTradingTime());
        long currentWindowStartTime = (currentEventTime / windowDuration) * windowDuration;
        long currentWindowEndTime = currentWindowStartTime + windowDuration;


        ctx.timerService().registerEventTimeTimer(currentWindowEndTime);

        Long lastWindowStartTime = currentWindowState.value();
        if (lastWindowStartTime == null || lastWindowStartTime < currentWindowStartTime) {
            // New window detected
            currentWindowState.update(currentWindowStartTime);
        }

        // Update the latest price for the current window
        latestPriceState.update(Double.parseDouble(value.getLastTradePrice()));

        // Store the tradingDate in state
        tradingDateState.update(value.getTradingDate());
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<TradingOutput> out) throws Exception {
        double latestPrice = latestPriceState.value();
        if (latestPrice == 0.0) {
            // No trades in this window, skip calculation
            return;
        }

        // Retrieve previous EMA values
        double prevEma38 = ema38State.value();
        double prevEma100 = ema100State.value();

        // Calculate smoothing constants
        double smoothing38 = 2.0 / (1 + 38.0);
        double smoothing100 = 2.0 / (1 + 100.0);

        // Calculate new EMA values based on the latest price
        double currEma38 = smoothing38 * latestPrice + (1 - smoothing38) * prevEma38;
        double currEma100 = smoothing100 * latestPrice + (1 - smoothing100) * prevEma100;

        // Update EMA states
        ema38State.update(currEma38);
        ema100State.update(currEma100);

        String tradingDate = tradingDateState.value();

        // Emit the result
        String windowEndTime = timestampToTimeString(timestamp);
        out.collect(new TradingOutput(ctx.getCurrentKey(), windowEndTime, tradingDate, latestPrice,
                currEma38, currEma100, prevEma38, prevEma100, 0, "No Crossover"));
        // Check for breakout patterns
        if (currEma38 > currEma100 && prevEma38 <= prevEma100 && prevEma38 != 0) {
            out.collect(new TradingOutput(ctx.getCurrentKey(), windowEndTime, tradingDate, latestPrice,
                    currEma38, currEma100, prevEma38, prevEma100, 2, "Bullish Crossover"));        }

        if (currEma38 < currEma100 && prevEma38 >= prevEma100 && prevEma100 != 0) {
            out.collect(new TradingOutput(ctx.getCurrentKey(), windowEndTime, tradingDate, latestPrice,
                    currEma38, currEma100, prevEma38, prevEma100, 1, "Bearish Crossover"));        }
    }

    private String timestampToTimeString(long timestamp) {
        long seconds = (timestamp / 1000) % 60;
        long minutes = (timestamp / (1000 * 60)) % 60;
        long hours = (timestamp / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private long parseTimeToMillis(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        return (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
    }
}
