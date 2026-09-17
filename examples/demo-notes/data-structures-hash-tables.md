# Data Structures — Hash Tables

A hash table stores key-value pairs and uses a hash function to map keys to positions in an underlying array.

## Collisions

Different keys may map to the same position. Common collision-resolution strategies include:

- separate chaining
- linear probing
- quadratic probing

With a suitable hash function and controlled load factor, lookup, insertion, and deletion have expected constant-time complexity.

## Resizing

When the load factor becomes too high, the table can allocate a larger array and rehash existing entries.

Hash tables are widely used for dictionaries, caches, symbol tables, and sets.
