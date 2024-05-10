package storage;

import java.io.Serializable;

public class PageInfo implements Serializable, Comparable<PageInfo> {

    private String pageAddress;
    private Comparable minKey;

    PageInfo (Comparable minKey){
        this.pageAddress = null;
        this.minKey = minKey;
    }
    PageInfo(String pageAddress, Comparable minKey) {
        this.pageAddress = pageAddress;
        this.minKey = minKey;
    }

    public Comparable getMinKey() {
        return minKey;
    }

    public String getPageAddress() {
        return pageAddress;
    }

    @Override
    public int compareTo(PageInfo o) {
        return minKey.compareTo(o.minKey);
    }
}
