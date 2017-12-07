package assignment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A web-index which efficiently stores information about pages. Serialization is done automatically
 * via the superclass "Index" and Java's Serializable interface.
 *
 * TODO: Implement this!
 */
public class WebIndex extends Index {
    /**
     * Needed for Serialization (provided by Index) - don't remove this!
     */
    private static final long serialVersionUID = 1L;
    // We use a hashmap because of its O(1) lookup time
    // Store pages for easier querying
    // HashSet of positions for pages in
    private HashMap<String, HashMap<Page, HashSet<Integer>>> index;

    public WebIndex() {
        index = new HashMap<>();
    }

    public void add(String str, String url, int location) {
        if (index.containsKey(str)) {
            HashMap<Page, HashSet<Integer>> map = index.get(str);
            try {
                Page page = new Page(new URL(url));
                if (map.containsKey(page)) {
                    map.get(page).add(location);
                } else {
                    HashSet<Integer> locations = new HashSet<>();
                    locations.add(location);
                    map.put(page, locations);
                }

                index.put(str, map);
            } catch (MalformedURLException e) {
                // shouldn't happen
            }
        } else {
            HashMap<Page, HashSet<Integer>> map = new HashMap<>();
            try {
                Page page = new Page(new URL(url));
                HashSet<Integer> locations = new HashSet<>();
                locations.add(location);
                map.put(page, locations);
                index.put(str, map);
            } catch (MalformedURLException e) {
                // also shouldn't happen
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (String key : index.keySet()) {
            result.append(key);
            result.append("\n");
            HashMap<Page, HashSet<Integer>> map = index.get(key);
            for (Page page : map.keySet()) {
                result.append("\t");
                result.append(page.toString());
                result.append(" ");
                result.append(map.get(page));
                result.append("\n");
            }
        }

        return result.toString();
    }

}
