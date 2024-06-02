(ns games.pages.minesweeper
  (:require [re-frame.core :as rf]))

(def rows 20)
(def cols 50)
(def number-of-bombs 100)
(def bomb-cell "x")
(def empty-cell " ")

(defn is-inside? [grid [x y]]
  (let [rows (count grid)
        cols (count (first grid))]
    (and (<= 0 y) (> cols y) ;; remove over board 
         (<= 0 x) (> rows x))))

(defn- neighbors [grid [x y]]
  (let [directions [[-1 -1] [0 -1] [1 -1]
                    [-1 0]         [1 0]
                    [-1 1]  [0 1]  [1 1]]
        repeated (repeat 8 [x y])
        neighbors (map #(map + %1 %2) repeated directions)]
    (filter #(is-inside? grid %) neighbors)))

(defn- cell-content [grid [x y]]
  (-> grid
      (nth x)
      (nth y)
      (:content)))

(defn- is-bomb? [[x y] grid]
  (let [cell (-> grid (nth x) (nth y))]
    (if (= bomb-cell cell) 1 0)))

(defn- count-adjecant-bombs [grid]
  (for [row (-> grid count range)
        col (-> grid first count range)]
    (let [neighbors (neighbors grid [row col])
          neighborbombs (map #(is-bomb? % grid) neighbors)
          bombcount (apply + neighborbombs)
          content (-> grid (nth row) (nth col))]
      (cond
        (= "x" content) "x"
        (< 0 bombcount) (str bombcount)
        :else " "))))

(defn- start-grid [rows cols number-of-bombs]
  (->> (repeat number-of-bombs bomb-cell)
      (concat (repeat (- (* rows cols) number-of-bombs) empty-cell))
      (shuffle)
      (partition cols)
      (count-adjecant-bombs)
      (map #(assoc {:is-open? false} :content %))
      (partition cols)))

(rf/reg-event-db
 :open-many-cells
 (fn [db [_ grid]]
   (assoc db :grid grid)))

(rf/reg-event-db
 :gridalize
 (fn [db _]
   (let [grid (start-grid rows cols number-of-bombs)]
     (assoc db :grid grid))))

(rf/dispatch [:gridalize])

(rf/reg-sub
 :grid
 (fn [db _]
   (:grid db)))

(rf/reg-event-db
 :open-cell
 (fn [db [_ grid]] 
   (assoc db :grid grid)))

(defn- find-all-to-open! [grid cells open]
  (let [neighbors (apply concat (map #(neighbors grid %) cells))
        to-check (filter #(= " " (cell-content grid %)) neighbors)
        next-cells (into (set to-check) cells)]
    (if (= next-cells cells)
      (into (set open) neighbors)
      (recur grid next-cells (into (set open) neighbors)))))

(defn- replace-cell [new-cell grid row col]
  (let [rows (-> grid count)
        cols (-> grid first count)
        position (+ col (* row cols))
        lined (apply concat grid)
        updated (map-indexed #(if (= position %1) new-cell %2) lined)]
    (partition cols updated)))

(defn- open-all! [grid cells]
  (if (empty? cells)
    grid
    (let [[x y] (first cells)
          cell (-> grid (nth x) (nth y))
          new-cell (assoc cell :is-open? true)
          new-grid (replace-cell new-cell grid x y)]
      (recur new-grid (rest cells)))))

(defn- show-all-bombs! [grid]
  (let [lined (apply concat grid)
        bombs-opened (map #(if (not= "x" (:content %)) %
                               (assoc % :is-open? true)) lined)]
    (-> grid
        first
        count
        (partition bombs-opened))))

(defn- click-handler [row col grid]
  (let [cell (-> grid (nth row) (nth col))
        content (:content cell)]
    (when-not (:is-open? cell)
      (cond
        (= "x" content) (rf/dispatch [:open-cell (show-all-bombs! grid)])
        (= " " content) (rf/dispatch
                         [:open-many-cells (->> [[row col]]
                                                (find-all-to-open! grid [[row col]])
                                                (open-all! grid))])
        :else (rf/dispatch
               [:open-cell (replace-cell (assoc cell :is-open? true) grid row col)])))))

(defn game []
  (let [grid @(rf/subscribe [:grid])]
    [:div
     [:button {:on-click #(rf/dispatch [:gridalize]) } "Start"]
     [:div {:style {:display "grid"
                    :grid-template-columns (apply str (repeat cols "auto "))
                    :column-gap "0px"
                    :width "fit-content"}}
      (for [r (-> grid count range)
            c (-> grid first count range)]
        (let [cell (-> grid (nth r) (nth c))]
          [:div {:on-click #(click-handler r c grid)
                 :key (str r "x" c)
                 :style {:width "20px" :height "20px" :border-style "solid"
                         :border-width "thin"
                         :background-color (if (:is-open? cell) "red" "green")}}
           (if (:is-open? cell) (:content cell))]))
      ]]))

