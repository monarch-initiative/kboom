[![Build Status](https://travis-ci.org/monarch-initiative/kboom.svg?branch=master)](https://travis-ci.org/monarch-initiative/kboom)

## BOOM: Bayes OWL Ontology Merging

For a description see:

 * [preprint](http://biorxiv.org/content/early/2016/05/18/048843)  ; latex source: [cmungall/kboom-paper](https://github.com/cmungall/kboom-paper)
 * [PhenoDay presentation](http://f1000research.com/slides/5-1648)

To run:

```
mvn install
bin/kboom -t all-ptable.tsv all.owl -o merged.owl
```

