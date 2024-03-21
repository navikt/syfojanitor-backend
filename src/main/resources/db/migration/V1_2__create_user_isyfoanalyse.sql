DO $$
BEGIN
  CREATE USER "isyfo-analyse";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role isyfo-analyse -- it already exists';
END
$$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO "isyfo-analyse";

