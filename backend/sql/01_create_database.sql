-- ============================================================
-- MicroVault - 01_create_database.sql
-- Creates the database and the UUID support needed by the schema.
-- ============================================================

-- The shared development database already exists, so the CREATE DATABASE
-- statement below is only needed when you set up a fresh local PostgreSQL.
-- Run it while connected to the "postgres" database.

-- CREATE DATABASE indr_aug13_smartsavingsandinvestment_dev;

-- Connect to the MicroVault database before running 02_create_tables.sql:
--   psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev \
--        -d indr_aug13_smartsavingsandinvestment_dev

-- PostgreSQL 13 and newer already provide gen_random_uuid().
-- On older versions enable pgcrypto (needs a privileged user):
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

SELECT current_database() AS connected_database,
       current_user       AS connected_user,
       version()          AS server_version;
