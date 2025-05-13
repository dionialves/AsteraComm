CREATE TABLE cdr (
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


CREATE TABLE extensions (
  id SERIAL PRIMARY KEY,
  context VARCHAR(20) NOT NULL,
  exten VARCHAR(20) NOT NULL,
  priority INTEGER NOT NULL,
  app VARCHAR(20),
  appdata VARCHAR(128)
);


CREATE TABLE ps_endpoints (
  id VARCHAR(255) PRIMARY KEY,
  transport VARCHAR(40),
  aors VARCHAR(2048),
  auth VARCHAR(255),
  context VARCHAR(40),
  disallow VARCHAR(200),
  allow VARCHAR(200),
  direct_media VARCHAR(200),
  force_rport VARCHAR(200),
  ice_support VARCHAR(200),
  identify_by VARCHAR(80),
  rewrite_contact VARCHAR(80),
  rtp_symmetric VARCHAR(80),
  callerid VARCHAR(40)
);


CREATE TABLE ps_aors (
  id VARCHAR(40) PRIMARY KEY,
  contact TEXT,
  default_expiration INTEGER,
  max_contacts INTEGER,
  minimum_expiration INTEGER,
  remove_existing VARCHAR(40),
  qualify_frequency INTEGER,
  authenticate_qualify VARCHAR(40),
  maximum_expiration INTEGER,
  outbound_proxy TEXT,
  support_path VARCHAR(40),
  remove_unavailable VARCHAR(40),
  qualify_2xx_only VARCHAR(40)
);


CREATE TABLE ps_auths (
  id VARCHAR(40) PRIMARY KEY,
  auth_type VARCHAR(40),
  nonce_lifetime INTEGER,
  md5_cred VARCHAR(80),
  password VARCHAR(80),
  realm VARCHAR(80),
  username VARCHAR(80)
);


CREATE TABLE ps_contacts (
  id VARCHAR(255),
  uri VARCHAR(511),
  expiration_time BIGINT,
  qualify_frequency INTEGER,
  outbound_proxy VARCHAR(255),
  path TEXT,
  user_agent VARCHAR(255),
  qualify_timeout REAL,
  reg_server VARCHAR(255),
  authenticate_qualify VARCHAR(40),
  via_addr VARCHAR(40),
  via_port INTEGER,
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


CREATE TABLE ps_registrations (
    id SERIAL PRIMARY KEY,
    auth_rejection_permanent BOOLEAN DEFAULT FALSE,
    client_uri VARCHAR(255) NOT NULL,
    contact_user VARCHAR(40),
    expiration INTEGER,
    max_retries INTEGER DEFAULT 0,
    outbound_auth VARCHAR(40),
    outbound_proxy VARCHAR(40),
    retry_interval INTEGER DEFAULT 30,
    forbidden_retry_interval INTEGER DEFAULT 60,
    server_uri VARCHAR(255) NOT NULL,
    transport VARCHAR(40),
    support_path BOOLEAN DEFAULT FALSE,
    line_identifier VARCHAR(40)
);


CREATE TABLE ps_domain_aliases (
  id VARCHAR(40) PRIMARY KEY,
  domain VARCHAR(80)
);


CREATE TABLE ps_systems (
  id VARCHAR(40) PRIMARY KEY,
  timer_t1 INTEGER,
  timer_b INTEGER,
  compact_headers VARCHAR(40),
  threadpool_threads INTEGER,
  disable_tcp_switch VARCHAR(40)
);


CREATE TABLE asteracomm_endpoint_status_history (
    id INTEGER PRIMARY KEY,
    online BOOLEAN NOT NULL,
    ip VARCHAR(255),
    rtt VARCHAR(255),
    checked_at TIMESTAMP
);
