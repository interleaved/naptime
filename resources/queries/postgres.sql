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
select
	distinct info.table_schema as schema,
	info.table_name as table_name,
	info.column_name as name,
	info.description as description,
	info.ordinal_position as position,
	info.is_nullable::BOOL as nullable,
	info.data_type as col_type,
	info.is_updatable::BOOL as updatable,
	info.character_maximum_length as max_len,
	info.numeric_precision as precision,
	info.column_default as default_value,
	array_to_string(enum_info.vals, ',') as enum
from
	( with key_columns as (
	select
		r.oid as r_oid,
		c.oid as c_oid,
		n.nspname,
		c.relname,
		r.conname,
		r.contype,
		unnest(r.conkey) as conkey
	from
		pg_catalog.pg_constraint as r,
		pg_catalog.pg_class as c,
		pg_catalog.pg_namespace as n
	where
		r.contype in ('f', 'p', 'u')
		and c.relkind in ('r', 'v', 'f', 'm')
		and r.conrelid = c.oid
		and c.relnamespace = n.oid
		and n.nspname != any ( array[ 'pg_catalog',
		'information_schema' ]) ),
	columns as (
	select
		nc.nspname::NAME as table_schema,
		c.relname::NAME as table_name,
		a.attname::NAME as column_name,
		d.description as description,
		a.attnum::INT8 as ordinal_position,
		pg_get_expr( ad.adbin,
		ad.adrelid )::text as column_default,
		not ( a.attnotnull
			or t.typtype = 'd'
			and t.typnotnull ) as is_nullable,
		case
			when t.typtype = 'd' then
			case
				when bt.typelem != 0::OID
				and bt.typlen = (-1) then 'ARRAY'::text
				when nbt.nspname = 'pg_catalog'::NAME then format_type( t.typbasetype, null )
				else format_type( a.atttypid, a.atttypmod )
			end
			else
			case
				when t.typelem != 0::OID
					and t.typlen = (-1) then 'ARRAY'::text
					when nt.nspname = 'pg_catalog'::NAME then format_type( a.atttypid, null )
					else format_type( a.atttypid, a.atttypmod )
				end
			end::text as data_type,
			information_schema._pg_char_max_length( information_schema._pg_truetypid( a.*,
			t.* ),
			information_schema._pg_truetypmod( a.*,
			t.* ) )::INT8 as character_maximum_length,
			information_schema._pg_numeric_precision( information_schema._pg_truetypid( a.*,
			t.* ),
			information_schema._pg_truetypmod( a.*,
			t.* ) )::INT8 as numeric_precision,
			coalesce( bt.typname, t.typname )::NAME as udt_name,
			( c.relkind in ('r', 'v', 'f')
				and pg_column_is_updatable( c.oid::REGCLASS,
				a.attnum,
				false ) )::BOOL as is_updatable
		from
			pg_attribute as a
		left join key_columns as kc on
			kc.conkey = a.attnum
			and kc.c_oid = a.attrelid
		left join pg_catalog.pg_description as d on
			d.objoid = a.attrelid
			and d.objsubid = a.attnum
		left join pg_attrdef as ad on
			a.attrelid = ad.adrelid
			and a.attnum = ad.adnum
		join ( pg_class as c
		join pg_namespace as nc on
			c.relnamespace = nc.oid ) on
			a.attrelid = c.oid
		join ( pg_type as t
		join pg_namespace as nt on
			t.typnamespace = nt.oid ) on
			a.atttypid = t.oid
		left join ( pg_type as bt
		join pg_namespace as nbt on
			bt.typnamespace = nbt.oid ) on
			t.typtype = 'd'
				and t.typbasetype = bt.oid
			left join ( pg_collation as co
			join pg_namespace as nco on
				co.collnamespace = nco.oid ) on
				a.attcollation = co.oid
					and ( nco.nspname != 'pg_catalog'::NAME
						or co.collname != 'default'::NAME )
				where
					not pg_is_other_temp_schema(nc.oid)
						and a.attnum > 0
						and not a.attisdropped
						and c.relkind in ('r', 'v', 'f', 'm'))
	select
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
	from
		columns
	where
		table_schema not in ('pg_catalog', 'information_schema') ) as info
left join (
	select
		n.nspname as s,
		t.typname as n,
		array_agg( e.enumlabel order by e.enumsortorder ) as vals
	from
		pg_type as t
	join pg_enum as e on
		t.oid = e.enumtypid
	join pg_catalog.pg_namespace as n on
		n.oid = t.typnamespace
	group by
		s,
		n ) as enum_info on
	(info.udt_name = enum_info.n)
order by
	schema,
	"position";

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

