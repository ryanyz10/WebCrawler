package assignment;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.net.*;
import org.attoparser.simple.*;

/**
 * A markup handler which is called by the Attoparser markup parser as it parses the input;
 * responsible for building the actual web index.
 */
public class CrawlingMarkupHandler extends AbstractSimpleMarkupHandler {
    private Page currPage;
    private LinkedList<URL> newURLs;
    private HashSet<String> seen;
    private WebIndex index;
    private boolean ignoreLastTag; // indicates whether or not we should ignore the last accessed tag
    private int currWordLoc; // keeps track of current position in the page

    public CrawlingMarkupHandler() {
        newURLs = new LinkedList<>();
        index = new WebIndex();
        seen = new HashSet<>();
        ignoreLastTag = false;
        currWordLoc = 1;
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

    public void setURL(URL currURL) {
        currPage = new Page(currURL);
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
        newURLs = new LinkedList<>();
        seen.add(currPage.toString());
        currWordLoc = 0;
    }

    /**
    * Called when the parser finishes reading a document.
    * @param endTimeNanos    the current time (in nanoseconds) when parsing ends
    * @param totalTimeNanos  the difference between current times at the start
    *                        and end of parsing
    * @param line            the line of the document where parsing ends
    * @param col             the column of the document where the parsing ends
    */
    public void handleDocumentEnd(long endTimeNanos, long totalTimeNanos, int line, int col) {}

    /**
    * Called at the start of any tag.
    * @param elementName the element name (such as "div")
    * @param attributes  the element attributes map, or null if it has no attributes
    * @param line        the line in the document where this elements appears
    * @param col         the column in the document where this element appears
    */
    public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
        elementName = elementName.toLowerCase();

        if (elementName.equals("script") || elementName.equals("style")) {
            ignoreLastTag = true;
            return;
        }

        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        TreeMap<String, String> caselessAttr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caselessAttr.putAll(attributes);

        if (elementName.equals("a")) {
            String href = caselessAttr.get("href");
            if (href == null) {
                return;
            }

            // only examine files that are valid web pages
            if (!href.contains(".html") && !href.contains(".htm")) {
                return;
            }

            try {
                // get an absolute path
                Path basePath = FileSystems.getDefault().getPath(currPage.toString());
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
    public void handleCloseElement(String elementName, int line, int col) {}

    @Override
    public void handleStandaloneElement(String elementName, Map<String,String> attributes, boolean minimized, int line, int col) {
        handleOpenElement(elementName, attributes, line, col);
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

        StringBuilder str = new StringBuilder();
        for(int i = start; i < start + length; i++) {
            String curr = Character.toString(ch[i]);
            if (curr.matches("\\w") || curr.equals("-")) {
                str.append(curr);
            } else {
                addWord(str.toString());
                str = new StringBuilder();
            }
        }

        if (str.length() != 0) {
            addWord(str.toString());
        }
    }

    /**
     * Adds the given word to the index
     * @param str the word to be add
     */
    private void addWord(String str) {
        if (str.length() == 0) {
            return;
        }

        index.add(str.toLowerCase(), currPage, currWordLoc);
        currWordLoc++;
    }
}
