import java.util.ArrayList;
import java.util.List;

public class PlayContentsBean {
//    public String[] MovieFile;
//    public String   GuidImageFile;

    public ArrayList<Contents> getContentsList() {
        return contentsList;
    }

    private ArrayList<Contents> contentsList = new ArrayList<Contents>();

    public void addContents(Contents c) {
        this.contentsList.add(c);
    }

    public void addContents(List<Contents> list) {
        this.contentsList.addAll(list);
    }

}
