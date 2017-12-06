(ns cem.nlp.corenlp-symbols)

(def fill-tags #{"SYM" ; symbols like "% & () * @" etc.
                 "TO" ; "to" as preposition or infinitive marker
                 "EX" ; existential "there"
                 "UH" ; interjections like "oops huh uh goodbye" etc.
                 "POS"}) ; genitive marker "'s"
(def clauses #{"csubj" "ccomp" "xcomp" "advcl"})
(def agents #{"nsubj" "csubj" "subj" "nsubj:xsubj"})
(def patients #{"dobj" "obj" "nsubjpass"})
(def verbs #{"VB" "VBD" "VBG" "VBP" "VBN" "VBZ"})
(def keyword-tags #{"IN"})
(def types #{"PERSON" "ORGANIZATION" "LOCATION" "DATE" "TIME" "DURATION" "NUMBER"})
