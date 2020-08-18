pgmig
=====

Database migrations for
[next.jdbc](https://github.com/seancorfield/next-jdbc) and
[PostgreSQL](https://www.postgresql.org/).


[![Documentation](https://cljdoc.org/badge/daaku/pgmig)](https://cljdoc.org/d/daaku/pgmig/CURRENT)
[![Clojars](https://img.shields.io/clojars/v/daaku/pgmig.svg)](https://clojars.org/daaku/pgmig)
[![Build](https://github.com/daaku/pgmig/workflows/build/badge.svg)](https://github.com/daaku/pgmig/actions?query=workflow%3Abuild)

Documentation: https://cljdoc.org/d/daaku/pgmig

## Usage

Make a directory that contains your migrations. You can put this in your
`resources` directory and load it from your `jar` if that makes
sense. The library will run thru all the files in sorted order.
The suggested naming convention is `000_first.sql`, `001_second.sql` and so
on.

The library:

 1. Will create a table named `pgmig_migrate` to manage the migration
    state.
 1. Will run everything in a single transaction, so all pending
    migrations are run, or nothing.
 1. Expects you to never delete or rename a migration.
 1. Expects you to not put a new migration between two existing ones.
 1. Expects file names and contents to be UTF-8.
 1. There are no rollbacks - just write a new migration.

To use it:

```clojure
(ns myapp
  (:require [clojure.java.io :as io]
            [daaku.pgmig :as pgmig]))

(def db-spec {:dbtype "postgres" :dbname "myapp"})

(def migrations
  (delay (pgmig/migrations-from-dir (io/resource "myapp/migrations"))))

(defn start []
  (pgmig/migrate db-spec @migrations))
```
