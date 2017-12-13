package assignment;

import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.ISimpleMarkupParser;
import org.attoparser.simple.SimpleMarkupParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class WebQueryEngineTest {
    private static final int NUM_PAGES = 500;
    private static HTMLBuilder builder;
    private static WebQueryEngine engine;
    private String[] operands;

    /**
     * Generate HTML documents and initialize the WebQueryEngine
     */
    @BeforeClass
    public static void genHTML() {
        builder = new HTMLBuilder(NUM_PAGES, 20);
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
        engine = WebQueryEngine.fromIndex((WebIndex) handler.getIndex());

    }

    /**
     * reset the operand array before each test
     */
    @Before
    public void before() {
        operands = new String[0];
    }

    /**
     * find some number of operands that we know exist on the same page
     * @param numOperands the number of operands to get
     */
    private void getOperands(int numOperands) {
        HashMap<Integer, HashSet<String>> docWords = builder.getDocWords();
        operands = new String[numOperands];
        int index = 0;
        for (Integer i : docWords.keySet()) {
            HashSet<String> set = docWords.get(i);
            for (String str : set) {
                if (index >= numOperands) {
                    break;
                } else {
                    operands[index] = str;
                    index++;
                }
            }

            if (index >= numOperands) {
                break;
            }
        }
    }

    /**
     * get the document number of the html file given the url
     * @param url the url of the file
     * @return the doc number
     */
    private int getDocNum(String url) {
        String tmp = url.substring(url.lastIndexOf("/") + 1, url.indexOf(".html"));

        if (tmp.equals("index")) {
            return -1;
        }

        return Integer.parseInt(tmp.substring(tmp.indexOf("doc") + 3));
    }

    /**
     * test a basic query (1 word)
     */
    @Test
    public void testBasicQuery() {
        getOperands(1);
        String query = operands[0];
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            String url = page.toString();
            int docNum = getDocNum(url);

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            assertTrue(words.contains(operands[0]));
        }
    }

    /**
     * test a simple AND query
     */
    @Test
    public void testAndQuery() {
        getOperands(2);
        String query = operands[0] + " & " + operands[1];
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            for (int i = 0; i < operands.length; i++) {
                assertTrue(words.contains(operands[i]));
            }
        }
    }

    /**
     * test a simple OR query
     */
    @Test
    public void testOrQuery() {
        getOperands(2);
        String query = operands[0] + " | " + operands[1];
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            boolean correct = false;
            for (int i = 0; i < operands.length; i++) {
                correct |= words.contains(operands[i]);
            }

            assertTrue(correct);
        }
    }

    /**
     * test a query containing AND and OR
     */
    @Test
    public void testAndOrQuery() {
        getOperands(3);
        String query = "((" + operands[0] + " & " + operands[1] + ")" + " | " + operands[2] + ")";
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            assertTrue((words.contains(operands[0]) && words.contains(operands[1])) || words.contains(operands[2]));
        }
    }

    /**
     * test a negative query
     */
    @Test
    public void testNegativeQuery() {
        getOperands(1);
        String query = "!" + operands[0];
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            assertFalse((words.contains(operands[0])));
        }
    }

    /**
     * test a phrase query
     */
    @Test
    public void testPhraseQuery() {
        String query = "\"A quick brown fox jumped over the lazy dog\"";
        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());
            try {

                Scanner reader = new Scanner(new File("testhtml/doc" + docNum + ".html"));
                String doc = "";
                while (reader.hasNextLine()) {
                    doc += reader.nextLine().trim();
                    doc += " ";
                }

                doc = doc.substring(doc.indexOf("<ul>") + 4, doc.lastIndexOf("</ul>"));
                doc = doc.replaceAll("<li>", " ");
                doc = doc.replaceAll("</li>\n", " ");
                doc = doc.replaceAll("\\s+", " ");

                assertTrue(doc.contains("A quick brown fox jumped over the lazy dog"));
            } catch (FileNotFoundException e) {
                System.err.println("Could not open file doc" + docNum + ".html");
            }
        }
    }

    /**
     * test an implicit and query
     */
    @Test
    public void testImplicitAndQuery() {
        getOperands(2);
        String query = "";
        for (String str : operands) {
            query += str + " ";
        }

        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            for (int i = 0; i < operands.length; i++) {
                assertTrue(words.contains(operands[i]));
            }
        }
    }

    /**
     * test a query with precedence (no parentheses)
     */
    @Test
    public void testPrecedence() {
        getOperands(4);
        String query = "";
        for (int i = 0; i < operands.length; i++) {
            query += operands[i];
            if (i != operands.length - 1) {
                if (i % 2 == 0) {
                    query += " & ";
                } else {
                    query += " | ";
                }
            }
        }

        Collection<Page> result = engine.query(query);
        for (Page page : result) {
            int docNum = getDocNum(page.toString());

            if (docNum < 0) {
                continue;
            }

            HashSet<String> words = builder.getDocWords().get(docNum);
            boolean part1 = words.contains(operands[0]) && words.contains(operands[1]);
            boolean part2 = words.contains(operands[2]) && words.contains(operands[3]);
            assertTrue(part1 || part2);
        }
    }
}