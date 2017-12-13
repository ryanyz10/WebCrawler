package assignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Generates a set of random connected webpages
 */
public class HTMLBuilder {
    private final ArrayList<String> words = new ArrayList<>();
    private boolean[][] graph;
    private HashMap<Integer, HashSet<String>> docWords;
    private int numPages;

    public HTMLBuilder(int numPages, int wordsPerPage) {
        try {
            Scanner reader = new Scanner(new File("words.txt"));
            while (reader.hasNext()) {
                words.add(reader.next());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Failed to read words.txt");
        }

        this.numPages = numPages;
        graph = new boolean[numPages][numPages];
        for (int i = 0; i < numPages; i++) {
            for (int j = 0; j <= i; j++) {
                graph[i][j] = random();
                graph[j][i] = graph[i][j];
            }
        }

        // make the folder if it doesn't exist
        File folder = new File("testhtml/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        docWords = new HashMap<>();
        for (int i = 0; i < numPages; i++) {
            writeDocument(i, wordsPerPage);
        }

        createIndex();
    }

    public boolean[][] getGraph() {
        return graph;
    }

    public HashMap<Integer, HashSet<String>> getDocWords() {
        return docWords;
    }

    private boolean random() {
        return ((int)(Math.random() * 100) + 1) >= 51;
    }

    private void writeDocument(int docNum, int numWords) {
        HashSet<String> words = new HashSet<>();
        for (int i = 0; i < numWords; i++) {
            words.add(getRandomWord());
        }
        docWords.put(docNum, words);

        String body = "";
        body += "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\"><title>Document " + docNum +"</title>\n" +
                "</head>\n<body>\n<ul>";

        for (String str : words) {
            body += "<li>" + str + "</li>\n";
        }

        if (((int)(Math.random() * 30) + 1) <= 10) {
            body += "<li>A quick brown fox jumped over the lazy dog</li>\n";
        }

        boolean[] links = graph[docNum];
        for (int i = 0; i < links.length; i++) {
            if (docNum != i && links[i]) {
                body += "<a href=\"doc" + i +".html\">LINK " + i + "</a><br>\n";
            }
        }

        body += "</ul>\n</body>\n</html>";

        try (PrintWriter out = new PrintWriter(new File("testhtml/doc" + docNum + ".html"))) {
            out.println(body);
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not write file");
        }
    }

    private void createIndex() {
        String body = "";
        body += "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\"><title>Index</title>\n" +
                "</head>\n<body>\n<ul>";

        for (int i = 0; i < numPages; i++) {
            body += "<li><a href=\"doc" + i +".html\">LINK " + i + "</a></li>\n";
        }

        body += "</ul>\n</body>\n</html>";

        try (PrintWriter out = new PrintWriter(new File("testhtml/index.html"))) {
            out.println(body);
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not write index");
        }
    }

    private String getRandomWord() {
        return words.get((int)(Math.random() * words.size()));
    }
}
