package assignment;

import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 */
public class WebQueryEngine {
    private static final HashMap<String, Integer> operators; // holds possible operators and their precedence

    static {
        operators = new HashMap<>();
        operators.put("!", 1);
        operators.put("|", 2);
        operators.put("&", 3);

    }

    private WebIndex index;
    /**
     * Returns a WebQueryEngine that uses the given Index to constructe answers to queries.
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
        query = query.replaceAll("\\s", "");

        ArrayList<Token> tokens = getTokens(query);
        ASTNode root = buildAST(tokens);

        Set<Page> result = parseTree(root);
        System.out.println(result.size());
        return result;
    }

    /**
     * parses the given AST
     * @param node the current node
     * @return a collection of pages conforming to the query
     */
    private Set<Page> parseTree(ASTNode node) {
        // base case: single word query
        if (!isOperator(node)) {
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

        return null;
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
     * separates a query into tokens
     * @param query the given query
     * @return an ArrayList of tokens in the query
     */
    private ArrayList<Token> getTokens(String query) {
        ArrayList<Token> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();

        // split the query into tokens
        for (int i = 0; i < query.length(); i++) {
            String current = Character.toString(query.charAt(i));
            if (operators.containsKey(current) || current.equals("(") || current.equals(")") || current.equals("\"")) {
                if (word.length() != 0) {
                    tokens.add(new Token(word.toString()));
                    word = new StringBuilder();
                }

                tokens.add(new Token(current));
            } else {
                word.append(current);
            }
        }

        if (word.length() != 0) {
            tokens.add(new Token(word.toString()));

        }

        return tokens;
    }

    /**
     * builds an AST from a list of tokens using Djikstra's Shunting-Yard algorithm
     * @param tokens - a list of tokens
     * @return the root of the newly-built AST
     */
    private ASTNode buildAST(ArrayList<Token> tokens) {
        Deque<ASTNode> nodeStack = new ArrayDeque<>();
        Deque<Token> operatorStack = new ArrayDeque<>();
        boolean quotationSeen = false;
        for (Token token : tokens) {
            if (isOperator(token)) {
                Token oper = operatorStack.peek();

                while (oper != null && !oper.token.equals("(") && operators.get(oper.token) >= operators.get(token.token)) {
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
            } else if (token.token.equals("(")) {
                operatorStack.push(token);
            } else if (token.token.equals("\"")) {
                if (quotationSeen) {
                    ASTNode newNode = new ASTNode(token);
                    ASTNode node = nodeStack.pop();
                    while (!node.token.token.equals("\"")) {
                        newNode.children.add(node);
                        node = nodeStack.pop();
                    }

                    nodeStack.push(newNode);
                } else {
                    nodeStack.push(new ASTNode(token));
                    quotationSeen = true;
                }
            } else if (token.token.equals(")")) {
                Token oper = operatorStack.pop();

                while (!oper.token.equals("(")) {
                    ASTNode newNode = new ASTNode(oper);
                    newNode.children.add(nodeStack.pop());
                    newNode.children.add(nodeStack.pop());
                    nodeStack.push(newNode);
                    oper = operatorStack.pop();
                }

                // this should remove the corresponding left bracket from the stack
                operatorStack.pop();
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
     * determines if a given ASTNode holds an operator
     * @param node the node to examine
     * @return true if the node holds an operator, false otherwise
     */
    private boolean isOperator(ASTNode node) {
        return operators.containsKey(node.token.token);
    }

    /**
     * determines if a given token is an operator
     * @param token the token to examine
     * @return true if the given token is an operator, false otherwise
     */
    private boolean isOperator(Token token) {
        return operators.containsKey(token.token);
    }

    /**
     * Helper class to abstract Strings into tokens
     */
    class Token {
        String token;

        Token(String token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    /**
     * Helper class to build the AST
     */
    class ASTNode {
        Token token;
        ArrayList<ASTNode> children; // use ArrayList since we don't know how many children a node will have

        ASTNode(Token token) {
            this.token = token;
            children = new ArrayList<>(2);
        }
    }
}
