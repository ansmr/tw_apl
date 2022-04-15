import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class Beacon {
    @Element(required=false)
    public String uuid;

    @Element(required=false)
    public int priority;

    @ElementList(name="current")
    public List<Contents> current;

    @ElementList(name="previous")
    public List<Contents> previous;

    @ElementList(name="next")
    public List<Contents> next;

    @ElementList(name="error")
    public List<Contents> error;



}
