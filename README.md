# WebCrawler
A limited :( web crawler

Some code that was cut out of WebQueryEngine:
        
        
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
