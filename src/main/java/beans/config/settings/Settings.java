import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.Serializable;

/**
 * Created by c1734 on 2017/06/06.
 */

@Root
public class Settings implements Serializable {
    @Element
    public boolean PlayControl;

    @Element
    public double OutOfDistance;

    @Element
    public int BackgroundBetweenScanPeriod;

    @Element
    public int BackgroundScanPeriod;

    @Element
    public String DefaultImage;
}
