OBO := http://purl.obolibrary.org/obo

src/test/resources/ontologies/%.obo:
	wget $(OBO)/$*.obo -O $@

src/test/resources/data/%.owl:
	wget $(OBO)/upheno/data/$*.owl  -O $@

src/test/resources/data/%.ttl:
	wget $(OBO)/upheno/data/$*.ttl  -O $@
