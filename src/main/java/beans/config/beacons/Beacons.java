import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class Beacons {
    @ElementList(inline=true)
    public List<Beacon> list;

    /**
     * 指定したuuidが存在するかチェックする
     * @param uuid
     * @return
     */
    public boolean isExistsUUID(String uuid) {
        for (Beacon beacon : this.list) {
            if (beacon.uuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定したuuidのBeaconオブジェクトを返す
     * @param uuid
     * @return
     */
    public Beacon getBeacon(String uuid) {
        for (Beacon beacon : this.list) {
            if (beacon.uuid.equals(uuid)) {
                return beacon;
            }
        }
        return null;
    }
}
