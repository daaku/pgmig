(defproject daaku/pgmig "1.0.1"
  :description "Database migrations for PostgreSQL using next.jdbc."
  :url "https://github.com/daaku/pgmig"
  :scm {:name "git" :url "https://github.com/daaku/pgmig"}
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.postgresql/postgresql "42.2.15"]
                 [seancorfield/next.jdbc "1.1.582"]]
  :dev-dependencies [[org.slf4j/slf4j-nop "1.7.30"]])
