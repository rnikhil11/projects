-- create tables
CREATE TABLE zip_city_state (
    zipcode char(5) not null,
    city varchar(20) not null,
    state char(2) not null,
    primary key (zipcode)
);

CREATE TABLE store (
    store_id varchar(15) not null,
    store_name varchar(15) not null unique,
    address1 varchar(10),
    address2 varchar(10),
    zipcode char(5) not null,
    primary key (store_id)
);

CREATE TABLE customer (
    phone char(10) not null,
    email varchar(20) unique,
    fname varchar(10) not null,
    lname varchar(10) not null,
    home_store_id varchar(10),
    primary key (phone)
);

CREATE TABLE customer_addresses (
    phone char(10) not null,
    address1 varchar(10) not null,
    address2 varchar(10) default '',
    zipcode char(5) not null,
    default_address number(1),
    primary key (phone, address1, address2, zipcode)
);

CREATE TABLE category (
    category_id varchar(5) not null,
    name varchar(10) not null,
    base_category number(1) default 1,
    super_category_id varchar(5),
    primary key (category_id)
);


CREATE TABLE item (
    item_id varchar(5) not null,
    name varchar(10) not null,
    details varchar(30),
    tax number(4,2) default 0,
    listed_price number(7,2) not null,
    category_id varchar(5),
    primary key (item_id)
);

CREATE TABLE item_deal (
    item_deal_id varchar(5) not null,
    percent_deal_type number(1) default 1,
    deal_value number(4,2),
    item_count number default 1,
    deal_name varchar(10),
    item_id varchar(5) not null,
    primary key (item_deal_id)
);

CREATE TABLE orders(
    order_id varchar(10) not null,
    date_time date not null,
    total_amount number(7,2) default 0,
    customer_phone char(10),
    store_id varchar(15),
    primary key(order_id)
);

CREATE TABLE order_items(
    order_id varchar(10) not null,
    item_id varchar(5) not null,
    item_count number default 1,
    primary key(order_id, item_id)

);

CREATE TABLE store_items(
    store_id varchar(10) not null,
    item_id varchar(5) not null,
    item_count number default 1,
    primary key(store_id, item_id)
);


CREATE TABLE delivery_order(
    order_id varchar(10) not null,
    delivery_start_time date not null,
    delivery_end_time date not null,
    delivery_address1 varchar(10) not null,
    delivery_address2 varchar(10),
    delivery_zip char(5) not null,
    delivery_time date,
    primary key(order_id)
);

CREATE TABLE pickup_order(
    order_id varchar(10) not null,
    pickup_start_time date not null,
    pickup_end_time date not null,        
    pickup_time date,
    primary key(order_id)
);

CREATE TABLE instore_order(
    order_id varchar(10) not null,
    primary key(order_id)
);

CREATE TABLE card(
    card_no char(16) not null,
    expiry char(4) not null,
    cardholder_name varchar(20) not null,
    billing_address1 varchar(10),
    billing_address2 varchar(10),
    billing_zip char(5),
    primary key(card_no)
);

CREATE TABLE giftcard(
    card_no char(10) not null,
    access_no char(10) not null,
    balance number(7,2) not null,
    primary key(card_no)
);

CREATE TABLE payment_type(
    order_id varchar(10) not null,
    payment_type_cash number(1) default 0,
    payment_type_card number(1) default 0,
    payment_type_giftcard number(1) default 0,
    card_no char(16),
    giftcard_no char(10),
    primary key(order_id)

);

CREATE TABLE customer_cards(
    customer_phone char(10) not null,
    card_no char(16) not null,
    primary key(customer_phone, card_no)
);

CREATE TABLE customer_giftcards(
    customer_phone char(10) not null,
    giftcard_no char(10) not null,
    primary key(customer_phone, giftcard_no)
);

-- foreign keys
ALTER TABLE store
ADD CONSTRAINT fk_store_zip FOREIGN KEY(zipcode) REFERENCES zip_city_state(zipcode);

ALTER TABLE customer
ADD CONSTRAINT fk_cus_homestore FOREIGN KEY(home_store_id) REFERENCES store(store_id) ON DELETE SET NULL;

