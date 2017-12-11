package assignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A web-index which efficiently stores information about pages. Serialization is done automatically
 * via the superclass "Index" and Java's Serializable interface.
 */
public class WebIndex extends Index {
    private static final long serialVersionUID = 1L;
    // We use a hashmap because of its O(1) lookup time
    // Store pages for easier querying
    // HashSet of positions so we know where the word is in the page
    private HashMap<String, HashMap<Page, HashSet<Integer>>> index;

    public WebIndex() {
        index = new HashMap<>();
    }

    public void add(String str, Page currPage, int location) {
        if (index.containsKey(str)) {
            HashMap<Page, HashSet<Integer>> map = index.get(str);

            if (map.containsKey(currPage)) {
                map.get(currPage).add(location); }
                else {
                    HashSet<Integer> locations = new HashSet<>();
                    locations.add(location);map.put(currPage, locations);
                }

                index.put(str, map);
        } else {
            HashMap<Page, HashSet<Integer>> map = new HashMap<>();
            HashSet<Integer> locations = new HashSet<>();
            locations.add(location);
            map.put(currPage, locations);
            index.put(str, map);
        }
    }

    public Set<Page> getPagesWith(String str) {
        if (!index.containsKey(str)) {
            return new HashSet<>();
        }

        return new HashSet<>(index.get(str).keySet());
    }

    public Set<Page> getAllPages() {
        HashSet<Page> all = new HashSet<>();
        for (String str : index.keySet()) {
            all.addAll(index.get(str).keySet());
        }

        return all;
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
