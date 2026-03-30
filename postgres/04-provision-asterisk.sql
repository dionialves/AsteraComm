-- =============================================================================
-- 04-provision-asterisk.sql
-- Provisionamento Asterisk PJSIP para circuitos e tronco importados via seed
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------
-- CIRCUITOS: ps_aors, ps_auths, ps_endpoints
-- -----------------------------------------------------------------------

INSERT INTO ps_aors (id, default_expiration, max_contacts, remove_existing, qualify_frequency)
SELECT number, '60', '1', 'yes', '30'
FROM asteracomm_circuits
ON CONFLICT (id) DO NOTHING;

INSERT INTO ps_auths (id, auth_type, username, password)
SELECT number, 'userpass', number, password
FROM asteracomm_circuits
ON CONFLICT (id) DO NOTHING;

INSERT INTO ps_endpoints (id, aors, auth, context, disallow, allow,
                          direct_media, force_rport, rewrite_contact,
                          rtp_symmetric, callerid)
SELECT number, number, number,
       'from-internal',
       'all', 'ulaw,alaw', 'no', 'yes', 'yes', 'yes', number
FROM asteracomm_circuits
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------
-- TRONCO: trunk-tellcheap
-- -----------------------------------------------------------------------

INSERT INTO ps_aors (id, contact, qualify_frequency)
VALUES ('trunk-tellcheap', 'sip:sip.tellcheap.local', '30')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ps_auths (id, auth_type, username, password)
VALUES ('trunk-tellcheap', 'userpass', 'tellcheap', 'placeholder')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ps_endpoints (id, aors, context, disallow, allow, direct_media, outbound_auth)
VALUES ('trunk-tellcheap', 'trunk-tellcheap',
        'pstn-trunk-tellcheap', 'all', 'ulaw,alaw', 'no', 'trunk-tellcheap')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ps_registrations (id, server_uri, client_uri, outbound_auth, retry_interval)
VALUES ('trunk-tellcheap',
        'sip:sip.tellcheap.local',
        'sip:tellcheap@sip.tellcheap.local',
        'trunk-tellcheap', '60')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------
-- EXTENSIONS: saída via trunk-tellcheap (dialplan outbound)
-- -----------------------------------------------------------------------

INSERT INTO extensions (context, exten, priority, app, appdata)
SELECT 'internal-trunk-tellcheap', '_X.', 1, 'NoOp', 'Chamada para ${EXTEN}'
WHERE NOT EXISTS (
    SELECT 1 FROM extensions
    WHERE context = 'internal-trunk-tellcheap' AND exten = '_X.' AND priority = 1
);

INSERT INTO extensions (context, exten, priority, app, appdata)
SELECT 'internal-trunk-tellcheap', '_X.', 2, 'Dial', 'PJSIP/${EXTEN}@trunk-tellcheap,60'
WHERE NOT EXISTS (
    SELECT 1 FROM extensions
    WHERE context = 'internal-trunk-tellcheap' AND exten = '_X.' AND priority = 2
);

INSERT INTO extensions (context, exten, priority, app, appdata)
SELECT 'internal-trunk-tellcheap', '_X.', 3, 'Hangup', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM extensions
    WHERE context = 'internal-trunk-tellcheap' AND exten = '_X.' AND priority = 3
);

COMMIT;
