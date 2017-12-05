package assignment;

import java.io.*;
import java.net.*;
import java.util.*;

import org.attoparser.simple.*;
import org.attoparser.config.ParseConfiguration;

/**
 * The entry-point for WebCrawler; takes in a list of URLs to start crawling from and saves an index
 * to index.db.
 */
public class WebCrawler {

    /**
    * The WebCrawler's main method starts crawling a set of pages.  You can change this method as
    * you see fit, as long as it takes URLs as inputs and saves an Index at "index.db".
    */
    public static void main(String[] args) {
        // Basic usage information
        if (args.length == 0) {
            System.err.println("Error: No URLs specified.");
            System.exit(1);
        }

        // We'll throw all of the args into a queue for processing.
        Queue<URL> remaining = new LinkedList<>();
        for (String url : args) {
            try {
                remaining.add(new URL(url));
            } catch (MalformedURLException e) {
                // Throw this one out!
                System.err.printf("Error: URL '%s' was malformed and will be ignored!%n", url);
            }
        }

        // Create a parser from the attoparser library, and our handler for markup.
        ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
        CrawlingMarkupHandler handler = new CrawlingMarkupHandler();

        // Try to start crawling, adding new URLS as we see them.
        try {
            while (!remaining.isEmpty()) {
                // pass the current URL to the handler so it can keep track of the information
                URL currURL = remaining.poll();
                handler.setURL(currURL.toString());
                // Parse the next URL's page
                try {
                    parser.parse(new InputStreamReader(currURL.openStream()), handler);
                } catch (FileNotFoundException e) {
                    System.err.printf("Could not find file %s\n", currURL.toString());
                } catch (org.attoparser.ParseException e) {
                    System.err.printf("Could not parse %s\n", currURL.toString());
                }


                // Add any new URLs
                remaining.addAll(handler.newURLs());
            }

            handler.getIndex().save("index.db");
            System.out.printf("Total time taken: %.3f seconds\n", ((double)handler.getTotalTime())/1000000000);
            System.out.printf("Pages visited: %d\n", handler.getTotalPages());
        } catch (Exception e) {
            // Bad exception handling :(
            System.err.println("Error: Index generation failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
