#!/bin/bash

# Array of regular expressions
expressions=(
    "(a+b)*c"
    "a(b+c)d"
    "(ab)*+(cd)*"
    "a(bc)*d+ef"
    "a*b*+c"
    "(a+b+c)*"
    "a(b+c)*d"
    "(a+b)*a(a+b)*"
    "ab+c(de+f)"
    "((a+b)(c+d))*"
)

# Array of sentences
sentences=(
    "c\nac\nbc\nabac\nbabc\n"
    "abd\nacd\nabcd\nad\naabdc\n"
    "\nab\ncd\nababcd\ncdcdcd\n"
    "ad\nabcd\nabcbd\nef\na\n"
    "\na\nb\naaabbb\nc\n"
    "\nabc\naabbcc\ncab\nbcaacb\n"
    "ad\nabcd\nabbbd\nabcbcbcbcd\nacd\n"
    "a\nba\nabaa\nbaba\nbbbbba\n"
    "abcde\nabcf\nabcdef\nab\ncde\n"
    "ac\nbd\nacbd\nacacac\nbdac\n"
)

# Create and populate the test files
for i in {0..9}; do
    regex_file="regex-$((i+1)).txt"
    sentence_file="sentence-$((i+1)).txt"
    
    # Write the regular expression to the regex file
    echo -e "${expressions[i]}" > "$regex_file"
    
    # Write the sentences to the sentence file
    echo -e "${sentences[i]}" > "$sentence_file"

# Run the tests
cd ..

for i in {0..9}; do
    java -jar RegexToDFA ../tests/sentence-$((i+1)).txt ../tests/regex-$((i+1)).txt
done
