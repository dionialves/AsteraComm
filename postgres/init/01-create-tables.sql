
-- =============================================================================
-- Tabelas do Asterisk (CDR / CEL / PJSIP via ODBC)
-- =============================================================================

CREATE TABLE cdr (
  id BIGSERIAL PRIMARY KEY,
  calldate TIMESTAMP NOT NULL,
  clid VARCHAR(80) NOT NULL,
  src VARCHAR(80) NOT NULL,
  dst VARCHAR(80) NOT NULL,
  dcontext VARCHAR(80) NOT NULL,
  channel VARCHAR(80) NOT NULL,
  dstchannel VARCHAR(80) NOT NULL,
  lastapp VARCHAR(80) NOT NULL,
  lastdata VARCHAR(80) NOT NULL,
  duration INTEGER NOT NULL,
  billsec INTEGER NOT NULL,
  disposition VARCHAR(45) NOT NULL,
  amaflags INTEGER NOT NULL,
  accountcode VARCHAR(20),
  uniqueid VARCHAR(32) NOT NULL,
  userfield VARCHAR(255)
);


CREATE TABLE cel (
  id SERIAL PRIMARY KEY,
  eventtime TIMESTAMP NOT NULL,
  eventtype VARCHAR(30) NOT NULL,
  cid_name VARCHAR(80),
  cid_num VARCHAR(80),
  cid_ani VARCHAR(80),
  cid_rdnis VARCHAR(80),
  cid_dnid VARCHAR(80),
  exten VARCHAR(80),
  context VARCHAR(80),
  channame VARCHAR(80),
  appname VARCHAR(80),
  appdata VARCHAR(80),
  amaflags INTEGER,
  accountcode VARCHAR(20),
  uniqueid VARCHAR(32),
  linkedid VARCHAR(32),
  peer VARCHAR(80),
  userdeftype VARCHAR(255),
  extra TEXT
);


CREATE TABLE ps_contacts (
  id VARCHAR(255),
  uri VARCHAR(511),
  expiration_time VARCHAR(20),
  qualify_frequency VARCHAR(10),
  outbound_proxy VARCHAR(255),
  path TEXT,
  user_agent VARCHAR(255),
  qualify_timeout VARCHAR(10),
  reg_server VARCHAR(255),
  authenticate_qualify VARCHAR(40),
  via_addr VARCHAR(40),
  via_port VARCHAR(10),
  call_id VARCHAR(255),
  endpoint VARCHAR(255),
  prune_on_boot VARCHAR(40),
  qualify_2xx_only VARCHAR(40)
);


CREATE TABLE ps_endpoint_id_ips (
  id VARCHAR(40) PRIMARY KEY,
  endpoint VARCHAR(40),
  match TEXT
);


CREATE TABLE ps_domain_aliases (
  id VARCHAR(40) PRIMARY KEY,
  domain VARCHAR(80)
);


CREATE TABLE ps_systems (
  id VARCHAR(40) PRIMARY KEY,
  timer_t1 VARCHAR(10),
  timer_b VARCHAR(10),
  compact_headers VARCHAR(40),
  threadpool_threads VARCHAR(10),
  disable_tcp_switch VARCHAR(40)
);


CREATE TABLE ps_aors (
  id VARCHAR(40) PRIMARY KEY,
  default_expiration VARCHAR(10),
  contact VARCHAR(255),
  max_contacts VARCHAR(10),
  remove_existing VARCHAR(10),
  qualify_frequency VARCHAR(10)
);


CREATE TABLE ps_auths (
  id VARCHAR(40) PRIMARY KEY,
  auth_type VARCHAR(20) NOT NULL,
  username VARCHAR(40),
  password VARCHAR(80)
);


CREATE TABLE ps_endpoints (
  id VARCHAR(40) PRIMARY KEY,
  transport VARCHAR(40),
  aors VARCHAR(40) REFERENCES ps_aors(id),
  auth VARCHAR(40) REFERENCES ps_auths(id),
  context VARCHAR(40),
  disallow VARCHAR(200),
  allow VARCHAR(200),
  direct_media VARCHAR(10),
  force_rport VARCHAR(10),
  rtp_symmetric VARCHAR(10),
  rewrite_contact VARCHAR(10),
  callerid VARCHAR(40),
  mailboxes VARCHAR(40),
  outbound_auth VARCHAR(40)
);


CREATE TABLE ps_registrations (
  id VARCHAR(40) PRIMARY KEY,
  server_uri VARCHAR(255) NOT NULL,
  client_uri VARCHAR(255) NOT NULL,
  outbound_auth VARCHAR(40),
  retry_interval VARCHAR(10)
);


CREATE TABLE extensions (
  id BIGSERIAL PRIMARY KEY,
  context VARCHAR(40) NOT NULL,
  exten VARCHAR(40) NOT NULL,
  priority INTEGER NOT NULL,
  app VARCHAR(20) NOT NULL,
  appdata VARCHAR(128)
);


-- =============================================================================
-- Tabelas da aplicação AsteraComm
-- =============================================================================

CREATE TABLE asteracomm_users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL
);


CREATE TABLE asteracomm_trunks (
  name VARCHAR(40) PRIMARY KEY,
  host VARCHAR(255) NOT NULL,
  username VARCHAR(80) NOT NULL,
  password VARCHAR(80) NOT NULL,
  prefix VARCHAR(20)
);


CREATE TABLE asteracomm_trunk_registration_status (
  id BIGSERIAL PRIMARY KEY,
  trunk_name VARCHAR(40) NOT NULL,
  registered BOOLEAN NOT NULL,
  checked_at TIMESTAMP NOT NULL
);


CREATE TABLE asteracomm_circuits (
  number VARCHAR(20) PRIMARY KEY,
  password VARCHAR(80) NOT NULL,
  trunk_name VARCHAR(40) NOT NULL
);


CREATE TABLE asteracomm_endpoint_status (
  id BIGSERIAL PRIMARY KEY,
  endpoint VARCHAR(40) NOT NULL REFERENCES ps_endpoints(id),
  online BOOLEAN NOT NULL,
  ip VARCHAR(45),
  rtt VARCHAR(20),
  checked_at TIMESTAMP NOT NULL
);


CREATE TABLE asteracomm_dids (
  id BIGSERIAL PRIMARY KEY,
  number VARCHAR(10) NOT NULL UNIQUE,
  circuit_number VARCHAR(20) REFERENCES asteracomm_circuits(number)
);
