# Information Retrieval — Search and Ranking

Information retrieval systems identify documents that are relevant to a user's query.

## Inverted index

An inverted index maps each analyzed term to the documents containing that term.

Posting lists may also store:

- term frequency
- field information
- token positions

Token positions allow the search engine to support exact phrase queries.

## Ranking

BM25 is a widely used relevance-ranking function. It considers term frequency, document length, and inverse document frequency.

Title matches can also receive greater importance than body matches.

## Prefix search

Prefix matching allows a query such as `neur` to match terms such as `neural` and `neuron`.

Quoted phrases should remain exact rather than using prefix expansion.
