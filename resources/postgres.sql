-- :name all-tables :? :*
-- :doc get salient information on all tables in a database
SELECT
	n.nspname AS table_schema,
	c.relname AS table_name,
	NULL AS table_description,
	(
		c.relkind IN ('r', 'v', 'f')
		AND (
			pg_relation_is_updatable(
				c.oid::REGCLASS,
				false
			)
			& 8
		)
		= 8
		OR EXISTS(
			SELECT
				1
				FROM
					pg_trigger
			 WHERE
					pg_trigger.tgrelid = c.oid
				 AND (pg_trigger.tgtype::INT8 & 69) = 69
		)
	)
		AS insertable
  FROM
	  pg_class AS c
	  JOIN pg_namespace AS n ON n.oid = c.relnamespace
 WHERE
	c.relkind IN ('v', 'r', 'm', 'f')
	 AND n.nspname
	 NOT IN ('pg_catalog', 'information_schema')
 GROUP BY
	table_schema, table_name, insertable
 ORDER BY
	table_schema, table_name;

-- :name all-columns :? :*
-- :doc get information on all non-key/non-fk columns in the database
SELECT
	DISTINCT
	info.table_schema AS schema,
	info.table_name AS table_name,
	info.column_name AS name,
	info.description AS description,
	info.ordinal_position AS position,
	info.is_nullable::BOOL AS nullable,
	info.data_type AS col_type,
	info.is_updatable::BOOL AS updatable,
	info.character_maximum_length AS max_len,
	info.numeric_precision AS precision,
	info.column_default AS default_value,
	array_to_string(enum_info.vals, ',') AS enum
FROM
	(
		WITH
			key_columns
				AS (
					SELECT
						r.oid AS r_oid,
						c.oid AS c_oid,
						n.nspname,
						c.relname,
						r.conname,
						r.contype,
						unnest(r.conkey) AS conkey
					FROM
						pg_catalog.pg_constraint AS r,
						pg_catalog.pg_class AS c,
						pg_catalog.pg_namespace AS n
					WHERE
						r.contype IN ('f', 'p', 'u')
						AND c.relkind
							IN ('r', 'v', 'f', 'm')
						AND r.conrelid = c.oid
						AND c.relnamespace = n.oid
						AND n.nspname
							!= ANY (
									ARRAY[
										'pg_catalog',
										'information_schema'
									]
								)
				),
			columns
				AS (
					SELECT
						nc.nspname::NAME AS table_schema,
						c.relname::NAME AS table_name,
						a.attname::NAME AS column_name,
						d.description AS description,
						a.attnum::INT8 AS ordinal_position,
						pg_get_expr(
							ad.adbin,
							ad.adrelid
						)::STRING
							AS column_default,
						NOT
							(
								a.attnotnull
								OR t.typtype = 'd'
									AND t.typnotnull
							)
							AS is_nullable,
						CASE
						WHEN t.typtype = 'd'
						THEN CASE
						WHEN bt.typelem != 0::OID
						AND bt.typlen = (-1)
						THEN 'ARRAY'::STRING
						WHEN nbt.nspname
						= 'pg_catalog'::NAME
						THEN format_type(
							t.typbasetype,
							NULL::INT8
						)
						ELSE format_type(
							a.atttypid,
							a.atttypmod
						)
						END
						ELSE CASE
						WHEN t.typelem != 0::OID
						AND t.typlen = (-1)
						THEN 'ARRAY'::STRING
						WHEN nt.nspname = 'pg_catalog'::NAME
						THEN format_type(
							a.atttypid,
							NULL::INT8
						)
						ELSE format_type(
							a.atttypid,
							a.atttypmod
						)
						END
						END::STRING
							AS data_type,
						information_schema._pg_char_max_length(
							information_schema._pg_truetypid(
								a.*,
								t.*
							),
							information_schema._pg_truetypmod(
								a.*,
								t.*
							)
						)::INT8
							AS character_maximum_length,
						information_schema._pg_numeric_precision(
							information_schema._pg_truetypid(
								a.*,
								t.*
							),
							information_schema._pg_truetypmod(
								a.*,
								t.*
							)
						)::INT8
							AS numeric_precision,
						COALESCE(
							bt.typname,
							t.typname
						)::NAME
							AS udt_name,
						(
							c.relkind IN ('r', 'v', 'f')
							AND pg_column_is_updatable(
									c.oid::REGCLASS,
									a.attnum,
									false
								)
						)::BOOL
							AS is_updatable
					FROM
						pg_attribute AS a
						LEFT JOIN key_columns AS kc ON
								kc.conkey = a.attnum
								AND kc.c_oid = a.attrelid
						LEFT JOIN pg_catalog.pg_description
								AS d ON
								d.objoid = a.attrelid
								AND d.objsubid = a.attnum
						LEFT JOIN pg_attrdef AS ad ON
								a.attrelid = ad.adrelid
								AND a.attnum = ad.adnum
						JOIN (
								pg_class AS c
								JOIN pg_namespace AS nc ON
										c.relnamespace
										= nc.oid
							) ON a.attrelid = c.oid
						JOIN (
								pg_type AS t
								JOIN pg_namespace AS nt ON
										t.typnamespace
										= nt.oid
							) ON a.atttypid = t.oid
						LEFT JOIN (
								pg_type AS bt
								JOIN pg_namespace AS nbt ON
										bt.typnamespace
										= nbt.oid
							) ON
								t.typtype = 'd'
								AND t.typbasetype = bt.oid
						LEFT JOIN (
								pg_collation AS co
								JOIN pg_namespace AS nco ON
										co.collnamespace
										= nco.oid
							) ON
								a.attcollation = co.oid
								AND (
										nco.nspname
										!= 'pg_catalog'::NAME
										OR co.collname
											!= 'default'::NAME
									)
					WHERE
						NOT pg_is_other_temp_schema(nc.oid)
						AND a.attnum > 0
						AND NOT a.attisdropped
						AND c.relkind
							IN ('r', 'v', 'f', 'm')
						AND (kc.r_oid IS NOT NULL)
				)
		SELECT
			table_schema,
			table_name,
			column_name,
			description,
			ordinal_position,
			is_nullable,
			data_type,
			is_updatable,
			character_maximum_length,
			numeric_precision,
			column_default,
			udt_name
		FROM
			columns
		WHERE
			table_schema
			NOT IN ('pg_catalog', 'information_schema')
	)
		AS info
	LEFT JOIN (
			SELECT
				n.nspname AS s,
				t.typname AS n,
				array_agg(
					e.enumlabel ORDER BY e.enumsortorder
				)
					AS vals
			FROM
				pg_type AS t
				JOIN pg_enum AS e ON t.oid = e.enumtypid
				JOIN pg_catalog.pg_namespace AS n ON
						n.oid = t.typnamespace
			GROUP BY
				s, n
		)
			AS enum_info ON (info.udt_name = enum_info.n)
