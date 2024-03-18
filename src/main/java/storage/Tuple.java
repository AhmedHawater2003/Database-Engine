package storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;

public class Tuple implements Serializable {

    private Hashtable<String, Object> content;
    public Hashtable<String, Object> getContent() {
        return content;
    }
    public void setContent(Hashtable<String, Object> content) {
        this.content = content;
    }



    public Tuple(Hashtable<String, Object> content) {
        this.content = content;
    }

    public String toString() {
        StringBuilder out = new StringBuilder();

        for (String key : content.keySet()) {
            out.append(content.get(key)).append(",");
        }
        return out.substring(0, out.length() - 1);
    }


}
