package flinkFinance;

import Calculators.EmaCalculationProcessFunction;
import Deserializer.JSONValueDeserializationSchema;
import Dto.TradeData;
import Helpers.InfluxSink;
import Dto.TradingOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.influxdb.dto.Point;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.concurrent.TimeUnit;


public class DataStreamJob {

 public static void main(String[] args) throws Exception {
  // Set up the Flink execution environment
  final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
  env.setParallelism(4); // Sets the global parallelism to 4


  String topic  = "my_topic";

  KafkaSource<TradeData> source = KafkaSource.<TradeData>builder()
    .setBootstrapServers("broker:29092")
    .setTopics(topic).setGroupId("flinkFinance")
    .setStartingOffsets(OffsetsInitializer.earliest())
    .setValueOnlyDeserializer(new JSONValueDeserializationSchema())
    .build();


  // Define watermark strategy for event time processing
  WatermarkStrategy<TradeData> watermarkStrategy = WatermarkStrategy
    .<TradeData>forMonotonousTimestamps()
    .withTimestampAssigner((event, timestamp) -> {
     try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
      LocalTime tradingTime = LocalTime.parse(event.getTradingTime(), formatter);
      return tradingTime.getLong(ChronoField.MILLI_OF_DAY); 
     } catch (Exception e) {
      System.err.println("Invalid tradingTime format: " + event.getTradingTime());
      return Long.MIN_VALUE; 
     }
    });

  // Create the data stream from the Kafka source
  DataStream<TradeData> tradeDataStream = env.fromSource(source, watermarkStrategy, "Kafka Source").setParallelism(4).disableChaining();

  // Filter out invalid data points where tradingTime is null or empty
  DataStream<TradeData> filteredTradeDataStream = tradeDataStream.filter(data ->
    data != null &&
      data.getTradingTime() != null && !data.getTradingTime().isEmpty() &&
      data.getLastTradePrice() != null && !data.getLastTradePrice().isEmpty()
  ).setParallelism(4).disableChaining(); 

  // Key the stream by trade ID (symbol)
  KeyedStream<TradeData, String> keyedStream = filteredTradeDataStream.keyBy(TradeData::getId);

  DataStream<TradingOutput> outputStream = keyedStream.process(new EmaCalculationProcessFunction())
    .setParallelism(4).disableChaining(); // Parallelism for the process function

	outputStream
    .map(output -> {
     // Parse tradingDate and tradingTime
     DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
     DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss[.SSS]");

     // Convert tradingDate and tradingTime to LocalDateTime
     LocalDate tradingDate = LocalDate.parse(output.getTradingDate(), dateFormatter);
     LocalTime tradingTime = LocalTime.parse(output.getTradingTime(), timeFormatter);
     LocalDateTime dateTime = LocalDateTime.of(tradingDate, tradingTime);

     // Convert LocalDateTime to timestamp in milliseconds
     long eventTimestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

// Build the InfluxDB Point
     return Point.measurement("trading_output")
       .time(eventTimestamp, TimeUnit.MILLISECONDS)
       .addField("lastTradePrice", output.getLastTradePrice())
       .addField("ema38", output.getEma38())
       .addField("ema100", output.getEma100())
       .addField("eventCode", output.getEventCode())
       .tag("symbol", output.getSymbol()) // Add symbol as a tag
       .build();
    }).setParallelism(4).disableChaining()
    .addSink(new InfluxSink("http://influxdb:8086", "username", "password", "trading_db"))
    .setParallelism(4).disableChaining();



  // Execute program, beginning computation.
  env.execute("Flink Quickstart Job");
 }
}