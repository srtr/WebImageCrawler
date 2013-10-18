package webimagecrawler;

import java.util.Comparator;

public class CustomComparator implements Comparator<Website> {
	@Override
    public int compare(Website o1, Website o2) {
        return Integer.compare(o1.getWeight(), o2.getWeight());
    }
}