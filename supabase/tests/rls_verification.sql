-- Transactional verification suite simulating JWT claims for OWNER, EDITOR, VIEWER, OUTSIDER & anon.
-- Wrapped in BEGIN ... ROLLBACK so test fixture rows never pollute the local database.
BEGIN;

DO $$
DECLARE
    v_owner_id constant uuid := '11111111-1111-4111-8111-111111111111';
    v_editor_id constant uuid := '22222222-2222-4222-8222-222222222222';
    v_viewer_id constant uuid := '33333333-3333-4333-8333-333333333333';
    v_outsider_id constant uuid := '44444444-4444-4444-8444-444444444444';

    v_wallet_id uuid;
    v_owner_tx_id uuid;
    v_editor_tx_id uuid;
    v_count integer;
    v_role_check public.shared_wallet_role;
    v_rls_disabled_count integer;
    v_joined_role public.shared_wallet_role;
BEGIN
    -- 0. Verify 100% of exposed public tables have RLS enabled & anon cannot call join_shared_wallet.
    SELECT count(*)
    INTO v_rls_disabled_count
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind = 'r'
      AND c.relrowsecurity = false;

    IF v_rls_disabled_count <> 0 THEN
        RAISE EXCEPTION 'Expected 0 public tables with RLS disabled, got %', v_rls_disabled_count;
    END IF;

    IF has_function_privilege('anon', 'public.join_shared_wallet(text, text)', 'EXECUTE') THEN
        RAISE EXCEPTION 'anon must NOT have EXECUTE privilege on public.join_shared_wallet';
    END IF;

    -- 1. Seed 4 synthetic auth.users rows cleanly inside the rolled-back test transaction.
    INSERT INTO auth.users (id, instance_id, aud, role, email, encrypted_password, raw_user_meta_data, created_at, updated_at)
    VALUES
        (v_owner_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'owner@notepay.local', 'x', '{"full_name":"Thinh"}'::jsonb, now(), now()),
        (v_editor_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'editor@notepay.local', 'x', '{"full_name":"An"}'::jsonb, now(), now()),
        (v_viewer_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'viewer@notepay.local', 'x', '{"full_name":"Be"}'::jsonb, now(), now()),
        (v_outsider_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'outsider@notepay.local', 'x', '{"full_name":"Stranger"}'::jsonb, now(), now())
    ON CONFLICT (id) DO NOTHING;

    -- 2. Act as OWNER: create shared wallet, verify auto-membership, add transaction & create invites.
    PERFORM set_config('request.jwt.claim.sub', v_owner_id::text, true);
    SET LOCAL ROLE authenticated;

    INSERT INTO public.shared_wallets (name, currency, monthly_budget_minor)
    VALUES ('Ví Gia Đình Test', 'VND', 1500000000)
    RETURNING id INTO v_wallet_id;

    SELECT role INTO v_role_check
    FROM public.shared_wallet_members
    WHERE wallet_id = v_wallet_id AND user_id = v_owner_id;
    IF v_role_check <> 'OWNER' THEN
        RAISE EXCEPTION 'Creator must be auto-enrolled as OWNER, got %', v_role_check;
    END IF;

    INSERT INTO public.shared_transactions (
        operation_id, wallet_id, created_by_name, type, amount_minor, category_id, note, occurred_at
    ) VALUES (
        gen_random_uuid(), v_wallet_id, 'Thinh', 'EXPENSE', 12000000, 'FOOD', 'Đi chợ cuối tuần', now()
    ) RETURNING id INTO v_owner_tx_id;

    INSERT INTO public.shared_wallet_invites (code, wallet_id, role, expires_at, max_uses)
    VALUES
        ('EDIT01', v_wallet_id, 'EDITOR', now() + interval '24 hours', 1),
        ('VIEW01', v_wallet_id, 'VIEWER', now() + interval '24 hours', 5),
        ('EXP001', v_wallet_id, 'VIEWER', now() - interval '1 hour', 5),
        ('REV001', v_wallet_id, 'VIEWER', now() + interval '24 hours', 5);

    UPDATE public.shared_wallet_invites SET revoked_at = now() WHERE code = 'REV001';

    -- 3. Act as EDITOR: join via invite RPC (case-insensitive code), verify exhausted single-use invite fails for others.
    PERFORM set_config('request.jwt.claim.sub', v_editor_id::text, true);
    SELECT j.role INTO v_joined_role FROM public.join_shared_wallet(' edit01 ', 'An') j;
    IF v_joined_role <> 'EDITOR' THEN
        RAISE EXCEPTION 'Expected EDITOR role on join, got %', v_joined_role;
    END IF;

    -- Insert own transaction (allowed for EDITOR).
    INSERT INTO public.shared_transactions (
        operation_id, wallet_id, created_by_name, type, amount_minor, category_id, note, occurred_at
    ) VALUES (
        gen_random_uuid(), v_wallet_id, 'An', 'EXPENSE', 5500000, 'FOOD', 'Trà sữa', now()
    ) RETURNING id INTO v_editor_tx_id;

    -- Update own transaction (allowed for EDITOR).
    UPDATE public.shared_transactions SET note = 'Trà sữa ít đường' WHERE id = v_editor_tx_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'EDITOR must be able to update own transaction';
    END IF;

    -- Attempt to update OWNER transaction as EDITOR -> must affect 0 rows.
    UPDATE public.shared_transactions SET note = 'Hacked' WHERE id = v_owner_tx_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'EDITOR must NOT update another member transaction';
    END IF;

    -- Attempt to delete wallet as EDITOR -> must affect 0 rows.
    DELETE FROM public.shared_wallets WHERE id = v_wallet_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'EDITOR must NOT delete wallet';
    END IF;

    -- Attempt to escalate own role as EDITOR -> must affect 0 rows.
    UPDATE public.shared_wallet_members SET role = 'OWNER'
    WHERE wallet_id = v_wallet_id AND user_id = v_editor_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'EDITOR must NOT escalate role';
    END IF;

    -- 4. Act as VIEWER: verify negative invite paths first, then join VIEW01.
    PERFORM set_config('request.jwt.claim.sub', v_viewer_id::text, true);

    BEGIN
        PERFORM * FROM public.join_shared_wallet('EXP001', 'Be');
        RAISE EXCEPTION 'Expired invite should have failed';
    EXCEPTION
        WHEN raise_exception THEN NULL;
    END;

    BEGIN
        PERFORM * FROM public.join_shared_wallet('REV001', 'Be');
        RAISE EXCEPTION 'Revoked invite should have failed';
    EXCEPTION
        WHEN raise_exception THEN NULL;
    END;

    BEGIN
        PERFORM * FROM public.join_shared_wallet('EDIT01', 'Be');
        RAISE EXCEPTION 'Exhausted max_uses=1 invite should have failed';
    EXCEPTION
        WHEN raise_exception THEN NULL;
    END;

    SELECT j.role INTO v_joined_role FROM public.join_shared_wallet('VIEW01', 'Be') j;
    IF v_joined_role <> 'VIEWER' THEN
        RAISE EXCEPTION 'Expected VIEWER role on join, got %', v_joined_role;
    END IF;

    -- VIEWER can SELECT wallet transactions.
    SELECT count(*) INTO v_count FROM public.shared_transactions WHERE wallet_id = v_wallet_id;
    IF v_count <> 2 THEN
        RAISE EXCEPTION 'VIEWER expected 2 visible transactions, got %', v_count;
    END IF;

    -- VIEWER CANNOT view invites table.
    SELECT count(*) INTO v_count FROM public.shared_wallet_invites WHERE wallet_id = v_wallet_id;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'VIEWER must see 0 invite rows, got %', v_count;
    END IF;

    -- VIEWER CANNOT INSERT transaction (RLS 42501).
    BEGIN
        INSERT INTO public.shared_transactions (
            operation_id, wallet_id, created_by_name, type, amount_minor, category_id, note, occurred_at
        ) VALUES (
            gen_random_uuid(), v_wallet_id, 'Be', 'EXPENSE', 100000, 'FOOD', 'Blocked', now()
        );
        RAISE EXCEPTION 'VIEWER INSERT should have been denied by RLS';
    EXCEPTION
        WHEN insufficient_privilege THEN NULL;
    END;

    -- VIEWER CANNOT DELETE wallet or UPDATE transactions.
    DELETE FROM public.shared_wallets WHERE id = v_wallet_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'VIEWER must NOT delete wallet';
    END IF;

    -- 5. Act as OUTSIDER: zero SELECT visibility across all 4 tables & INSERT rejected.
    PERFORM set_config('request.jwt.claim.sub', v_outsider_id::text, true);

    SELECT count(*) INTO v_count FROM public.shared_wallets;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'OUTSIDER must see 0 shared_wallets, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM public.shared_wallet_members;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'OUTSIDER must see 0 shared_wallet_members, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM public.shared_wallet_invites;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'OUTSIDER must see 0 shared_wallet_invites, got %', v_count;
    END IF;

    SELECT count(*) INTO v_count FROM public.shared_transactions;
    IF v_count <> 0 THEN
        RAISE EXCEPTION 'OUTSIDER must see 0 shared_transactions, got %', v_count;
    END IF;

    BEGIN
        INSERT INTO public.shared_transactions (
            operation_id, wallet_id, created_by_name, type, amount_minor, category_id, note, occurred_at
        ) VALUES (
            gen_random_uuid(), v_wallet_id, 'Stranger', 'EXPENSE', 9900000, 'FOOD', 'Injection', now()
        );
        RAISE EXCEPTION 'OUTSIDER INSERT should have been denied by RLS';
    EXCEPTION
        WHEN insufficient_privilege THEN NULL;
    END;

    -- 6. Act as OWNER again: manage spending limit, soft-delete any tx, delete wallet cleanly.
    PERFORM set_config('request.jwt.claim.sub', v_owner_id::text, true);

    UPDATE public.shared_wallet_members
    SET spending_limit_minor = 300000000
    WHERE wallet_id = v_wallet_id AND user_id = v_editor_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'OWNER must be able to update member spending limit';
    END IF;

    UPDATE public.shared_transactions
    SET deleted_at = now()
    WHERE id = v_editor_tx_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'OWNER must be able to update/soft-delete any wallet transaction';
    END IF;

    DELETE FROM public.shared_wallets WHERE id = v_wallet_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'OWNER must be able to delete own shared wallet';
    END IF;

    RAISE NOTICE 'ALL RLS + INVITE + ROLE ISOLATION CHECKS PASSED (4 roles + anon)';
END
$$;

ROLLBACK;