-- :name primary-and-foreign-keys-referenced-in-views :? :*
-- :doc returns all the primary and foreign key columns which are referenced in views
with recursive
      pks_fks as (
        -- pk + fk referencing col
        select
          conrelid as resorigtbl,
          unnest(conkey) as resorigcol
        from pg_constraint
        where contype IN ('p', 'f')
        union
        -- fk referenced col
        select
          confrelid,
          unnest(confkey)
        from pg_constraint
        where contype='f'
      ),
      views as (
        select
          c.oid       as view_id,
          n.nspname   as view_schema,
          c.relname   as view_name,
          r.ev_action as view_definition
        from pg_class c
        join pg_namespace n on n.oid = c.relnamespace
        join pg_rewrite r on r.ev_class = c.oid
        where c.relkind in ('v', 'm')
        -- and n.nspname = ANY($1 || $2)
      ),
      transform_json as (
        select
          view_id, view_schema, view_name,
          -- the following formatting is without indentation on purpose
          -- to allow simple diffs, with less whitespace noise
          replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            regexp_replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
            replace(
              view_definition::text,
            -- This conversion to json is heavily optimized for performance.
            -- The general idea is to use as few regexp_replace() calls as possible.
            -- Simple replace() is a lot faster, so we jump through some hoops
            -- to be able to use regexp_replace() only once.
            -- This has been tested against a huge schema with 250+ different views.
            -- The unit tests do NOT reflect all possible inputs. Be careful when changing this!
            -- -----------------------------------------------
            -- pattern           | replacement         | flags
            -- -----------------------------------------------
            -- `<>` in pg_node_tree is the same as `null` in JSON, but due to very poor performance of json_typeof
            -- we need to make this an empty array here to prevent json_array_elements from throwing an error
            -- when the targetList is null.
            -- We'll need to put it first, to make the node protection below work for node lists that start with
            -- null: `(<> ...`, too. This is the case for coldefexprs, when the first column does not have a default value.
               '<>'              , '()'
            -- `,` is not part of the pg_node_tree format, but used in the regex.
            -- This removes all `,` that might be part of column names.
            ), ','               , ''
            -- The same applies for `{` and `}`, although those are used a lot in pg_node_tree.
            -- We remove the escaped ones, which might be part of column names again.
            ), E'\\{'            , ''
            ), E'\\}'            , ''
            -- The fields we need are formatted as json manually to protect them from the regex.
            ), ' :targetList '   , ',"targetList":'
            ), ' :resno '        , ',"resno":'
            ), ' :resorigtbl '   , ',"resorigtbl":'
            ), ' :resorigcol '   , ',"resorigcol":'
            -- Make the regex also match the node type, e.g. `{QUERY ...`, to remove it in one pass.
            ), '{'               , '{ :'
            -- Protect node lists, which start with `({` or `((` from the greedy regex.
            -- The extra `{` is removed again later.
            ), '(('              , '{(('
            ), '({'              , '{({'
            -- This regex removes all unused fields to avoid the need to format all of them correctly.
            -- This leads to a smaller json result as well.
            -- Removal stops at `,` for used fields (see above) and `}` for the end of the current node.
            -- Nesting can't be parsed correctly with a regex, so we stop at `{` as well and
            -- add an empty key for the followig node.
            ), ' :[^}{,]+'       , ',"":'              , 'g'
            -- For performance, the regex also added those empty keys when hitting a `,` or `}`.
            -- Those are removed next.
            ), ',"":}'           , '}'
            ), ',"":,'           , ','
            -- This reverses the "node list protection" from above.
            ), '{('              , '('
            -- Every key above has been added with a `,` so far. The first key in an object doesn't need it.
            ), '{,'              , '{'
            -- pg_node_tree has `()` around lists, but JSON uses `[]`
            ), '('               , '['
            ), ')'               , ']'
            -- pg_node_tree has ` ` between list items, but JSON uses `,`
            ), ' '             , ','
          )::json as view_definition
        from views
      ),
      target_entries as(
        select
          view_id, view_schema, view_name,
          json_array_elements(view_definition->0->'targetList') as entry
        from transform_json
      ),
      results as(
        select
          view_id, view_schema, view_name,
          (entry->>'resno')::int as view_column,
          (entry->>'resorigtbl')::oid as resorigtbl,
          (entry->>'resorigcol')::int as resorigcol
        from target_entries
      ),
      recursion as(
        select r.*
        from results r
        -- where view_schema = ANY ($1)
        union all
        select
          view.view_id,
          view.view_schema,
          view.view_name,
          view.view_column,
          tab.resorigtbl,
          tab.resorigcol
        from recursion view
        join results tab on view.resorigtbl=tab.view_id and view.resorigcol=tab.view_column
      )
      select
        sch.nspname as table_schema,
        tbl.relname as table_name,
        col.attname as table_column_name,
        rec.view_schema,
        rec.view_name,
        vcol.attname as view_column_name
      from recursion rec
      join pg_class tbl on tbl.oid = rec.resorigtbl
      join pg_attribute col on col.attrelid = tbl.oid and col.attnum = rec.resorigcol
      join pg_attribute vcol on vcol.attrelid = rec.view_id and vcol.attnum = rec.view_column
      join pg_namespace sch on sch.oid = tbl.relnamespace
      join pks_fks using (resorigtbl, resorigcol)
      order by view_schema, view_name, view_column_name;
