CREATE SCHEMA IF NOT EXISTS file;
CREATE SCHEMA IF NOT EXISTS file_history;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET search_path TO file,file_history,public;