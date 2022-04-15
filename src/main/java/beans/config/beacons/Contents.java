import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class Contents {
    @Attribute(name="loop")
    public boolean loop = false;

    @Attribute(name="type")
    public String type;

    @Attribute(name="filename")
    public String filename;

    public boolean isMovie() {
        return type.equals("movie");
    }

    public boolean isImage() {
        return type.equals("image");
    }
}
