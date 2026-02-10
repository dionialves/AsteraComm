CREATE TABLE asterisk.cdr (
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


CREATE TABLE asterisk.cel (
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


CREATE TABLE asterisk.extensions (
  id SERIAL PRIMARY KEY,
  context VARCHAR(20) NOT NULL,
  exten VARCHAR(20) NOT NULL,
  priority INTEGER NOT NULL,
  app VARCHAR(20),
  appdata VARCHAR(128)
);


CREATE TABLE asterisk.ps_contacts (
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


CREATE TABLE asterisk.ps_endpoint_id_ips (
  id VARCHAR(40) PRIMARY KEY,
  endpoint VARCHAR(40),
  match TEXT
);


CREATE TABLE asterisk.ps_registrations (
    id VARCHAR(40) PRIMARY KEY,
    auth_rejection_permanent BOOLEAN DEFAULT FALSE,
    client_uri VARCHAR(255) NOT NULL,
    contact_user VARCHAR(40),
    expiration VARCHAR(10),
    max_retries VARCHAR(10),
    outbound_auth VARCHAR(40),
    outbound_proxy VARCHAR(40),
    retry_interval VARCHAR(10),
    forbidden_retry_interval VARCHAR(10),
    server_uri VARCHAR(255) NOT NULL,
    transport VARCHAR(40),
    support_path BOOLEAN DEFAULT FALSE,
    line_identifier VARCHAR(40)
);


CREATE TABLE asterisk.ps_domain_aliases (
  id VARCHAR(40) PRIMARY KEY,
  domain VARCHAR(80)
);


CREATE TABLE asterisk.ps_systems (
  id VARCHAR(40) PRIMARY KEY,
  timer_t1 VARCHAR(10),
  timer_b VARCHAR(10),
  compact_headers VARCHAR(40),
  threadpool_threads VARCHAR(10),
  disable_tcp_switch VARCHAR(40)
);
