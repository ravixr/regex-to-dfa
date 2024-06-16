
# Regex to DFA Converter

Este projeto converte expressões regulares em Autômatos Finitos Determinísticos (DFA) e permite testar sentenças com o DFA gerado. 

## Arquivos no Projeto

- `FA.java`: Implementa a classe Finite Automaton (FA) que manipula a conversão de expressões regulares para autômatos finitos e outras operações relacionadas.
- `RegexToDFA.java`: Contém a lógica principal para ler a expressão regular e as sentenças a partir de arquivos, converter a expressão para DFA, testar as sentenças e salvar o DFA em formato JFLAP.
- `run_tests.sh`: Script de shell para criar arquivos de teste e executar testes automaticamente.

## Pré-requisitos

- JDK 11 ou superior.

## Compilação

Para compilar o projeto, use o comando `javac`:

```sh
javac FA.java RegexToDFA.java
```

Este comando irá gerar os arquivos `.class` necessários para executar o programa.

## Execução

### Ajuda

Para ver a ajuda e os detalhes do uso:

```sh
java RegexToDFA --help
```

### Uso

Para executar o programa, forneça o caminho para o arquivo de sentenças e o arquivo de regex:

```sh
java RegexToDFA <arquivo-de-sentencas> <arquivo-de-regex>
```

### Exemplo

```sh
java RegexToDFA sentences.txt regex.txt
```

## Testes Automáticos

Um script `run_tests.sh` está incluído para criar arquivos de teste e executar testes automaticamente. Para usá-lo:

1. Navegue até a pasta do script.
2. Execute o script:

```sh
./run_tests.sh
```

## Estrutura dos Arquivos de Entrada

### Arquivo de Regex

O arquivo de regex deve conter uma única linha com a expressão regular a ser convertida.

Exemplo (`regex.txt`):

```plaintext
(a+b)c
```

### Arquivo de Sentenças

O arquivo de sentenças deve conter uma sentença por linha.

Exemplo (`sentences.txt`):

```plaintext
ac
bc
abac
```

## Saída

O programa exibirá no console se cada sentença foi aceita ou rejeitada pelo DFA gerado a partir da expressão regular. Além disso, um arquivo `.jff` (JFLAP) será gerado contendo a definição do DFA.

## Releases

Você pode baixar o arquivo JAR na seção de releases do repositório no GitHub. [Seção de Releases](https://github.com/ravixr/regex-to-dfa/releases)
