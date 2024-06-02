(ns games.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[games started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[games has shut down successfully]=-"))
   :middleware identity})