ORDER BY
	schema, "position";

-- :name many-to-one :? :*
-- :doc get information on foriegn keys
SELECT
	ns1.nspname AS table_schema,
	tab.relname AS table_name,
	conname AS constraint_name,
	column_info.cols AS columns,
	ns2.nspname AS foreign_table_schema,
	other.relname AS foreign_table_name,
	column_info.refs AS foreign_columns
FROM
	pg_constraint,
	LATERAL (
		SELECT
			array_agg(cols.attname) AS cols,
			array_agg(cols.attnum) AS nums,
			array_agg(refs.attname) AS refs
		FROM
			(
				SELECT
					unnest(conkey) AS col,
					unnest(confkey) AS ref
			)
				AS k,
			LATERAL (
				SELECT
					*
				FROM
					pg_attribute
				WHERE
					attrelid = conrelid AND attnum = col
			)
				AS cols,
			LATERAL (
				SELECT
					*
				FROM
					pg_attribute
				WHERE
					attrelid = confrelid AND attnum = ref
			)
				AS refs
	)
		AS column_info,
	LATERAL (
		SELECT
			*
		FROM
			pg_namespace
		WHERE
			pg_namespace.oid = connamespace
	)
		AS ns1,
	LATERAL (
		SELECT * FROM pg_class WHERE pg_class.oid = conrelid
	)
		AS tab,
	LATERAL (
		SELECT
			*
		FROM
			pg_class
		WHERE
			pg_class.oid = confrelid
	)
		AS other,
	LATERAL (
		SELECT
			*
		FROM
			pg_namespace
		WHERE
			pg_namespace.oid = other.relnamespace
	)
		AS ns2
WHERE
	confrelid != 0
ORDER BY
	(conrelid, column_info.nums);

