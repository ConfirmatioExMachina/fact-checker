# fact-checker
This graph-based fact checker was a miniproject as part of the lecture *Statistical Natural Language Processing* at the *University Paderborn*.

## Approach
This fact checker uses a concept graph created from Wikipedia articles as a corpus to check facts against. When given a fact it will create a small concept graph for the fact and will perform a graph matching for evaluation. To calculate a confidence score for the fact bewteen -1 (False) and 1 (True) the fact checker will search in the indexed concept graph for matching nodes via the string similarity. With that nodes it will perform a look up for undirected paths and compare the paths to the gramatical and syntactical structure of the given fact. For further informations see the slides in the doc-repository.

## Requirements
* Java 1.8
* Leiningen 2.8
* Neo4j 3.3
* Elasticsearch 6.1

Application uses Neo4j and Elasticsearch credentials, URLs and ports from `resources/db.properties`.

## Getting started
``` shell
git clone --recursive https://github.com/ConfirmatioExMachina/fact-checker.git
cd fact-checker
lein repl
```
