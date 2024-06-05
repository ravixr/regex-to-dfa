import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


// Finite Automaton
public class FA {
    List<HashMap<String, List<Integer>>> adj; // Adjacency list
    Integer iState; // Initial state
    List<Integer> fStates; // Final states
    boolean isDFA = false;

    public static final String LAMBDA = "λ";

    public FA() {
        adj = new ArrayList<HashMap<String, List<Integer>>>();
        iState = 0;
        fStates = new ArrayList<Integer>();
        isDFA = false;
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

    public static FA fromRegex(String regex) throws IllegalArgumentException {
        FA nfa = new FA();
        nfa.adj.add(new HashMap<String, List<Integer>>());
        List<Integer> destStates = new ArrayList<Integer>();
        nfa.adj.get(nfa.iState).put(regex, destStates);
        nfa.adj.add(new HashMap<String, List<Integer>>());
        destStates.add(nfa.size() - 1);
        nfa.fStates.add(nfa.size() - 1);
        for (int i = 0; i < nfa.size(); i++) {
            HashMap<String, List<Integer>> q = nfa.adj.get(i);
            String s = getExpandableTransition(q);
            List<Integer> qsDestStates = q.get(s);
            while (s != null) {
                q.remove(s);
                String currSymbol = "";
                int j = 0;
                //boolean unionFlag = false;
                //while (j < s.length()) {
                    // Parse the current symbol
                    if (s.charAt(j) == '\\') {
                        currSymbol = s.substring(j, j + 2);
                        j += 2;
                    } else if (s.charAt(j) == '(') {
                        int begin = j, count = 1;
                        while (count != 0) {
                            j++;
                            if (s.charAt(j) == '(') {
                                count++;
                            } else if (s.charAt(j) == ')') {
                                count--;
                            }
                        }
                        currSymbol = s.substring(begin + 1, j);
                        j++;
                    } else {
                        currSymbol = s.substring(j, j + 1);
                        j++;
                    }
                    // Ensure j is within bounds
                    if (j >= s.length()) {
                        j = s.length() - 1;
                    }
                    if (currSymbol != "" && s.charAt(j) == '*') {
                        // Kleene star case:
                        // (CurrState) -currSymbol-> (CurrState) -RestOfRegex-> (DestStates)
                        List<Integer> dest = q.get(currSymbol);
                        if (dest == null) {
                            dest = new ArrayList<Integer>();
                            q.put(currSymbol, dest);
                        }
                        if (!dest.contains(i)) {
                            dest.add(i);
                        }
                        currSymbol = "";
                        j++;
                        if (j < s.length()) {
                            q.put(s.substring(j), qsDestStates);
                        } else { 
                            q.put(LAMBDA, qsDestStates);
                        }
                    } else if (currSymbol != "" && s.charAt(j) == '+') {
                        // Union case:
                        // (CurrState) -currSymbol-> (DestStates) / (CurrState) -RestOfRegex-> (DestStates)
                        List<Integer> dest = q.get(currSymbol);
                        if (dest == null) {
                            dest = new ArrayList<Integer>();
                            q.put(currSymbol, dest);
                        }
                        for (var to : qsDestStates) {
                            if (!dest.contains(to)) {
                                dest.add(to);
                            }
                        }
                        currSymbol = "";
                        j++;
                        q.put(s.substring(j), qsDestStates);
                    } else if (currSymbol != "" ) {
                        // Concatenation case:
                        // (CurrState) -currSymbol-> (NewState) -RestOfRegex-> (DestStates)
                        nfa.adj.add(new HashMap<String, List<Integer>>());
                        var newState = nfa.adj.get(nfa.size() - 1);
                        if (j < s.length()) {
                            newState.put(s.substring(j), qsDestStates);
                        } else { 
                            newState.put(LAMBDA, qsDestStates);
                        }
                        List<Integer> dest = q.get(currSymbol);
                        if (dest == null) {
                            dest = new ArrayList<Integer>();
                            q.put(currSymbol, dest);
                        }
                        dest.add(nfa.size() - 1);
                        currSymbol = "";
                        j = s.length();
                    } else {
                        throw new IllegalArgumentException("Invalid regex");
                    }
                //}
                s = getExpandableTransition(q);
                qsDestStates = q.get(s);
            }
        }
        nfa.removeLambdaTransitions();
        return nfa;
    }

    public void removeLambdaTransitions() {
        for (int i = 0; i < adj.size(); i++) {
            var q = adj.get(i);
            var dest = q.get(LAMBDA);
            if (dest == null) {
                continue;
            }
            for (var j : dest) {
                for (var key : adj.get(j).keySet()) {
                    var newDest = q.get(key);
                        if (newDest == null) {
                            newDest = new ArrayList<Integer>();
                            q.put(key, newDest);
                        }
                    for (var jDest : adj.get(j).get(key)) {
                        if (!newDest.contains(jDest)) {
                            newDest.add(jDest);
                        }
                    }
                }
                if (fStates.contains(j)) {
                    fStates.add(i);
                }
            }
            q.remove(LAMBDA);
        }
        removeUnreachableStates();
    }

    private void removeUnreachableStates() {
        boolean[] visited = new boolean[adj.size()];
        List<Integer> reachableStates = new ArrayList<>();
	    reachableStates.add(iState);
        visited[iState] = true;
        while (reachableStates.size() > 0) {
            int state = reachableStates.get(0);
            reachableStates.remove(0);
            for (String key : adj.get(state).keySet()) {
                var dest = adj.get(state).get(key);
		        for (var to : dest) {
                    if (to != null && !visited[to]) {
                        visited[to] = true;
                        reachableStates.add(to);
                    }
                }
            }
        }
        int unreachable = 0;
        for (int i = 0; i < adj.size(); i++) {
            if (!visited[i]) {
                // Reshift the states and update the transitions
                for (int j = 0; j < adj.size(); j++) {
                    for (String symbol : adj.get(j).keySet()) {
                        var dest = adj.get(j).get(symbol);
                        for (int k = 0; k < dest.size(); k++) {
                            if (dest.get(k) > i) {
                                dest.set(k, dest.get(k) - 1);
                            }
                        }
                    }	
                }
                unreachable++;
            }
        }
        for (int i = 1; i <= unreachable; i++) {
            adj.remove(adj.get(size() - 1));
            if (fStates.contains(size() - 1)) {
                fStates.remove(size() - 1);
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
                    xml += "\t\t\t<read>" + symbol + "</read>&#13;\n";
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
