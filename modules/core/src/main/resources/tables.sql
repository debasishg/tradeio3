CREATE TABLE IF NOT EXISTS accounts (
    no varchar NOT NULL PRIMARY KEY,
    name varchar NOT NULL,
    type varchar NOT NULL,
    dateOfOpen timestamp with time zone NOT NULL,
    dateOfClose timestamp with time zone,
    baseCurrency varchar NOT NULL,
    tradingCurrency varchar,
    settlementCurrency varchar
);

CREATE TABLE IF NOT EXISTS instruments (
    isinCode varchar NOT NULL PRIMARY KEY,
    name varchar NOT NULL,
    type varchar NOT NULL,
    dateOfIssue timestamp,
    dateOfMaturity timestamp,
    lotSize integer,
    unitPrice decimal,
    couponRate decimal,
    couponFrequency decimal
);

CREATE TABLE IF NOT EXISTS orders (
    no varchar NOT NULL PRIMARY KEY,
    dateOfOrder timestamp NOT NULL,
    accountNo varchar references accounts(no)
);

CREATE TABLE IF NOT EXISTS lineItems (
    lineItemId serial PRIMARY KEY,
    orderNo varchar references orders(no),
    isinCode varchar references instruments(isinCode),
    quantity decimal NOT NULL,
    unitPrice decimal NOT NULL,
    buySellFlag varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS executions (
    executionRefNo uuid NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    accountNo varchar NOT NULL references accounts(no),
    orderNo varchar NOT NULL references orders(no),
    isinCode varchar NOT NULL references instruments(isinCode),
    market varchar NOT NULL,
    buySellFlag varchar NOT NULL,
    unitPrice decimal NOT NULL,
    quantity decimal NOT NULL,
    dateOfExecution timestamp NOT NULL,
    exchangeExecutionRefNo varchar
);

CREATE TABLE IF NOT EXISTS taxFees (
    taxFeeId varchar NOT NULL PRIMARY KEY,
    description varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS trades (
    tradeRefNo uuid NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    accountNo varchar NOT NULL references accounts(no),
    isinCode varchar NOT NULL references instruments(isinCode),
    market varchar NOT NULL,
    buySellFlag varchar NOT NULL,
    unitPrice decimal NOT NULL,
    quantity decimal NOT NULL,
    tradeDate timestamp NOT NULL,
    valueDate timestamp,
    netAmount decimal,
    userId uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS tradeTaxFees (
    tradeTaxFeeId serial PRIMARY KEY,
    tradeRefNo uuid NOT NULL references trades(tradeRefNo),
    taxFeeId varchar NOT NULL references taxFees(taxFeeId),
    amount decimal NOT NULL
);

CREATE TABLE IF NOT EXISTS balance (
    balanceId serial PRIMARY KEY,
    accountNo varchar NOT NULL UNIQUE references accounts(no),
    amount decimal NOT NULL,
    asOf timestamp NOT NULL,
    currency varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar UNIQUE NOT NULL,
    password varchar NOT NULL
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

insert into accounts
values(
  'ibm-123',
  'IBM',
  'Trading',
  current_date,
  null,
  'USD',
  'USD',
  null
);

insert into accounts
values(
  'ibm-124',
  'IBM',
  'Trading',
  current_date,
  null,
  'USD',
  'USD',
  null
);

insert into accounts
values(
  'nri-654',
  'Nomura',
  'Trading',
  current_date,
  null,
  'USD',
  'USD',
  null
);

insert into instruments
values (
  'US0378331005',
  'apple',
  'equity',
  '2019-08-25 19:10:25',
  null,
  100,
  1200.50,
  null,
  null
);

insert into instruments
values (
  'GB0002634946',
  'bae systems',
  'equity',
  '2018-08-25 19:10:25',
  null,
  100,
  200.50,
  null,
  null
);

insert into instruments
values (
  'US4592001014',
  'ibm',
  'equity',
  '2018-08-25 19:10:25',
  null,
  100,
  500.50,
  null,
  null
);