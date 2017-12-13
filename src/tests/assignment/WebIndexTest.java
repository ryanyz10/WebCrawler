package assignment;

import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.ISimpleMarkupParser;
import org.attoparser.simple.SimpleMarkupParser;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


import static org.junit.Assert.*;

public class WebIndexTest {
    private static HTMLBuilder builder;
    private static WebIndex index;
    private static final int NUM_PAGES = 5000;

    @BeforeClass
    public static void setUp() {
        builder = new HTMLBuilder(NUM_PAGES, 5);
        System.out.println("Done generating HTML");
        ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
        CrawlingMarkupHandler handler = new CrawlingMarkupHandler();
        Deque<URL> remaining = new ArrayDeque<>();
        try {
            remaining.add(new URL("file:///Users/ryanzhou/prog7/testhtml/index.html"));
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        }

        while (!remaining.isEmpty()) {
            URL currURL = remaining.poll();
            handler.setURL(currURL);
            try {
                parser.parse(new InputStreamReader(currURL.openStream()), handler);
            } catch (ParseException e) {
                System.err.println("Parse failed!");
            } catch (IOException e) {
                System.err.println("IOException during parsing");
            }

            remaining.addAll(handler.newURLs());
        }
        System.out.println("Done crawling");
        index = (WebIndex)(handler.getIndex());
    }

    /**
     * checks if the number of pages specified by NUM_PAGES exists
     */
    @Test
    public void testAllPagesVisited() {
        // plus one for index.html
        assertEquals(NUM_PAGES + 1, index.getAllPages().size());
    }

    /**
     * Checks if all the words that are supposed to be on a page were detected
     * @throws MalformedURLException this shouldn't happen
     */
    @Test
    public void wordsOnCorrectPages() throws MalformedURLException {
        HashMap<Integer, HashSet<String>> docWords = builder.getDocWords();
        for (Integer i : docWords.keySet()) {
            Page page = new Page(new URL("file:///Users/ryanzhou/prog7/testhtml/doc" + i + ".html"));
            HashSet<String> words = docWords.get(i);
            for (String word : words) {
                Set<Page> pages = index.getPagesWith(word.toLowerCase());
                assertTrue(pages.contains(page));
            }
        }
    }

    @Test
    public void allPagesExist() throws MalformedURLException {
        Set<Page> all = index.getAllPages();
        for (int i = 0; i < NUM_PAGES; i++) {
            Page page = new Page(new URL("file:///Users/ryanzhou/prog7/testhtml/doc" + i + ".html"));
            assertTrue(all.contains(page));
        }
    }
}