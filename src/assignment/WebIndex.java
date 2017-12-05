package assignment;

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
    // We avoid URL because .equals is wonky (attempts to resolve host to IP)
    // Use String instead
    private HashMap<String, HashSet<String>> index;

    public WebIndex() {
        index = new HashMap<>();
    }

    public void add(String str, String url) {
        if (index.containsKey(str)) {
            index.get(str).add(url);
        } else {
            HashSet<String> set = new HashSet<>();
            set.add(url);
            index.put(str, set);
        }
    }
}
