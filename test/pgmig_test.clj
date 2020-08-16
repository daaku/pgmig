(ns pgmig-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc]
            [pgmig :as pgmig]))

(def db-count (atom 0))

(defn- admin-query [sql]
  (with-open [db (jdbc/get-connection {:dbtype "postgres" :dbname "postgres"})]
    (jdbc/execute! db [sql])))

(defmacro with-test-ds [sym & body]
  `(let [name# (str "pgmig_migrate_test_" (swap! db-count inc))
         ~sym {:dbtype "postgres" :dbname name#}]
     (admin-query (str "CREATE DATABASE " name#))
     (try
       ~@body
       (finally (admin-query (str "DROP DATABASE " name#))))))

(def sample-migrations
  [{:name "one" :content "CREATE TABLE one (name text);
                          INSERT INTO one VALUES ('yoda');"}
   {:name "two" :content "CREATE TABLE two (name text);
                          INSERT INTO two VALUES ('yoda');"}])

(deftest test-empty-migrations
  (with-test-ds ds
    (pgmig/migrate ds [])
    (pgmig/migrate ds [])
    (is (nil? (jdbc/execute-one! ds ["SELECT migration FROM pgmig_migrate"])))))

(deftest test-success
  (with-test-ds ds
    (pgmig/migrate ds sample-migrations)
    (pgmig/migrate ds sample-migrations)
    (is (= {:one/name "yoda"}
           (jdbc/execute-one! ds ["SELECT name FROM one"])))
    (is (= {:two/name "yoda"}
           (jdbc/execute-one! ds ["SELECT name FROM two"])))))

(deftest test-failure
  (with-test-ds ds
    (pgmig/migrate ds (drop 1 sample-migrations))
    (is (thrown-with-msg? Exception #"unexpected migration"
                          (pgmig/migrate ds (take 1 sample-migrations))))))

(deftest test-db-missing
  (is (thrown-with-msg? Exception #"database \"pgmig_does_not_exist\" does not exist"
                        (pgmig/migrate {:dbtype "postgres"
                                          :dbname "pgmig_does_not_exist"} []))))

(defn- tmpdir ^java.nio.file.Path [^String prefix]
  (java.nio.file.Files/createTempDirectory
   prefix
   (into-array java.nio.file.attribute.FileAttribute [])))

(deftest test-migrations-from-dir
  (let [ms [{:name "0.sql" :content "foo"}
            {:name "1.sql" :content "bar"}]
        dirp (tmpdir "pgmig_migrate_")
        dir (.toString dirp)]
    (run! (fn [{:keys [^String name content]}]
            (spit (.. dirp (resolve name) (toString)) content))
          ms)
    (is (= ms (pgmig/migrations-from-dir dir)))
    (run! #(io/delete-file % true) (file-seq (.toFile dirp)))
    (io/delete-file (.toFile dirp))))
