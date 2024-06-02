(ns games.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [games.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[games started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[games has shut down successfully]=-"))
   :middleware wrap-dev})
