package storage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;

public class Tuple implements Serializable, Comparable<Tuple>  {

    private Hashtable<String, Object> content;

    private String clusteringKey;
    public Hashtable<String, Object> getContent() {
        return content;
    }

    public void setContent(Hashtable<String, Object> content) {
        this.content = content;
    }


    public Tuple(Hashtable<String, Object> content, String clusteringKey) {
        this.content = content;
        this.clusteringKey = clusteringKey;
    }

    public String toString() {
        StringBuilder out = new StringBuilder();

        for (String key : content.keySet()) {
            out.append(content.get(key)).append(",");
        }
        return out.substring(0, out.length() - 1);
    }

    public Comparable getClusteringKeyValue() {
        return (Comparable) content.get(clusteringKey);
    }

    public Tuple clone() {
        return new Tuple((Hashtable<String, Object>) content.clone(), clusteringKey);
    }

    @Override
    public int compareTo(Tuple o) {
        return ((Comparable) content.get(clusteringKey)).compareTo(o.content.get(clusteringKey));
    }


    public boolean equals(Tuple tuple) {
        for(String key : content.keySet()) {
            if(!content.get(key).equals(tuple.content.get(key))) return false;
        }
        return true;
    }
}
