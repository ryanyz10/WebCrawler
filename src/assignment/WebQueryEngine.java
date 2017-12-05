package assignment;
import java.net.URL;
import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 *
 * TODO: Implement this!
 */
public class WebQueryEngine {
    private static final HashSet<Character> operators = new HashSet<>();

    static {
        operators.add('(');
        operators.add(')');
        operators.add('|');
        operators.add('&');
    }

    private Index index;
    /**
     * Returns a WebQueryEngine that uses the given Index to constructe answers to queries.
     *
     * @param index The WebIndex this WebQueryEngine should use.
     * @return A WebQueryEngine ready to be queried.
     */
    public static WebQueryEngine fromIndex(WebIndex index) {
        return new WebQueryEngine(index);
    }

    public WebQueryEngine(Index index) {
        this.index = index;
    }

    /**
     * Returns a Collection of URLs (as Strings) of web pages satisfying the query expression.
     *
     * @param query A query expression.
     * @return A collection of web pages satisfying the query.
     */
    public Collection<Page> query(String query) {
        query = query.replaceAll("\\s", "");

        ArrayList<Token> tokens = new ArrayList<>(3);

        StringBuilder word = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char current = query.charAt(i);
            if (operators.contains(current)) {
                tokens.add(new Token(Character.toString(current)));
            }

            word.append(current);
            if (i < query.length() - 1) {
                char next = query.charAt(i + 1);
                if (operators.contains(next)) {
                    tokens.add(new Token(word.toString()));
                    word = new StringBuilder();
                }
            }
        }



        return new LinkedList<>();
    }

    class Token {
        String token;
        Token(String token) {
            this.token = token;
        }
    }
}
