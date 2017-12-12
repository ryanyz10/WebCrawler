package assignment;

import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 */
public class WebQueryEngine {
    private final static HashSet<String> operators;

    static {
        operators = new HashSet<>();
        operators.add("|");
        operators.add("&");
        operators.add("!");
        operators.add("(");
        operators.add(")");
        operators.add("\"");
    }

    private WebIndex index;
    /**
     * Returns a WebQueryEngine that uses the given Index to construct answers to queries.
     *
     * @param index The WebIndex this WebQueryEngine should use.
     * @return A WebQueryEngine ready to be queried.
     */
    public static WebQueryEngine fromIndex(WebIndex index) {
        return new WebQueryEngine(index);
    }

    public WebQueryEngine(WebIndex index) {
        this.index = index;
    }

    /**
     * Returns a Collection of URLs (as Strings) of web pages satisfying the query expression.
     *
     * @param query A query expression.
     * @return A collection of web pages satisfying the query.
     */
    public Collection<Page> query(String query) {
        if (query == null) {
            return new HashSet<>();
        }

        if (query.length() == 0) {
            return new HashSet<>();
        }

        query = query.toLowerCase();

        ArrayList<Token> tokens = getTokens(query);
        ASTNode root = buildAST(tokens);

        Set<Page> result = parseTree(root);
        System.out.println(result.size());
        return result;
    }

    /**
     * separates a query into tokens
     * @param query the given query
     * @return an ArrayList of tokens in the query
     */
    private ArrayList<Token> getTokens(String query) {
        ArrayList<Token> tokens = new ArrayList<>();

        String[] splitQuery = query.split("\\s+");
        StringBuilder builder = new StringBuilder();
        Token prev = null;
        boolean quotationSeen = false;

        for (String str : splitQuery) {
            for (int i = 0; i < str.length(); i++) {
                String curr = Character.toString(str.charAt(i));

                if (operators.contains(curr)) {
                    Token oper = new Token(curr);
                    if (builder.length() > 0) {
                        Token tmp = new Token(builder.toString());
                        if (!quotationSeen && needsImplicit(prev, tmp)) {
                            tokens.add(new Token("&", Token.AND));
                        }

                        tokens.add(tmp);
                        builder = new StringBuilder();
                        prev = tmp;
                    }

                    switch (curr) {
                        case "|": {
                            oper.precedence = Token.OR;
                            break;
                        }
                        case "&": {
                            oper.precedence = Token.AND;
                            break;
                        }
                        case "!": {
                            oper.precedence = Token.NOT;
                            break;
                        }
                        default: {
                            if (!quotationSeen && needsImplicit(prev, oper)) {
                                tokens.add(new Token("&", Token.AND));
                            }

                            if (curr.equals("\"")) {
                                quotationSeen = !quotationSeen;
                            }
                        }
                    }

                    prev = oper;
                    tokens.add(oper);
                } else {
                    builder.append(curr);
                }
            }

            if (builder.length() > 0) {
                Token tmp = new Token(builder.toString());

                if (!quotationSeen && needsImplicit(prev, tmp)) {
                    tokens.add(new Token("&", Token.AND));
                }

                prev = tmp;
                tokens.add(tmp);
                builder = new StringBuilder();
            }
        }

        return tokens;
    }

    /**
     * determines whether two tokens are the same kind (operator or word)
     * @param prev the first token
     * @param curr the second token
     * @return true if the two tokens are the same type, false otherwise
     */
    private boolean needsImplicit(Token prev, Token curr) {
        if (prev == null || curr == null) {
            return false;
        }

        boolean isOpenBracket = isOpenBracket(curr);
        boolean isQuotation = isQuotation(curr);
        boolean isWord = isWord(curr);
        boolean currNeedsImplicit = isOpenBracket || isQuotation || isWord;

        return (isCloseBracket(prev) || isWord(prev) || isQuotation(prev)) && currNeedsImplicit;
    }

    // the below four methods are helper methods to make code cleaner
    /**
     * determines whether the given token is a word
     * @param token the token
     * @return true if the token is a word, false otherwise
     */
    private boolean isWord(Token token) {
        return token.token.matches("[\\w-]+");
    }

    /**
     * determines whether the given token is an open parentheses
     * @param token the token
     * @return true if token is an open parentheses, false otherwise
     */
    private boolean isOpenBracket(Token token) {
        return token.token.equals("(");
    }

    /**
     * determines whether the given token is a close parentheses
     * @param token the token
     * @return true if token is an open parentheses, false otherwise
     */
    private boolean isCloseBracket(Token token) {
        return token.token.equals(")");
    }

    /**
     * determines whether the given token is a quote mark
     * @param token the token
     * @return true if token is a quote mark, false otherwise
     */
    private boolean isQuotation(Token token) {
        return token.token.equals("\"");
    }

