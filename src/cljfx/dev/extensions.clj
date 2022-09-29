(in-ns 'cljfx.dev)

(s/def :cljfx.ext-on-instance-lifecycle/on-created ifn?)
(s/def :cljfx.ext-on-instance-lifecycle/on-advanced ifn?)
(s/def :cljfx.ext-on-instance-lifecycle/on-deleted ifn?)

(register-type! `fx/ext-on-instance-lifecycle
  :spec (s/keys :opt-un [:cljfx.ext-on-instance-lifecycle/on-created
                         :cljfx.ext-on-instance-lifecycle/on-advanced
                         :cljfx.ext-on-instance-lifecycle/on-deleted]
                :req-un [:cljfx/desc])
  :of :desc)

(s/def :cljfx.ext-let-refs/refs (s/nilable (s/map-of any? :cljfx/desc)))

(register-type! `fx/ext-let-refs
  :spec (s/keys :req-un [:cljfx/desc :cljfx.ext-let-refs/refs])
  :of :desc)

(s/def :cljfx.ext-get-ref/ref any?)

(register-type! `fx/ext-get-ref :spec (s/keys :req-un [:cljfx.ext-get-ref/ref]))

(s/def :cljfx.ext-set-env/env map?)

(register-type! `fx/ext-set-env
  :spec (s/keys :req-un [:cljfx/desc :cljfx.ext-set-env/env])
  :of :desc)

(s/def :cljfx.ext-get-env/env coll?)

(register-type! `fx/ext-get-env
  :spec (s/keys :req-un [:cljfx/desc :cljfx.ext-get-env/env])
  :of :desc)

(s/def :cljfx.ext-many/desc (s/coll-of :cljfx/desc))

(register-type! `fx/ext-many
  :spec (s/keys :req-un [:cljfx.ext-many/desc])
  :of 'java.util.Collection)

(s/def :cljfx.make-ext-with-props/props (s/nilable map?))

(register-type! `fx/make-ext-with-props
  :spec (s/keys :req-un [:cljfx/desc :cljfx.make-ext-with-props/props])
  :of :desc)