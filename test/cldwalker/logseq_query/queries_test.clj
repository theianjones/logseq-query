(ns cldwalker.logseq-query.queries-test
  "These are high level tests for queries in queries.edn. These tests use the
  test-notes graph."
  (:require [clojure.test :refer [is deftest]]
            [datascript.core :as d]
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
  (with-redefs [ld/print-results (fn [result _] result)
                ld/get-graph-db (fn [graph]
                                  (filter-db (@#'ld/get-graph-db* graph) pages))]
    (ld/q {:arguments args
           :options {:graph "test/cldwalker/logseq_query/test-notes.json"}})))

(deftest block-property
  (is (= #{{:type "comment" :desc "hi"} {:type "comment" :desc "hola"}}
         (->> (q :args ["block-property" "type" "comment"]
                 :pages #{"test/block-property"})
              (map :block/properties)
              set))))

(deftest block-property-counts
  (is (= [[:type 2] [:desc 1]]
         (q :args ["block-property-counts"]
            :pages #{"test/block-property-counts"}))))
