# About `naptime.layers`

Naptime is divided into layers

# 1 - meta

This is the lowest possible layer.  It contains queries which gather information about a database.

# 2 - dsl

The next layer up contains a parser for translating postgrest-style query parameters to a usable hiccup-style data structure.

# 3 - honey

The next layer up contains functions for translating the parsed DSL into a honeysql map.

# 4 - query

Layer four ties the previous 3 layers together and produces a map of <action>-<entity> to function. This layer will perform queries.

# 5 - router

Layer 5 produces routes which map to level 4.  They are consumable by reitit.
