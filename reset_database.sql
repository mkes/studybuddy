-- Connect to postgres database first to drop studytracker database
\c postgres;

-- Drop the database if it exists
DROP DATABASE IF EXISTS studytracker;

-- Drop the user if it exists (if not the owner)
DROP USER IF EXISTS studytracker;

-- Create the user
CREATE USER studytracker WITH PASSWORD 'password';

-- Create the database with the current user as owner
CREATE DATABASE studytracker;

-- Grant all privileges to studytracker user
GRANT ALL PRIVILEGES ON DATABASE studytracker TO studytracker;

-- Connect to the new database
\c studytracker;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO studytracker;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO studytracker;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO studytracker;