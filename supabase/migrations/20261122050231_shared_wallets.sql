-- NotePay Phase 4: Shared Wallets Schema, RLS Policies, Invite RPC & Realtime
-- Local-first personal wallets remain SQLite-only on device; only shared wallets live in Supabase.

CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC, anon;
GRANT USAGE ON SCHEMA private TO authenticated, service_role;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typname = 'shared_wallet_role'
    ) THEN
        CREATE TYPE public.shared_wallet_role AS ENUM ('OWNER', 'EDITOR', 'VIEWER');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS public.shared_wallets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL CHECK (char_length(trim(name)) BETWEEN 1 AND 50),
    currency text NOT NULL DEFAULT 'VND' CHECK (char_length(trim(currency)) BETWEEN 3 AND 10),
    purpose text NOT NULL DEFAULT 'SHARED',
    monthly_budget_minor bigint CHECK (monthly_budget_minor IS NULL OR monthly_budget_minor >= 0),
    created_by uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shared_wallets_created_by
    ON public.shared_wallets (created_by);

CREATE TABLE IF NOT EXISTS public.shared_wallet_members (
    wallet_id uuid NOT NULL REFERENCES public.shared_wallets(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name text NOT NULL DEFAULT '',
    role public.shared_wallet_role NOT NULL DEFAULT 'VIEWER',
    spending_limit_minor bigint CHECK (spending_limit_minor IS NULL OR spending_limit_minor >= 0),
    joined_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (wallet_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_shared_wallet_members_user_id
    ON public.shared_wallet_members (user_id, wallet_id);

CREATE TABLE IF NOT EXISTS public.shared_wallet_invites (
    code text PRIMARY KEY CHECK (char_length(trim(code)) >= 6),
    wallet_id uuid NOT NULL REFERENCES public.shared_wallets(id) ON DELETE CASCADE,
    role public.shared_wallet_role NOT NULL CHECK (role IN ('EDITOR', 'VIEWER')),
    created_by uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    max_uses integer NOT NULL DEFAULT 1 CHECK (max_uses > 0),
    used_count integer NOT NULL DEFAULT 0 CHECK (used_count >= 0 AND used_count <= max_uses),
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shared_wallet_invites_wallet_id
    ON public.shared_wallet_invites (wallet_id);

CREATE INDEX IF NOT EXISTS idx_shared_wallet_invites_created_by
    ON public.shared_wallet_invites (created_by);

CREATE TABLE IF NOT EXISTS public.shared_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_id uuid NOT NULL UNIQUE,
    wallet_id uuid NOT NULL REFERENCES public.shared_wallets(id) ON DELETE CASCADE,
    created_by uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    created_by_name text NOT NULL DEFAULT '',
    type text NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    category_id text NOT NULL CHECK (char_length(trim(category_id)) > 0),
    note text NOT NULL DEFAULT '' CHECK (char_length(note) <= 200),
    occurred_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_shared_transactions_wallet_updated
    ON public.shared_transactions (wallet_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_shared_transactions_created_by
    ON public.shared_transactions (created_by);

-- Helper in private schema prevents infinite RLS recursion when querying shared_wallet_members.
CREATE OR REPLACE FUNCTION private.user_wallet_role(p_wallet_id uuid)
RETURNS public.shared_wallet_role
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT m.role
    FROM public.shared_wallet_members m
    WHERE m.wallet_id = p_wallet_id
      AND m.user_id = (select auth.uid())
    LIMIT 1;
$$;

REVOKE ALL ON FUNCTION private.user_wallet_role(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.user_wallet_role(uuid) TO authenticated, service_role;

-- Auto-enroll the creator as OWNER whenever a shared wallet is created.
CREATE OR REPLACE FUNCTION private.handle_new_shared_wallet()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_display_name text;
BEGIN
    SELECT COALESCE(
        NULLIF(trim(u.raw_user_meta_data ->> 'full_name'), ''),
        NULLIF(trim(u.raw_user_meta_data ->> 'name'), ''),
        NULLIF(split_part(COALESCE(u.email, ''), '@', 1), ''),
        'Owner'
    )
    INTO v_display_name
    FROM auth.users u
    WHERE u.id = NEW.created_by;

    INSERT INTO public.shared_wallet_members (wallet_id, user_id, display_name, role)
    VALUES (NEW.id, NEW.created_by, COALESCE(v_display_name, 'Owner'), 'OWNER')
    ON CONFLICT (wallet_id, user_id) DO NOTHING;

    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION private.handle_new_shared_wallet() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS on_shared_wallet_created ON public.shared_wallets;
CREATE TRIGGER on_shared_wallet_created
    AFTER INSERT ON public.shared_wallets
    FOR EACH ROW
    EXECUTE FUNCTION private.handle_new_shared_wallet();

-- Keep updated_at fresh on shared_transactions updates.
CREATE OR REPLACE FUNCTION private.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
BEGIN
    IF NEW.updated_at IS NULL OR NEW.updated_at = OLD.updated_at THEN
        NEW.updated_at := now();
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS touch_shared_wallets_updated_at ON public.shared_wallets;
CREATE TRIGGER touch_shared_wallets_updated_at
    BEFORE UPDATE ON public.shared_wallets
    FOR EACH ROW
    EXECUTE FUNCTION private.touch_updated_at();

DROP TRIGGER IF EXISTS touch_shared_transactions_updated_at ON public.shared_transactions;
CREATE TRIGGER touch_shared_transactions_updated_at
    BEFORE UPDATE ON public.shared_transactions
    FOR EACH ROW
    EXECUTE FUNCTION private.touch_updated_at();

-- Explicit table privileges: deny anon, grant authenticated + service_role.
REVOKE ALL ON public.shared_wallets FROM PUBLIC, anon;
REVOKE ALL ON public.shared_wallet_members FROM PUBLIC, anon;
REVOKE ALL ON public.shared_wallet_invites FROM PUBLIC, anon;
REVOKE ALL ON public.shared_transactions FROM PUBLIC, anon;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.shared_wallets TO authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.shared_wallet_members TO authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.shared_wallet_invites TO authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.shared_transactions TO authenticated, service_role;

-- Enable RLS on 100% of exposed public tables.
ALTER TABLE public.shared_wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_wallet_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_wallet_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_transactions ENABLE ROW LEVEL SECURITY;

-- Policies: public.shared_wallets
CREATE POLICY "shared_wallets_select_member_or_creator"
    ON public.shared_wallets
    FOR SELECT
    TO authenticated
    USING (
        private.user_wallet_role(id) IS NOT NULL
        OR created_by = (select auth.uid())
    );

CREATE POLICY "shared_wallets_insert_authenticated_owner"
    ON public.shared_wallets
    FOR INSERT
    TO authenticated
    WITH CHECK (created_by = (select auth.uid()));

CREATE POLICY "shared_wallets_update_owner"
    ON public.shared_wallets
    FOR UPDATE
    TO authenticated
    USING (private.user_wallet_role(id) = 'OWNER')
    WITH CHECK (private.user_wallet_role(id) = 'OWNER');

CREATE POLICY "shared_wallets_delete_owner"
    ON public.shared_wallets
    FOR DELETE
    TO authenticated
    USING (private.user_wallet_role(id) = 'OWNER');

-- Policies: public.shared_wallet_members (no global user listing; scoped strictly to wallet members)
CREATE POLICY "shared_wallet_members_select_same_wallet"
    ON public.shared_wallet_members
    FOR SELECT
    TO authenticated
    USING (private.user_wallet_role(wallet_id) IS NOT NULL);

CREATE POLICY "shared_wallet_members_insert_owner"
    ON public.shared_wallet_members
    FOR INSERT
    TO authenticated
    WITH CHECK (private.user_wallet_role(wallet_id) = 'OWNER');

CREATE POLICY "shared_wallet_members_update_owner"
    ON public.shared_wallet_members
    FOR UPDATE
    TO authenticated
    USING (
        private.user_wallet_role(wallet_id) = 'OWNER'
        AND user_id <> (select auth.uid())
    )
    WITH CHECK (
        private.user_wallet_role(wallet_id) = 'OWNER'
        AND user_id <> (select auth.uid())
        AND role IN ('EDITOR', 'VIEWER')
    );

CREATE POLICY "shared_wallet_members_delete_owner_or_self"
    ON public.shared_wallet_members
    FOR DELETE
    TO authenticated
    USING (
        (private.user_wallet_role(wallet_id) = 'OWNER' AND user_id <> (select auth.uid()))
        OR (user_id = (select auth.uid()) AND role <> 'OWNER')
    );

-- Policies: public.shared_wallet_invites (OWNER-only management)
CREATE POLICY "shared_wallet_invites_select_owner"
    ON public.shared_wallet_invites
    FOR SELECT
    TO authenticated
    USING (private.user_wallet_role(wallet_id) = 'OWNER');

CREATE POLICY "shared_wallet_invites_insert_owner"
    ON public.shared_wallet_invites
    FOR INSERT
    TO authenticated
    WITH CHECK (
        private.user_wallet_role(wallet_id) = 'OWNER'
        AND created_by = (select auth.uid())
    );

CREATE POLICY "shared_wallet_invites_update_owner"
    ON public.shared_wallet_invites
    FOR UPDATE
    TO authenticated
    USING (private.user_wallet_role(wallet_id) = 'OWNER')
    WITH CHECK (private.user_wallet_role(wallet_id) = 'OWNER');

CREATE POLICY "shared_wallet_invites_delete_owner"
    ON public.shared_wallet_invites
    FOR DELETE
    TO authenticated
    USING (private.user_wallet_role(wallet_id) = 'OWNER');

-- Policies: public.shared_transactions
-- VIEWER/EDITOR/OWNER can SELECT; only OWNER or EDITOR can INSERT; EDITOR updates/deletes only own rows, OWNER any row.
CREATE POLICY "shared_transactions_select_member"
    ON public.shared_transactions
    FOR SELECT
    TO authenticated
    USING (private.user_wallet_role(wallet_id) IS NOT NULL);

CREATE POLICY "shared_transactions_insert_editor_or_owner"
    ON public.shared_transactions
    FOR INSERT
    TO authenticated
    WITH CHECK (
        private.user_wallet_role(wallet_id) IN ('OWNER', 'EDITOR')
        AND created_by = (select auth.uid())
    );

CREATE POLICY "shared_transactions_update_owner_or_own_editor"
    ON public.shared_transactions
    FOR UPDATE
    TO authenticated
    USING (
        private.user_wallet_role(wallet_id) = 'OWNER'
        OR (
            private.user_wallet_role(wallet_id) = 'EDITOR'
            AND created_by = (select auth.uid())
        )
    )
    WITH CHECK (
        private.user_wallet_role(wallet_id) = 'OWNER'
        OR (
            private.user_wallet_role(wallet_id) = 'EDITOR'
            AND created_by = (select auth.uid())
        )
    );

CREATE POLICY "shared_transactions_delete_owner_or_own_editor"
    ON public.shared_transactions
    FOR DELETE
    TO authenticated
    USING (
        private.user_wallet_role(wallet_id) = 'OWNER'
        OR (
            private.user_wallet_role(wallet_id) = 'EDITOR'
            AND created_by = (select auth.uid())
        )
    );

-- RPC: join_shared_wallet(invite_code text, p_display_name text DEFAULT '')
-- Validates invite code atomically (not revoked, not expired, remaining uses > 0) before adding member.
CREATE OR REPLACE FUNCTION public.join_shared_wallet(
    invite_code text,
    p_display_name text DEFAULT ''
)
RETURNS TABLE (
    wallet_id uuid,
    wallet_name text,
    currency text,
    monthly_budget_minor bigint,
    role public.shared_wallet_role
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid uuid;
    v_invite public.shared_wallet_invites%ROWTYPE;
    v_existing_role public.shared_wallet_role;
    v_wallet public.shared_wallets%ROWTYPE;
    v_resolved_name text;
BEGIN
    v_uid := (select auth.uid());
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501';
    END IF;

    SELECT *
    INTO v_invite
    FROM public.shared_wallet_invites i
    WHERE i.code = upper(trim(COALESCE(invite_code, '')))
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'INVITE_NOT_FOUND' USING ERRCODE = 'P0001';
    END IF;

    IF v_invite.revoked_at IS NOT NULL THEN
        RAISE EXCEPTION 'INVITE_REVOKED' USING ERRCODE = 'P0001';
    END IF;

    IF v_invite.expires_at <= now() THEN
        RAISE EXCEPTION 'INVITE_EXPIRED' USING ERRCODE = 'P0001';
    END IF;

    IF v_invite.used_count >= v_invite.max_uses THEN
        RAISE EXCEPTION 'INVITE_MAX_USES_REACHED' USING ERRCODE = 'P0001';
    END IF;

    SELECT *
    INTO v_wallet
    FROM public.shared_wallets w
    WHERE w.id = v_invite.wallet_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'WALLET_NOT_FOUND' USING ERRCODE = 'P0001';
    END IF;

    SELECT m.role
    INTO v_existing_role
    FROM public.shared_wallet_members m
    WHERE m.wallet_id = v_invite.wallet_id
      AND m.user_id = v_uid;

    IF FOUND THEN
        RETURN QUERY SELECT
            v_wallet.id,
            v_wallet.name,
            v_wallet.currency,
            v_wallet.monthly_budget_minor,
            v_existing_role;
        RETURN;
    END IF;

    v_resolved_name := NULLIF(trim(COALESCE(p_display_name, '')), '');
    IF v_resolved_name IS NULL THEN
        SELECT COALESCE(
            NULLIF(trim(u.raw_user_meta_data ->> 'full_name'), ''),
            NULLIF(trim(u.raw_user_meta_data ->> 'name'), ''),
            NULLIF(split_part(COALESCE(u.email, ''), '@', 1), ''),
            'Member'
        )
        INTO v_resolved_name
        FROM auth.users u
        WHERE u.id = v_uid;
    END IF;

    INSERT INTO public.shared_wallet_members (wallet_id, user_id, display_name, role)
    VALUES (v_invite.wallet_id, v_uid, COALESCE(v_resolved_name, 'Member'), v_invite.role);

    UPDATE public.shared_wallet_invites
    SET used_count = used_count + 1
    WHERE code = v_invite.code;

    RETURN QUERY SELECT
        v_wallet.id,
        v_wallet.name,
        v_wallet.currency,
        v_wallet.monthly_budget_minor,
        v_invite.role;
END;
$$;

REVOKE ALL ON FUNCTION public.join_shared_wallet(text, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.join_shared_wallet(text, text) TO authenticated;

-- Enable Realtime replication on shared_transactions and shared_wallet_members.
DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_transactions;
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_wallet_members;
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END;
END
$$;
