package assignment;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.net.*;
import org.attoparser.simple.*;

/**
 * A markup handler which is called by the Attoparser markup parser as it parses the input;
 * responsible for building the actual web index.
 *
 * TODO: Implement this!
 */
public class CrawlingMarkupHandler extends AbstractSimpleMarkupHandler {
    private String currURL;
    private LinkedList<URL> newURLs;
    private HashSet<String> seen;
    private WebIndex index;
    private boolean ignoreLastTag; // indicates whether or not we should ignore the last accessed tag
    private long totalTime;
    private long totalPages;


    public CrawlingMarkupHandler() {
        newURLs = new LinkedList<>();
        index = new WebIndex();
        seen = new HashSet<>();
        ignoreLastTag = false;
        totalTime = 0;
        totalPages = 0;
    }

    /**
    * This method returns the complete index that has been crawled thus far when called.
    */
    public Index getIndex() {
        return index;
    }

    /**
    * This method returns any new URLs found to the Crawler; upon being called, the set of new URLs
    * should be cleared.
    */
    public List<URL> newURLs() {
        return newURLs;
    }

    public void setURL(String currURL) {
        this.currURL = currURL;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getTotalPages() {
        return totalPages;
    }

    /*
    * These are some of the methods from AbstractSimpleMarkupHandler.
    * All of its method implementations are NoOps, so we've added some things
    * to do; please remove all the extra printing before you turn in your code.
    *
    * Note: each of these methods defines a line and col param, but you probably
    * don't need those values. You can look at the documentation for the
    * superclass to see all of the handler methods.
    */

    /**
    * Called when the parser first starts reading a document.
    * @param startTimeNanos  the current time (in nanoseconds) when parsing starts
    * @param line            the line of the document where parsing starts
    * @param col             the column of the document where parsing starts
    */
    public void handleDocumentStart(long startTimeNanos, int line, int col) {
        // System.out.println("Start of document");
        newURLs = new LinkedList<>();
        seen.add(currURL);
        totalPages++;
    }

    /**
    * Called when the parser finishes reading a document.
    * @param endTimeNanos    the current time (in nanoseconds) when parsing ends
    * @param totalTimeNanos  the difference between current times at the start
    *                        and end of parsing
    * @param line            the line of the document where parsing ends
    * @param col             the column of the document where the parsing ends
    */
    public void handleDocumentEnd(long endTimeNanos, long totalTimeNanos, int line, int col) {
        // System.out.println("End of document");
        totalTime += totalTimeNanos;
    }

    /**
    * Called at the start of any tag.
    * @param elementName the element name (such as "div")
    * @param attributes  the element attributes map, or null if it has no attributes
    * @param line        the line in the document where this elements appears
    * @param col         the column in the document where this element appears
    */
    public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
        if (elementName.equals("script") || elementName.equals("style")) {
            ignoreLastTag = true;
            return;
        }

        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        elementName = elementName.toLowerCase();
        TreeMap<String, String> caselessAttr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caselessAttr.putAll(attributes);

        // TODO handle more cases, for now handle urls and script/style tags
        if (elementName.equals("a")) {
            String href = caselessAttr.get("href");
            // no emails
            if (!href.contains(".html") || !href.contains(".htm")) {
                return;
            }

            // this is a valid link, we want the href attribute
            try {
                // get an absolute path
                Path basePath = FileSystems.getDefault().getPath(currURL);
                Path resolvedPath = basePath.getParent().resolve(href);
                Path absolutePath = resolvedPath.normalize();
                String path = absolutePath.toString();

                if (seen.contains(path)) {
                    return;
                }

                seen.add(path);

                URL tmp = new URL(path);
                newURLs.add(tmp);
            } catch (MalformedURLException e) {
                System.err.println("Error in CrawlingMarkupHandler: malformed url");
            }
        }
    }

    /**
    * Called at the end of any tag.
    * @param elementName the element name (such as "div").
    * @param line        the line in the document where this elements appears.
    * @param col         the column in the document where this element appears.
    */
    public void handleCloseElement(String elementName, int line, int col) {
        // System.out.println("End element:   " + elementName);
    }

    @Override
    public void handleStandaloneElement(String elementName, Map<String,String> attributes, boolean minimized, int line, int col) {

    }

    /**
    * Called whenever characters are found inside a tag. Note that the parser is not
    * required to return all characters in the tag in a single chunk. Whitespace is
    * also returned as characters.
    * @param ch      buffer containing characters; do not modify this buffer
    * @param start   location of 1st character in ch
    * @param length  number of characters in ch
    */
    public void handleText(char ch[], int start, int length, int line, int col) {
        if (ignoreLastTag) {
            ignoreLastTag = false;
            return;
        }
        // System.out.print("Characters:    \"");
        StringBuilder str = new StringBuilder();
        for(int i = start; i < start + length; i++) {
            // Instead of printing raw whitespace, we're escaping it
            switch(ch[i]) {
                case '\\':
                    // System.out.print("\\\\");
                    str.append(" ");
                    break;
                case '"':
                    // System.out.print("\\\"");
                    str.append(" ");
                    break;
                case '\n':
                    // System.out.print("\\n");
                    str.append(" ");
                    break;
                case '\r':
                    // System.out.print("\\r");
                    str.append(" ");
                    break;
                case '\t':
                    // System.out.print("\\t");
                    str.append(" ");
                    break;
                default:
                    // System.out.print(ch[i]);
                    str.append(ch[i]);
                    break;
            }
        }

        // System.out.print("\"\n");
        index.add(str.toString(), currURL);

    }
}
