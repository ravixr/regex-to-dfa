import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// Finite Automaton
public class FA {
    List<HashMap<String, List<Integer>>> adj; // Adjacency list
    Integer iState; // Initial state
    List<Integer> fStates; // Final states
    boolean isDFA = false;
    Set<String> symbolSet;

    public static final String LAMBDA = "λ";

    public FA() {
        adj = new ArrayList<HashMap<String, List<Integer>>>();
        iState = 0;
        fStates = new ArrayList<Integer>();
        isDFA = false;
        symbolSet = new HashSet<>();
    }

    public int size() {
        return adj.size();
    }

    private static String getExpandableTransition(HashMap<String, List<Integer>> q) {
        for (String key : q.keySet()) {
            if (key.length() > 1 && !(key.length() == 2 && key.charAt(0) == '\\')) {
                return key;
            }
        }
        return null;
    }

    public void addTransition(int from, String symbol, int to) {
        var q = adj.get(from).get(symbol);
        if (q == null) {
            q = new ArrayList<Integer>();
            adj.get(from).put(symbol, q);
        }
        if (!q.contains(to)) {
            q.add(to);
        }
    }

    public int addState(int from, String symbol) {
        adj.add(new HashMap<String, List<Integer>>());
        addTransition(from, symbol, size() - 1);
        return size() - 1;
    }

    private static final String operators = "+*()";

    private void getSymbols(String regex) {
        for (int i = 0; i < regex.length(); i++) {
            if (regex.charAt(i) == '\\') {
                symbolSet.add(regex.substring(i, i + 2));
                i++;
            } else if (!operators.contains(regex.charAt(i) + "")) {
                symbolSet.add(regex.substring(i, i + 1));
            }
        }
    }
    
    public static FA fromRegex(String regex) throws IllegalArgumentException {
        FA nfa = new FA();
        nfa.getSymbols(regex);
        nfa.adj.add(new HashMap<String, List<Integer>>());
        List<Integer> destStates = new ArrayList<Integer>();
        nfa.adj.get(nfa.iState).put(regex, destStates);
        nfa.adj.add(new HashMap<String, List<Integer>>());
        destStates.add(nfa.size() - 1);
        nfa.fStates.add(nfa.size() - 1);
        for (int i = 0; i < nfa.size(); i++) {
            HashMap<String, List<Integer>> q = nfa.adj.get(i);
            String s = getExpandableTransition(q);
            while (s != null) {
                int pos = -1;
                char op = ' ';
                for (int j = 0; j < s.length() - 1;) {
                    if (s.charAt(j) == '\\') {
                        j += 2;
                    } else if (s.charAt(j) == '(') {
                        int count = 1;
                        while (count != 0) {
                            j++;
                            if (s.charAt(j) == '(') {
                                count++;
                            } else if (s.charAt(j) == ')') {
                                count--;
                            }
                        }
                        j++;
                    } else {
                        j++;
                    }
                    if (pos == -1 && j == s.length()) {
                        var dest = q.get(s);
                        q.remove(s);
                        s = s.substring(1, s.length() - 1);
                        for (var d : dest) {
                            nfa.addTransition(i, s, d);
                        }
                        j = 0;
                        continue;
                    } else if (j >= s.length()) {
			break;
		    }
                    if (pos == -1 || calcPriority(s.charAt(j)) > calcPriority(op)) {
                        pos = j;
                        if (s.charAt(j) != '+' && s.charAt(j) != '*') {
                            op = '.';
                        } else {
                            op = s.charAt(j);
                        }
                    }
                }
                if (op == '+') {
                    handleUnion(nfa, i, s, pos);
                } else if (op == '*') {
                    handleKleeneStar(nfa, i, s, pos);
                } else if (op == '.') {
                    handleConcatenation(nfa, i, s, pos);
                } else {
                    throw new IllegalArgumentException("Invalid regex");
                }
                s = getExpandableTransition(q);
            }
        }
        // nfa.SaveJFLAPXML("nfa-λ.jff");
        nfa.removeLambdaTransitions();
        // nfa.SaveJFLAPXML("nfa.jff");
        return nfa;
    }

    private static int calcPriority(char symbol) {
        switch (symbol) {
            case '+':
                return 2;
            case '*':
                return 0;
            default:
                return 1;
        }
    }

