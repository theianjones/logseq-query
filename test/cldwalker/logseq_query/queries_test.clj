(ns cldwalker.logseq-query.queries-test
  "These are high level tests for queries in queries.edn. These tests use the
  test-notes graph."
  (:require [clojure.test :refer [is deftest testing]]
            [datascript.core :as d]
            [clojure.string :as str]
            [cldwalker.logseq-query.datascript :as ld]))

(defn- filter-db
  "Filters datascript db to only contain blocks with given pages"
  [db pages]
  (let [allowed-blocks
        (->> (d/q '[:find ?b
                    :in $ ?pages
                    :where
                    [?b :block/page ?bp]
                    [?bp :block/name ?page]
                    [(contains? ?pages ?page)]]
                  db pages)
             (map first)
             set)]
    (d/filter db (fn [_db datom]
                   (contains? allowed-blocks (:e datom))))))

(defn- q [& {:keys [args pages]}]
  (with-redefs [ld/get-graph-db (fn [graph]
                                  (filter-db (@#'ld/get-graph-db* graph) pages))]
    (ld/q {:arguments args
           :options {:graph "test/cldwalker/logseq_query/test-notes.json"
                     :raw true}})))

(deftest property
  (is (= #{{:type "comment" :desc "hi"} {:type "comment" :desc "hola"}}
         (->> (q :args ["property" "type" "comment"]
                 :pages #{"test/property"})
              (map :block/properties)
              set))))

(deftest properties
  (is (= 2
         (count (q :args ["properties"]
                   :pages #{"test/property-counts"})))))

(deftest property-counts
  (is (= [[:type 2] [:desc 1]]
         (q :args ["property-counts"]
            :pages #{"test/property-counts"}))))

(deftest has-property
  (is (= #{"apple" "banana"}
         (set
          (map (comp first str/split-lines :block/content)
               (q :args ["has-property" "type"]
                  :pages #{"test/has-property"}))))))

(deftest todo-for-marker
  (testing "default args"
    (is (= #{"TODO b1" "DOING b2"}
           (set
            (map (comp first str/split-lines :block/content)
                 (q :args ["todo-for-marker"]
                    :pages #{"test/todo-for-marker"}))))))

  (testing "with args"
    (is (= #{"TODO b1" "DONE b3"}
           (set
            (map (comp first str/split-lines :block/content)
                 (q :args ["todo-for-marker" "todo" "done"]
                    :pages #{"test/todo-for-marker"})))))))

(deftest content-search
  (is (= #{"blarg\nblargity" "oopsie\nflower:: blarg"}
         (set
          (map :block/content
               (q :args ["content-search" "blarg"]
                  :pages #{"test/content-search"}))))))

(deftest property-search
  (is (= #{"https://logseq.com" "https://www.bbc.com/"}
         (set
          (map (comp :url :block/properties)
               (q :args ["property-search" "url" "https"]
                  :pages #{"test/property-search"}))))))
