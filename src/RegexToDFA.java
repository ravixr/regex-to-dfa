import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

public class RegexToDFA {
    static BufferedReader sentenceReader;
    static BufferedReader regexReader;
    static String regex;
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || Arrays.asList(args).contains("--help")) {
            usage();
            return;
        }
        try {
            // Open files
            sentenceReader = new BufferedReader(new FileReader(new File(args[0])));
            regexReader = new BufferedReader(new FileReader(new File(args[1])));
            // Read regex
            regex = regexReader.readLine();
            regexReader.close();
            // Create DFA from regex
            FA dfa = FA.fromRegex(regex).toDFA();
            // Test DFA
            testDFA(dfa);
            // Save DFA as JFLAP XML
            int endPos = args[1].lastIndexOf(".") == -1 ? args[1].length() : args[1].lastIndexOf(".");
            String prefix = args[1].substring(args[1].lastIndexOf("/") + 1, endPos);
            dfa.SaveJFLAPXML(prefix + "-DFA.jff");

            sentenceReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void usage() {
        System.out.println("Usage: java  RegexToDfa.jar [options] <sentence-file> <regex-file>");
        System.out.println("Options:");
        System.out.println("\t--help\t\tPrint this message");
        System.out.println("Details:");
        System.out.println("\t<sentence-file>\tFile containing the sentences to be tested (one per line)");
        System.out.println("\t<regex-file>\tFile containing the regex to be converted to DFA (one line)");
        // Specify the format of the regex
        System.out.println("Regex format:");
        System.out.println("\t- Use '" + FA.LAMBDA + "' for lambda transitions ('blank' character input transitions)");
        System.out.println("\t- Use '()' for grouping (e.g. '(a+b)*' to group 'a+b'");
        System.out.println("\t- Use '+' for union operation");
        System.out.println("\t- Use '*' for Kleene star operation");
        System.out.println("\t- Use '\\' followed by an operator character to use it as a literal (e.g. '\\" + FA.LAMBDA + "' to use '\" + FA.LAMBDA + \"' as a literal)");
    }

    public static void testDFA(FA dfa) throws Exception {
        System.out.println("Testing DFA from regex: " + regex);
        while (sentenceReader.ready()) {
            String sentence = sentenceReader.readLine();
            if (dfa.runDFA(sentence)) {
                System.out.println("[ACCEPTED]: " + sentence);
            } else {
                System.out.println("[REJECTED]: " + sentence);
            }
        }
    }
}