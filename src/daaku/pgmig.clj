(ns daaku.pgmig
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [next.jdbc :as jdbc]))

(defn- already-migrated [db]
  (jdbc/execute! db ["CREATE TABLE IF NOT EXISTS pgmig_migrate (
                        id SERIAL PRIMARY KEY,
                        migration TEXT UNIQUE,
                        created TIMESTAMP NOT NULL DEFAULT current_timestamp
                      );"])
  (jdbc/execute! db ["SELECT migration FROM pgmig_migrate ORDER BY id"]))

(defn- pending [done migrations]
  (dorun (map (fn [{:keys [pgmig_migrate/migration]} {:keys [name]}]
                (when-not (= name migration)
                  (throw (ex-info "unexpected migration"
                                  {:expected name :actual migration}))))
              done migrations))
  (drop (count done) migrations))

(defn migrate
  "Given a spec or data source, and a vector of maps containing :name and
   :content, it will run the SQL content of all pending migrations if any.
   Remember never to delete migrations, and only add new ones at the end."
  [ds migrations]
  (jdbc/with-transaction [tx ds]
    (run! (fn [{:keys [name content]}]
            (jdbc/execute! tx [content])
            (jdbc/execute! tx ["INSERT INTO pgmig_migrate (migration) VALUES (?)" name]))
          (pending (already-migrated tx) migrations))))

(defn- jar-seq [dir]
  (let [[jurl prefix] (string/split (str dir) #"!/" 2)
        jf (string/replace-first jurl #"^jar:file:" "")
        jar (java.util.jar.JarFile. jf)
        entries (.entries jar)]
    (loop [result []]
      (if (.hasMoreElements entries)
        (let [el ^java.util.jar.JarEntry (.nextElement entries)
              name (.getName el)]
          (recur (if (and (string/starts-with? name prefix)
                          (string/ends-with? name ".sql"))
                   (conj result {:name (subs name (count prefix))
                                 :content (slurp (io/resource name))})
                   result)))
        result))))

(defn- dir-seq [dir]
  (into []
        (comp
         (filter (fn [^java.io.File f] (string/ends-with? (.getName f) ".sql")))
         (map (fn [^java.io.File f] {:name (.getName f) :content (slurp f)})))
        (-> dir io/as-file file-seq)))

(defn- protocol [dir]
  (try
    (-> dir io/as-url .getProtocol)
    (catch java.net.MalformedURLException _e "")))

(defn migrations-from-dir
  "Given a directory, loads all all .sql files and prepares them as a vector
   of migrations as expected by the migrate function."
  [dir]
  (sort-by :name (if (= (protocol dir) "jar")
                   (jar-seq dir)
                   (dir-seq dir))))