ALTER TABLE customer_addresses
ADD CONSTRAINT fk_cusaddrs_zip FOREIGN KEY(zipcode) REFERENCES zip_city_state(zipcode);

ALTER TABLE customer_addresses
ADD CONSTRAINT fk_cusaddrs_phone FOREIGN KEY(phone) REFERENCES customer(phone);

ALTER TABLE category
ADD CONSTRAINT fk_cat_supercatid FOREIGN KEY(super_category_id) REFERENCES category(category_id) ON DELETE SET NULL;

ALTER TABLE item
ADD CONSTRAINT fk_item_catid FOREIGN KEY(category_id) REFERENCES category(category_id) ON DELETE SET NULL;

ALTER TABLE item_deal
ADD CONSTRAINT fk_itemdeal_itemid FOREIGN KEY(item_id) REFERENCES item(item_id) ON DELETE CASCADE;

ALTER TABLE orders
ADD CONSTRAINT fk_orders_cusph FOREIGN KEY(customer_phone) REFERENCES customer(phone) ON DELETE SET NULL;
ALTER TABLE orders
ADD CONSTRAINT fk_orders_storeid FOREIGN KEY(store_id) REFERENCES store(store_id) ON DELETE SET NULL;

ALTER TABLE order_items
ADD CONSTRAINT fk_orderitems_orderid FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE;
ALTER TABLE order_items
ADD CONSTRAINT fk_orderitems_itemid FOREIGN KEY(item_id) REFERENCES item(item_id);

ALTER TABLE store_items
ADD CONSTRAINT fk_storeitems_storeid FOREIGN KEY(store_id) REFERENCES store(store_id) ON DELETE CASCADE;
ALTER TABLE store_items
ADD CONSTRAINT fk_storeitems_itemid FOREIGN KEY(item_id) REFERENCES item(item_id);

ALTER TABLE delivery_order
ADD CONSTRAINT fk_deliveryorder_orderid FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE;
ALTER TABLE delivery_order
ADD CONSTRAINT fk_deliveryorder_zip FOREIGN KEY(delivery_zip) REFERENCES zip_city_state(zipcode);

ALTER TABLE pickup_order
ADD CONSTRAINT fk_pickuporder_orderid FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE;

ALTER TABLE card
ADD CONSTRAINT fk_card_billingzip FOREIGN KEY(billing_zip) REFERENCES zip_city_state(zipcode);

ALTER TABLE payment_type
ADD CONSTRAINT fk_paymenttype_orderid FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE;
ALTER TABLE payment_type
ADD CONSTRAINT fk_paymenttype_cardno FOREIGN KEY(card_no) REFERENCES card(card_no);
ALTER TABLE payment_type
ADD CONSTRAINT fk_paymenttype_giftcardno FOREIGN KEY(giftcard_no) REFERENCES giftcard(card_no);

ALTER TABLE customer_cards
ADD CONSTRAINT fk_cuscards_phone FOREIGN KEY(customer_phone) REFERENCES customer(phone) ON DELETE CASCADE;
ALTER TABLE customer_cards
ADD CONSTRAINT fk_cuscards_cardno FOREIGN KEY(card_no) REFERENCES card(card_no);

ALTER TABLE customer_giftcards
ADD CONSTRAINT fk_giftcards_phone FOREIGN KEY(customer_phone) REFERENCES customer(phone) ON DELETE CASCADE;
ALTER TABLE customer_giftcards
ADD CONSTRAINT fk_giftcards_giftcardno FOREIGN KEY(giftcard_no) REFERENCES giftcard(card_no);


-- drop tables

-- drop table zip_city_state;
-- drop table store;
-- drop table customer;
-- drop table customer_addresses;
-- drop table category;
-- drop table item;
-- drop table item_deal;
-- drop table orders;
-- drop table order_items;
-- drop table store_items;
-- drop table delivery_order;
-- drop table pickup_order;
-- drop table instore_order;
-- drop table card;
-- drop table giftcard;
-- drop table payment_type;
-- drop table customer_cards;
-- drop table customer_giftcards;