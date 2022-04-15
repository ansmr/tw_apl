import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MyBeacon Beansクラス
 */
public class MyBeaconBean {
    @JsonProperty("uuid")
    public String       uuid;

    @JsonProperty("distance")
    public double       distance;
}
