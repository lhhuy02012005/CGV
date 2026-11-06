-- Align Instant columns created by historical V1 with Hibernate's TIMESTAMPTZ mapping.
-- Existing timestamp-without-zone values are interpreted in UTC (the local DB timezone).
DO $$
DECLARE c record;
BEGIN
    FOR c IN SELECT table_name, column_name FROM information_schema.columns
             WHERE table_schema = 'public' AND data_type = 'timestamp without time zone'
               AND table_name IN ('membership_tiers', 'users', 'user_connected_accounts')
    LOOP
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE ''UTC''',
                       c.table_name, c.column_name, c.column_name);
    END LOOP;
END $$;

-- User.version is an int; refuse out-of-range values rather than truncate them.
ALTER TABLE users ALTER COLUMN version TYPE integer USING version::integer;
-- Current account entity uses provider_id and auditing, unlike the old provider_user_id schema.
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema='public'
               AND table_name='user_connected_accounts' AND column_name='provider_user_id')
       AND NOT EXISTS (SELECT FROM information_schema.columns WHERE table_schema='public'
               AND table_name='user_connected_accounts' AND column_name='provider_id') THEN
        ALTER TABLE user_connected_accounts RENAME COLUMN provider_user_id TO provider_id;
    END IF;
END $$;
ALTER TABLE user_connected_accounts ADD COLUMN IF NOT EXISTS provider_email varchar(255);
ALTER TABLE user_connected_accounts ADD COLUMN IF NOT EXISTS access_token text;
ALTER TABLE user_connected_accounts ADD COLUMN IF NOT EXISTS token_expires_at timestamptz;
ALTER TABLE user_connected_accounts ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_connected_accounts ADD COLUMN IF NOT EXISTS updated_at timestamptz DEFAULT CURRENT_TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cgv_provider_provider_id ON user_connected_accounts(provider, provider_id);
