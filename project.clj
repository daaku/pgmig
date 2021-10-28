(defproject daaku/pgmig "2.0.1"
  :description "Database migrations for PostgreSQL using next.jdbc."
  :url "https://github.com/daaku/pgmig"
  :scm {:name "git" :url "https://github.com/daaku/pgmig"}
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.postgresql/postgresql "42.3.0"]
                 [seancorfield/next.jdbc "1.2.659"]])
