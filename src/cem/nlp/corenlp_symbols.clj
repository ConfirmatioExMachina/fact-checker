(ns cem.nlp.corenlp-symbols)

(def fill-tags #{"SYM" ; symbols like "% & () * @" etc.
                 "TO" ; "to" as preposition or infinitive marker
                 "EX" ; existential "there"
                 "UH" ; interjections like "oops huh uh goodbye" etc.
                 "POS"}) ; genitive marker "'s"
(def types #{"PERSON" "ORGANIZATION" "LOCATION" "DATE" "TIME" "DURATION" "NUMBER"})
