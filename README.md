# WebCrawler
A limited :( web crawler

Some code that was cut out of WebQueryEngine:

        if (!isOperator(node.left) && !isOperator(node.right)) {
            // parse operation
            if (node.token.token.equals("|")) {
                Collection<Page> collection = index.getPagesWith(node.left.token.token);
                collection.addAll(index.getPagesWith(node.right.token.token));
                return collection;
            } else if (node.token.token.equals("&")) {
                Collection<Page> collection = index.getPagesWith(node.left.token.token);
                for (Page page : collection) {
                    if (!index.wordOnPage(node.right.token.token, page)) {
                        collection.remove(page);
                    }
                }

                return collection;
            }
        }
        
        // Instead of printing raw whitespace, we're escaping it
        switch(ch[i]) {
            case '\\':
            str.append('\\');
                break;
            case '"':
                addWord(str.toString());
                str = new StringBuilder();
                break;
                case '\n':
                addWord(str.toString());
                str = new StringBuilder();
                break;
            case '\r':
                addWord(str.toString());
                str = new StringBuilder();
                break;
            case '\t':
                addWord(str.toString());
                str = new StringBuilder();
                break;
            default:
                char c = ch[i];
                if (endChars.contains(c)) {
                    addWord(str.toString());
                    str = new StringBuilder();
                } else {
                    str.append(ch[i]);
                }
                break;
        }