    // Union case:
    // (currState)-λ->(q1)-leftExp-->(q2)-λ->(destStates)
    //      |------λ->(q3)-rightExp->(q4)-λ----^
    private static void handleUnion(FA nfa, int q, String s, int i) {
        var destStates = nfa.adj.get(q).get(s);
        nfa.adj.get(q).remove(s);
        int q1 = nfa.addState(q, LAMBDA);
        int q2 = nfa.addState(q, LAMBDA);
        int q3 = nfa.addState(q1, s.substring(0, i));
        int q4 = nfa.addState(q2, s.substring(i + 1));
        for (int j = 0; j < destStates.size(); j++) {
            nfa.addTransition(q3, LAMBDA, destStates.get(j));
            nfa.addTransition(q4, LAMBDA, destStates.get(j));
        }
    }

    // Kleene star case:
    // (currState)-λ->(q1)-leftExp->(q2)-λ->(destStates)
    //        ^ |--------------λ-------------^ |
    //        |----------------λ---------------|
    private static void handleKleeneStar(FA nfa, int q, String s, int i) {
        var destStates = nfa.adj.get(q).get(s);
        nfa.adj.get(q).remove(s);
        int q1 = nfa.addState(q, LAMBDA);
        int q2 = nfa.addState(q1, s.substring(0, i));
        for (int j = 0; j < destStates.size(); j++) {
            nfa.addTransition(q, LAMBDA, destStates.get(j));
            nfa.addTransition(q2, LAMBDA, destStates.get(j));
            nfa.addTransition(destStates.get(j), LAMBDA, q);
        }
    }

    // Concatenation case:
    // (currState)-λ->(q1)-leftExp->(q2)-λ->(q3)-rightExp->(q4)-λ->(destStates)
    private static void handleConcatenation(FA nfa, int q, String s, int i) {
        var destStates = nfa.adj.get(q).get(s);
        nfa.adj.get(q).remove(s);
        int q1 = nfa.addState(q, LAMBDA);
        int q2 = nfa.addState(q1, s.substring(0, i));
        int q3 = nfa.addState(q2, LAMBDA);
        int q4 = nfa.addState(q3, s.substring(i));
        for (int j = 0; j < destStates.size(); j++) {
            nfa.addTransition(q4, LAMBDA, destStates.get(j));
        }
    }

    private Set<Integer> lambdaClosure(int q, Map<Integer, Set<Integer>> closureMap, Map<Integer, Boolean> visited) {
        visited.put(q, true);
        Set<Integer> closure = new HashSet<>();
        var dest = adj.get(q).get(LAMBDA) == null ? new ArrayList<Integer>() : adj.get(q).get(LAMBDA);
        for (var to : dest) {
            if (to < q && closureMap.get(to) != null) {
                closure.addAll(closureMap.get(to));
            } else if (visited.get(to) == null) {
                var c = lambdaClosure(to, closureMap, visited);
                closure.addAll(c);
            }
        }
        for (var c : closure) {
            // Check if has a final state in the closure
            if (fStates.contains(c)) {
                fStates.add(q);
                break;
            }
        }
        closure.addAll(dest);
        closure.add(q);
        closureMap.put(q, closure);
        return closure;
    }

    public void removeLambdaTransitions() {
        Map<Integer, Set<Integer>> closureMap = new HashMap<>();
        Map<Integer, Boolean> visited = new HashMap<>();
        for (int i = 0; i < adj.size(); i++) {
            var c = lambdaClosure(i, closureMap, visited);
            closureMap.put(i, c);
            adj.get(i).remove(LAMBDA);
            visited.clear();
        }
        for (var s : symbolSet) {
            for (int i = 0; i < adj.size(); i++) {
                var lClosure = closureMap.get(i);
                if (lClosure == null) {
                    continue;
                }
                for (var to : lClosure) {
                    if (to == i) {
                        continue;
                    }
                    var c = adj.get(to).get(s);
                    if (c == null) {
                        continue;
                    }
                    for (var q : c) {
                        addTransition(i, s, q);
                        var lq = closureMap.get(q);
                        if (lq == null) {
                            continue;
                        }
                        boolean firstFinal = true;
                        for (var l : lq) {
                            if ((firstFinal && fStates.contains(l))
                            || !(adj.get(l).keySet().contains(LAMBDA) && adj.get(l).keySet().size() == 1)) {
                                addTransition(i, s, l);
                                firstFinal = false;
                            }
                        }
                    }
                }
            }
        }
    }
    