-- :name all-primary-keys :? :*
-- :doc returns information on all the primary keys
-- CTE to replace information_schema.table_constraints to remove owner limit
WITH
	tc
		AS (
			SELECT
				c.conname::NAME AS constraint_name,
				nr.nspname::NAME AS table_schema,
				r.relname::NAME AS table_name
			FROM
				pg_namespace AS nc,
				pg_namespace AS nr,
				pg_constraint AS c,
				pg_class AS r
			WHERE
				nc.oid = c.connamespace
				AND nr.oid = r.relnamespace
				AND c.conrelid = r.oid
				AND r.relkind = 'r'
				AND NOT pg_is_other_temp_schema(nr.oid)
				AND c.contype = 'p'
		),
	kc
		AS (
			SELECT
				ss.conname::NAME AS constraint_name,
				ss.nr_nspname::NAME AS table_schema,
				ss.relname::NAME AS table_name,
				a.attname::NAME AS column_name,
				(ss.x).n::INT8 AS ordinal_position,
				CASE
				WHEN ss.contype = 'f'
				THEN information_schema._pg_index_position(
					ss.conindid,
					ss.confkey[(ss.x).n]
				)
				ELSE NULL::INT8
				END::INT8
					AS position_in_unique_constraint
			FROM
				pg_attribute AS a,
				(
					SELECT
						r.oid AS roid,
						r.relname,
						r.relowner,
						nc.nspname AS nc_nspname,
						nr.nspname AS nr_nspname,
						c.oid AS coid,
						c.conname,
						c.contype,
						c.conindid,
						c.confkey,
						information_schema._pg_expandarray(
							c.conkey
						)
							AS x
					FROM
						pg_namespace AS nr,
						pg_class AS r,
						pg_namespace AS nc,
						pg_constraint AS c
					WHERE
						nr.oid = r.relnamespace
						AND r.oid = c.conrelid
						AND nc.oid = c.connamespace
						AND c.contype IN ('p', 'u', 'f')
						AND r.relkind = 'r'
						AND NOT
								pg_is_other_temp_schema(
									nr.oid
								)
				)
					AS ss
			WHERE
				ss.roid = a.attrelid
				AND a.attnum = (ss.x).x
				AND NOT a.attisdropped
		)
SELECT
	kc.table_schema, kc.table_name, kc.column_name
FROM
	tc, kc
WHERE
	kc.table_name = tc.table_name
	AND kc.table_schema = tc.table_schema
	AND kc.constraint_name = tc.constraint_name
	AND kc.table_schema
		NOT IN ('pg_catalog', 'information_schema');

-- :name all-stored-procedures :? :*
-- :doc returns info on all functions & stored procedures
WITH RECURSIVE
  rec_types AS (
    SELECT
      oid,
      typbasetype,
      COALESCE(NULLIF(typbasetype, 0), oid) AS base
    FROM pg_type
    UNION
    SELECT
      t.oid,
      b.typbasetype,
      COALESCE(NULLIF(b.typbasetype, 0), b.oid) AS base
    FROM rec_types t
    JOIN pg_type b ON t.typbasetype = b.oid
  ),
  base_types AS (
    SELECT
      oid,
      base
    FROM rec_types
    WHERE typbasetype = 0
  )
  SELECT
    pn.nspname AS proc_schema,
    p.proname AS proc_name,
    d.description AS proc_description,
    pg_get_function_arguments(p.oid) AS args,
    tn.nspname AS schema,
    COALESCE(comp.relname, t.typname) AS name,
    p.proretset AS rettype_is_setof,
    (t.typtype = 'c'
     -- Only pg pseudo type that is a row type is 'record'
     or t.typtype = 'p' and t.typname = 'record'
     -- if any INOUT or OUT arguments present, treat as composite
     or COALESCE(proargmodes::text[] && '{b,o}', false)
    ) AS rettype_is_composite,
    p.provolatile
  FROM pg_proc p
  JOIN pg_namespace pn ON pn.oid = p.pronamespace
  JOIN base_types bt ON bt.oid = p.prorettype
  JOIN pg_type t ON t.oid = bt.base
  JOIN pg_namespace tn ON tn.oid = t.typnamespace
  LEFT JOIN pg_class comp ON comp.oid = t.typrelid
  LEFT JOIN pg_catalog.pg_description as d ON d.objoid = p.oid;
