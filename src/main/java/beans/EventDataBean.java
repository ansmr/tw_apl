import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Vector;

public class EventDataBean {
    public int                  eventtype;

    public Vector<MyBeaconBean> beacons = new Vector<MyBeaconBean>();

    /**
     * オブジェクトをJSON形式の文字列に変換する
     * @return
     */
    public String toJsonString() {
        String          json;
        ObjectMapper    om = new ObjectMapper();

        try {
            json = om.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            json = null;
        }

        return json;
    }
}
