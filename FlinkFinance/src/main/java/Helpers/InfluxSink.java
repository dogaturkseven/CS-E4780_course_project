package Helpers;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

public class InfluxSink extends RichSinkFunction<Point> {
    private final String influxUrl;
    private final String username;
    private final String password;
    private final String database;
    private transient InfluxDB influxDB;

    public InfluxSink(String influxUrl, String username, String password, String database) {
        this.influxUrl = influxUrl;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        super.open(parameters);
        influxDB = InfluxDBFactory.connect(influxUrl, username, password);
        influxDB.setDatabase(database);
    }

    @Override
    public void invoke(Point value, Context context) throws Exception {
        if (influxDB != null) {
            influxDB.write(value);
        }
    }

    @Override
    public void close() throws Exception {
        if (influxDB != null) {
            influxDB.close();
        }
        super.close();
    }
}
