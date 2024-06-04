import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Main {
    static String userInput;
    public static void main(String[] args) throws Exception {
        System.out.println("Enter regex: ");
        userInput = System.console().readLine();
        FA dfa = FA.fromRegex(userInput);
        dfa.SaveJFLAPXML("test.jff");
    }
}

// Finite Automaton
class FA {
    List<HashMap<String, List<Integer>>> transitionList;
    Integer iState;
    List<Integer> fStates;

    public FA() {
        transitionList = new ArrayList<HashMap<String, List<Integer>>>();
        iState = 0;
        fStates = new ArrayList<Integer>();
    }

    public int size() {
        return transitionList.size();
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
        nfa.transitionList.add(new HashMap<String, List<Integer>>());
        List<Integer> destStates = new ArrayList<Integer>();
        nfa.transitionList.get(nfa.iState).put(regex, destStates);
        nfa.transitionList.add(new HashMap<String, List<Integer>>());
        destStates.add(nfa.size() - 1);
        nfa.fStates.add(nfa.size() - 1);

        for (int i = 0; i < nfa.size(); i++) {
            HashMap<String, List<Integer>> q = nfa.transitionList.get(i);
            String s = getExpandableTransition(q);
            List<Integer> qsDestStates = q.get(s);
            while (s != null) {
                q.remove(s);
                String currSymbol = "";
                int j = 0;
                boolean unionFlag = false;
                while (j < s.length()) {
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
                    if (j >= s.length()) {
                        j = s.length() - 1;
                    }
                    if (currSymbol != "" && s.charAt(j) == '*') {
                        // Kleene star case:
                        // (CurrState) -currSymbol-> (CurrState)
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
                    } else if (currSymbol != "" && (s.charAt(j) == '+' || unionFlag)) {
                        // Union case:
                        // (CurrState) -currSymbol,nextSymbol-> (DestStates)
                        List<Integer> dest = q.get(currSymbol);
                        if (dest == null) {
                            dest = new ArrayList<Integer>();
                            q.put(currSymbol, dest);
                        }
                        for (int k = 0; k < qsDestStates.size(); k++) {
                            if (!dest.contains(qsDestStates.get(k))) {
                                dest.add(qsDestStates.get(k));
                            }
                        }
                        currSymbol = "";
                        unionFlag = !unionFlag;
                        j++;
                    } else if (currSymbol != "") {
                        // Concatenation case:
                        // (CurrState) -currSymbol-> (NewState) -RestOfRegex-> (DestStates)
                        nfa.transitionList.add(new HashMap<String, List<Integer>>());
                        var newState = nfa.transitionList.get(nfa.size() - 1);
                        newState.put(s.substring(j), qsDestStates);
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
                }
                s = getExpandableTransition(q);
                qsDestStates = q.get(s);
            }
        }
        return nfa;
    }

    public FA toDFA() {
        FA dfa = new FA();
        return dfa;
    }

    private static final String WATERMARK = "<!-- Created by https://github.com/ravixr/ -->\n";

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
        for (int i = 0; i < transitionList.size(); i++) {
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
        for (int i = 0; i < transitionList.size(); i++) { 
            for (String symbol : transitionList.get(i).keySet()) {
                for (int j = 0; j < transitionList.get(i).get(symbol).size(); j++) {
                    xml += "\t\t<transition>&#13;\n";
                    xml += "\t\t\t<from>" + i + "</from>&#13;\n";
                    xml += "\t\t\t<to>" + transitionList.get(i).get(symbol).get(j) + "</to>&#13;\n";
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
            filePath = Paths.get("").toAbsolutePath().toString().split("/src")[0] + ("/tests/" + filePath);
            File file = new File(filePath);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(toJFLAPXML());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}