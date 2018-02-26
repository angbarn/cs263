CREATE TABLE customer (
    customer_id Int NOT NULL AUTO_INCREMENT,
    first_name Varchar(30) NOT NULL,
    last_name Varchar(30) NOT NULL,
    address_id Int NOT NULL,

    PRIMARY KEY (customer_id),

    FOREIGN KEY (address_id) REFERENCES address
);

CREATE TABLE address (
    address_id Int NOT NULL AUTO_INCREMENT,
    address_1 Varchar(30) NOT NULL,
    address_2 Varchar(30) NOT NULL,
    postcode Varchar(10) NOT NULL,
    county Varchar(26) NOT NULL, -- Cambridgeshire is the longest county name

    PRIMARY KEY (address_id)
);

CREATE TABLE payment_details (
    
);
