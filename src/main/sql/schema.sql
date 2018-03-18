CREATE TABLE customer (
    customer_id INTEGER,
    phone_number VARCHAR(12) NOT NULL,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    address_id INTEGER NOT NULL,
    security_id INTEGER NOT NULL,

    PRIMARY KEY (customer_id),
    FOREIGN KEY (address_id) REFERENCES address,
    FOREIGN KEY (security_id) REFERENCES security
);

CREATE TABLE session (
    session_id INTEGER,
    session_key VARCHAR(20) NOT NULL,
    expiry INTEGER NOT NULL, -- SQLite automatically selects Long
    otac_authenticated INTEGER DEFAULT 0 NOT NULL,
    customer_id INTEGER NOT NULL,

    PRIMARY KEY (session_id),
    FOREIGN KEY (customer_id) REFERENCES customer,
    CHECK (otac_authenticated >= 0 AND otac_authenticated <= 1)
);

CREATE TABLE address (
    address_id INTEGER,
    address_1 VARCHAR(30) NOT NULL,
    address_2 VARCHAR(30),
    postcode VARCHAR(10) NOT NULL,
    county VARCHAR(26) NOT NULL, -- "Cambridgeshire and Isle of Ely" is the longest
                                 -- county name

    PRIMARY KEY (address_id)
);

CREATE TABLE security (
    security_id INTEGER,
    login_salt BLOB(20) NOT NULL,
    support_in_salt BLOB(20) NOT NULL,
    support_out_salt BLOB(20) NOT NULL,
    password BLOB(20) NOT NULL,
    password_salt BLOB(20) NOT NULL,
    password_hash_passes INTEGER NOT NULL,

    PRIMARY KEY (security_id)
);

CREATE TABLE log (
    log_id INTEGER,
    time_created INTEGER NOT NULL,
    content BLOB(1000) NOT NULL,

    PRIMARY KEY (log_id)
);

--CREATE TABLE payment_details (
--
--);
