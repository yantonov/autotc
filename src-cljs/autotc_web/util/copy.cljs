(ns autotc-web.util.copy)

(defn copy [text]
  (let [element-id "elbaId"
        text-node (or (.getElementById js/document element-id)
                      (.createElement js/document "span"))]
    (set! (.-id text-node) element-id)
    (set! (.-innerText text-node) text)
    (set! (.-style text-node) "display: none:")
    (.appendChild (.-body js/document) text-node)
    (let [sel (.getSelection js/window)
          range (.createRange js/document)]
      (.selectNodeContents range text-node)
      (.removeAllRanges sel)
      (.addRange sel range)
      (.execCommand js/document "copy")
      (.removeChild (.-body js/document) text-node))))