    public FA toDFA() {
        FA dfa = new FA();
        HashMap<List<Integer>, Integer> stateMap = new HashMap<>();
        List<List<Integer>> queue = new ArrayList<>();
        List<Integer> iStateSet = new ArrayList<>();
        iStateSet.add(iState);
        stateMap.put(iStateSet, dfa.size());
        if (fStates.contains(iState)) {
            dfa.fStates.add(dfa.size());
        }
        dfa.adj.add(new HashMap<String, List<Integer>>());
        queue.add(iStateSet);
        while (queue.size() > 0) {
            List<Integer> currStateSet = queue.get(0);
            queue.remove(0);
            int currState = stateMap.get(currStateSet);
            Set<String> symbols = new java.util.HashSet<>();
            for (var q : currStateSet) {
                for (var key : adj.get(q).keySet()) {
                    symbols.add(key);
                }
            }
            for (String symbol : symbols) {
                List<Integer> destStateSet = new ArrayList<>();
                for (int i = 0; i < currStateSet.size(); i++) {
                    var dest = adj.get(currStateSet.get(i)).get(symbol);
                    if (dest != null) {
                        for (var to : dest) {
                            if (!destStateSet.contains(to)) {
                                destStateSet.add(to);
                            }
                        }
                    }
                }
                if (destStateSet.size() == 0) {
                    continue;
                }
                destStateSet.sort(null);
                if (!stateMap.containsKey(destStateSet)) {
                    stateMap.put(destStateSet, dfa.size());
                    dfa.adj.add(new HashMap<String, List<Integer>>());
                    for (int i = 0; i < destStateSet.size(); i++) {
                        if (fStates.contains(destStateSet.get(i))) {
                            dfa.fStates.add(dfa.size() - 1);
                            break;
                        }
                    }
                    queue.add(destStateSet);
                }
                int destState = stateMap.get(destStateSet);
                if (dfa.adj.get(currState).get(symbol) == null) {
                    dfa.adj.get(currState).put(symbol, new ArrayList<Integer>());
                }
                dfa.adj.get(currState).get(symbol).add(destState);
            }
        }
        dfa.isDFA = true;
        return dfa;
    }

    public boolean runDFA(String sentence) throws Exception {
        if (!isDFA) {
            throw new IllegalArgumentException("Automaton is not a DFA");
        }
        if (sentence.length() == 0) {
            return fStates.contains(iState);
        }
        Integer currState = iState;
        for (int i = 0; i < sentence.length(); i++) {
            String symbol = "";
            if (sentence.charAt(i) == '\\') {
                symbol = sentence.substring(i, i + 2);
                i++;
            } else {
                symbol = sentence.substring(i, i + 1);
            }
            var dest = adj.get(currState).get(symbol);
            if (dest == null || (currState = dest.get(0)) == null) {
                return false;
            }
        }
        if (fStates.contains(currState)) {
            return true;
        }
        return false;
    }

    private static final String WATERMARK = "<!-- Created by https://github.com/ravixr/regex-to-dfa -->\n";

    /**
     * @return A string containing a JFLAP 7.0 XML compatible description of this automaton.
     */
    public String toJFLAPXML() {
        String xml = "";
        xml += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
        xml += WATERMARK;
        xml += "<structure>&#13;\n";
        xml += "\t<type>fa</type>&#13;\n";
        xml += "\t<automaton>&#13;\n";
        // States
        for (int i = 0; i < adj.size(); i++) {
            xml += "\t\t<state id=\"" + i + "\" name=\"q" + i + "\">&#13;\n";
            xml += "\t\t\t<x>0</x>&#13;\n";
            xml += "\t\t\t<y>0</y>&#13;\n";
            if (i == iState) {
                xml += "\t\t\t<initial/>&#13;\n";
            }
            if (fStates.contains(i)) {
                xml += "\t\t\t<final/>&#13;\n";
            }
            xml += "\t\t</state>&#13;\n";
        }
        // Transitions
        for (int i = 0; i < adj.size(); i++) { 
            for (String symbol : adj.get(i).keySet()) {
                for (int j = 0; j < adj.get(i).get(symbol).size(); j++) {
                    xml += "\t\t<transition>&#13;\n";
                    xml += "\t\t\t<from>" + i + "</from>&#13;\n";
                    xml += "\t\t\t<to>" + adj.get(i).get(symbol).get(j) + "</to>&#13;\n";
                    if (symbol.equals(LAMBDA)) {
                        xml += "\t\t\t<read/>\n";
                    } else {
                        xml += "\t\t\t<read>" + symbol + "</read>&#13;\n";
                    }
                    xml += "\t\t</transition>&#13;\n";
                }
            }
        }
        xml += "\t</automaton>&#13;\n";
        xml += "</structure>\n";
        return xml;
    }

    public void SaveJFLAPXML(String filePath) {
        try {
            filePath = Paths.get("").toAbsolutePath().toString().split("/src")[0] + ("/instances/" + filePath);
            File file = new File(filePath);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(toJFLAPXML());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