    /**
     * builds an AST from a list of tokens using Djikstra's Shunting-Yard algorithm
     * @param tokens - tokens in the given query
     * @return the root of the newly-built AST
     */
    private ASTNode buildAST(ArrayList<Token> tokens) {
        Deque<ASTNode> nodeStack = new ArrayDeque<>();
        Deque<Token> operatorStack = new ArrayDeque<>();
        boolean quotationSeen = false;

        for (Token token : tokens) {
            if (operators.contains(token.token)) {
                if (token.token.equals("(")) {
                    operatorStack.push(token);
                } else if (token.token.equals(")")) {
                    Token oper = operatorStack.pop();

                    while (!oper.token.equals("(")) {
                        ASTNode newNode = new ASTNode(oper);
                        newNode.children.add(nodeStack.pop());
                        newNode.children.add(nodeStack.pop());
                        nodeStack.push(newNode);
                        oper = operatorStack.pop();
                    }
                } else if (token.token.equals("\"")) {
                    if (quotationSeen) {
                        ASTNode newNode = new ASTNode(token);
                        ASTNode node = nodeStack.pop();
                        while (!node.token.token.equals("\"")) {
                            newNode.children.add(node);
                            node = nodeStack.pop();
                        }

                        nodeStack.push(newNode);
                        quotationSeen = false;
                    } else {
                        nodeStack.push(new ASTNode(token));
                        quotationSeen = true;
                    }
                } else {
                    Token oper = operatorStack.peek();

                    while (oper != null && !oper.token.equals("(") && oper.precedence >= token.precedence) {
                        operatorStack.pop();
                        ASTNode newNode = new ASTNode(oper);

                        if (oper.token.equals("!")) {
                            newNode.children.add(nodeStack.pop());
                        } else {
                            newNode.children.add(nodeStack.pop());
                            newNode.children.add(nodeStack.pop());
                        }

                        nodeStack.push(newNode);
                        oper = operatorStack.peek();
                    }

                    operatorStack.push(token);
                }

            } else {
                nodeStack.push(new ASTNode(token));
            }
        }

        while (!operatorStack.isEmpty()) {
            Token oper = operatorStack.pop();
            if (oper.token.equals("!")) {
                ASTNode newNode = new ASTNode(oper);
                newNode.children.add(nodeStack.pop());
                nodeStack.push(newNode);
            } else {
                ASTNode newNode = new ASTNode(oper);
                newNode.children.add(nodeStack.pop());
                newNode.children.add(nodeStack.pop());
                nodeStack.push(newNode);
            }
        }

        ASTNode root = nodeStack.pop();
        if (!nodeStack.isEmpty()) {
            throw new IllegalStateException("WebQueryEngine: buildAST() nodeStack should be empty at end");
        }

        return root;
    }

    /**
     * parses the given AST
     * @param node the current node
     * @return a collection of pages conforming to the query
     */
    private Set<Page> parseTree(ASTNode node) {
        // base case: single word query
        if (!operators.contains(node.token.token)) {
            return index.getPagesWith(node.token.token);
        }

        if (node.token.token.equals("\"")) {
            return handlePhrase(node);
        } else if (node.token.token.equals("!")) {
            Set<Page> result = parseTree(node.children.get(0));
            return negate(result);
        } else {
            Set<Page> left = parseTree(node.children.get(0));
            Set<Page> right = parseTree(node.children.get(1));
            if (node.token.token.equals("|")) {
                left.addAll(right);
            } else if (node.token.token.equals("&")) {
                left.removeIf((Page page) -> !right.contains(page));
            }

            return left;
        }
    }

    /**
     * finds the set of pages the contains the given phrase query
     * note the that phrase is stored in reverse order
     * @param node the phrase query in the tree
     * @return a set of pages containing the phrase query
     */
    private Set<Page> handlePhrase(ASTNode node) {
        ArrayList<ASTNode> children = node.children;
        if (children.size() == 0) {
            return new HashSet<>();
        }

        // get the child that returns the least number of pages to reduce how many pages we have to check
        Set<Page> pages = index.getPagesWith(children.get(0).token.token);
        for (int i = 1; i < children.size(); i++) {
            Set<Page> curr = index.getPagesWith(children.get(i).token.token);
            if (curr.size() < pages.size()) {
                pages = curr;
            }
        }

        pages.removeIf((Page p) -> {
            Set<Integer> locations = index.getLocationsOnPage(children.get(0).token.token, p);
            if (locations.size() == 0) {
                return true;
            }

            for (int i = 1; i < children.size(); i++) {
                ASTNode child = children.get(i);
                Set<Integer> currLocations = index.getLocationsOnPage(child.token.token, p);
                boolean wordExists = false;

                for (int loc : currLocations) {
                    if (locations.contains(loc + i)) {
                        wordExists = true;
                        break;
                    }
                }

                if (!wordExists) {
                    return true;
                }
            }

            return false;
        });

        return pages;
    }

    /**
     * 'negates' the given set by removing elements from the set of all pages
     * probably highly inefficient, but the easiest way to do it
     * @param pages the result of the non-negated query
     * @return the set of all pages except those passed in pages
     */
    private Set<Page> negate(Set<Page> pages) {
        Set<Page> all = index.getAllPages();
        all.removeIf(pages::contains);
        return all;
    }

    /**
     * Helper class to abstract Strings into tokens
     */
    private class Token {
        static final int NOT = 3;
        static final int AND = 2;
        static final int OR = 1;

        String token;
        int precedence;

        Token(String token) {
            this.token = token;
            this.precedence = 0;
        }

        Token(String token, int precedence) {
            this.token = token;
            this.precedence = precedence;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    /**
     * Helper class to build the AST
     */
    private class ASTNode {
        Token token;
        ArrayList<ASTNode> children; // use ArrayList since we don't know how many children a node will have

        ASTNode(Token token) {
            this.token = token;
            children = new ArrayList<>(2);
        }
    }
}
