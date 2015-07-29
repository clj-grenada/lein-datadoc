(ns leiningen.datadoc
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [grenada
             [config :as gren-config]
             [core :as gren-core]
             [exporters :as exporters]
             [utils :as gren-utils]]
            [grimoire.api.fs :as api.fs]
            [jolly.core :as jolly]
            [leiningen
             [clean :as lein-clean]
             [deploy :as lein-deploy]
             [pom :as lein-pom]]
            [leiningen.core
             [eval :as lein-eval]
             [main :as lein]]
            [plumbing
             [core :as plumbing :refer [fnk safe-get safe-get-in <-]]
             [graph :as graph]]
            [schema.core :as s])
  (:import org.sonatype.aether.transfer.NoRepositoryConnectorException))

;;;; Helper functions

(defn- name-of-var
  "Returns the name under which var V is interned as a string."
  [v]
  (-> v
      (safe-get :name)
      name))

(defn- str-file
  "Like io/file, but (str …)ingifies the resulting File."
  [& args]
  (str (apply io/file args)))


;;;; Schema for the :datadoc entry in project.clj

;; MAYBE TODO: Add options to switch off diagnostic messages. (RM 2015-07-30)
;; MAYBE TODO: Add a subtask that prints the current configuration to help the
;;             user figure out the defaults in case they need to.
;;             (RM 2015-08-02)
(def PluginConfig
  "Schema for this plugin's configuration. If you want to influence what this
  plugin does, you can put a map that adheres to this Schema in **project.clj**.

  ```clojure
  (defproject …

    …
    :datadoc {…
              :jar-coords …}

    …)


  Unfortunately, you can't see the Schema in generated docs, so you'll have to
  look into the source code. There you'll also find comments **explaining** the
  entries in the configuration map. All entries are optional, as is the whole
  `:datadoc` map itself. The **defaults** are chosen to comply with
  Leiningen conventions."
  {;; Paths to directories with Clojure files to extract doc data from.
   ;; Relative to project root.
   (s/optional-key :source-paths) [s/Str]

   ;; Path to the directory where to store all the output. Relative to project
   ;; root. 'lein datadoc clean' will REMOVE everything below this path.
   (s/optional-key :target-path) s/Str

   ;; Coordinates of the Datadoc JAR to produce
   (s/optional-key :jar-coords)
   {;; Maven groupId
    (s/optional-key :group) s/Str
    ;; Maven artifactId
    (s/optional-key :artifact) s/Str
    ;; Maven version
    (s/optional-key :version) s/Str}})


;;;; Configuration generators

;; MAYBE TODO: Make this prettier. Is there no good way to have a map with some
;;             defaults and some computation? (RM 2015-07-29)
(defn make-config
  "Given the PROJECT-MAP as provided by Leiningen, computes and returns a map of
  configuration values for this plugin's operation."
  [project-map]
  (let [user-config (s/validate PluginConfig (get project-map :datadoc {}))
        pm project-map

        pre-conf
        {:group        (safe-get pm :group)
         :artifact     (safe-get pm :name)
         :version      (safe-get pm :version)
         :root         (safe-get pm :root)
         :source-paths (safe-get pm :source-paths)
         :target-path  (safe-get pm :target-path)
         :local-repo   (get pm :local-repo) ; Can be not there and works anyway.
                                            ;  – Same in Leiningen itself.

         :project-map  pm

         :datadoc
         {:source-paths (if-let [sps (:source-paths user-config)]
                          (map #(str-file (safe-get pm :root) %) sps)
                          (safe-get pm :source-paths))
          :target-path  (if-let [tp (:target-path user-config)]
                          (str-file (safe-get pm :root) tp)
                          (str-file (safe-get pm :target-path) "datadoc"))

          :jar-coords
          {:group
           (get-in user-config [:jar-coords :group]    (safe-get pm :group))

           :artifact
           (get-in user-config [:jar-coords :artifact] (safe-get pm :name))

           :version
           (get-in user-config [:jar-coords :version]  (safe-get pm :version))}}}

        graph-conf
        ((graph/eager-compile
           {;; Where the Grimoire data should be stored
            :grimoire-out
            (fnk [[:datadoc target-path]]
              (str-file target-path "grimoire-data"))

            ;; Configuration object for lib-grimoire calls
            :grimoire-config
            (fnk [grimoire-out]
              (api.fs/->Config grimoire-out grimoire-out grimoire-out))

            ;; Where the raw Grenada data should be stored
            :grenada-out
            (fnk [[:datadoc target-path]]
              (str-file target-path "grenada-data"))})
         pre-conf)]
    (merge pre-conf graph-conf))) ; If this confuses you, read about Graph.

(defn make-aether-args
  "Given configuration data as returned by clj::leiningen.datadoc/make-config,
  return map of arguments that both clj::cemerick.pomegranate.aether/install and
  clj::cemerick.pomegranate.aether/deploy take."
  [config]
  (plumbing/letk [[local-repo
                   [:datadoc
                    ,,target-path
                    ,,[:jar-coords group artifact version]]]
                  config]
    {:coordinates [(symbol group artifact) version :classifier "datadoc"]
     :jar-file (io/file target-path (gren-core/jar-name artifact version))
     :pom-file (io/file target-path "pom.xml")
     :local-repo local-repo}))


;;;; Procedures for on-demand running of other subtasks

(declare clean collect jar install deploy)

(defn ensure-collect [config]
  (let [grenada-out-file (io/file (safe-get config :grenada-out))]
    (when-not (.exists grenada-out-file)
      (collect config))
    (assert (.exists grenada-out-file)
            (str "datadoc: Cannot find directory with Grenada data, even"
                 " after collecting them."))))

(defn ensure-jar [config jar-file pom-file]
  (when-not (.exists jar-file)
    (jar config))
  (assert (and (.exists jar-file) (.exists pom-file))
          "datadoc: Cannot find JAR or POM even after installation."))


;;;; Subtasks

;; TODO: Add incorporation of external metadata. (RM 2015-07-28)
;; TODO: Add incorporation of metadata annotations. (RM 2015-07-30)
(defn collect
  "Extract metadata from your Clojure files.

  This puts the metadata somewhere below target/ (unless you've specified
  target directory). However, I don't expect you to want to look at those
  metadata. Run 'lein datadoc jar' in order to produce a Datadoc JAR that can be
  installed, deployed and loaded into tools that support it.

  WARNING: you shouldn't **edit** the files produced by 'lein datadoc collect'.
  They will be removed by the next 'lein datadoc clean' or 'lein datadoc
  collect'.

  Your project has to use Clojure ≧ 1.7.0 in order for this to work, because
  lein-grim uses clj::clojure.core/update."
  {:help-arglists []}
  [config]
  (clean config)

  (lein/info "datadoc collect: Extracting data from sources.")

  (plumbing/letk [[group
                   artifact
                   version
                   project-map
                   grimoire-out
                   [:datadoc source-paths]]
                  ,,config]

    (lein/info
      (str
        "■■■■■■■■\n"
        "If the compiler throws a FileNotFoundException after this, you might"
        " have forgotten to add the lein-grim dependency to .lein/profiles or"
        " project.clj. See the README.\n"
        "■■■■■■■■"))

    (lein-eval/eval-in-project
      project-map
      `(grimoire.doc/-main ~group
                           ~artifact
                           ~version
                           ~(vec source-paths)
                           "--clobber"
                           "true"
                           "source"
                           "clj"
                           ~grimoire-out)
      '(require 'grimoire.doc)))

  (->> (jolly/read-all-things (safe-get config :grimoire-config))
       (jolly/grim-ts->gren-ts-with-bars (safe-get config :grimoire-config))
       (<- (exporters/fs-hier (safe-get config :grenada-out)))))

(defn jar
  "Create a JAR from the extracted metadata.

  If the metadata aren't found, runs the equivalent of 'lein datadoc collect'
  first."
  {:help-arglists []}
  [config]
  (ensure-collect config)
  (lein/info "datadoc jar: Creating JAR from compiled data.")
  (gren-core/jar-from-files
    (safe-get config :grenada-out)
    (safe-get-in config [:datadoc :target-path])
    (gren-utils/safe-select-keys (safe-get-in config [:datadoc :jar-coords])
                                 #{:group :artifact :version})))

;; Credits: https://github.com/technomancy/leiningen/blob/2e181037521c1837fa8e75913f5744fe4aa28bf4/src/leiningen/install.clj
(defn install
  "Install Datadoc JAR in local Maven repository.

  Creates the JAR with 'lein datadoc jar' if it doesn't exist."
  {:help-arglists []}
  [config]
  (plumbing/letk [[jar-file pom-file :as aether-map] (make-aether-args config)]
    (ensure-jar config jar-file pom-file)
    (lein/info "datadoc install: Installing Datadoc JAR into local Maven repo.")
    (plumbing/mapply aether/install aether-map)))

;; Note: Keep in mind that the Leiningen procedures used in the following are
;;       not really part of the public API.
;;
;; Credits: https://github.com/technomancy/leiningen/blob/370423622cdd961c527749faf7336d6ce8bb39a5/src/leiningen/deploy.clj
(defn deploy
  "Deploy Datadoc to a Maven repository.

  Deployment happens according to your deployment settings. If you **can't
  remember** having made any settings for deployment, you probably want to write
  'clojars' for REPO-NAME.

  In order to be consistent with 'lein deploy', REPO-NAME defaults to
  'snapshots' for snapshot releases and 'releases' for non-snapshot releases.
  Note that Leiningen doesn't provide any **default settings** for these
  repositories. – You have specify them yourself."
  {:help-arglists ['() '(repo-name)]}
  ([config]
   (deploy config (if (lein-pom/snapshot? (safe-get config :project-map))
                    "snapshot"
                    "releases")))
  ([config repo-name]
   (plumbing/letk [[project-map] config
                   [jar-file pom-file :as aether-map] (make-aether-args config)]
     (ensure-jar config jar-file pom-file)
     (lein/info (str
                  "Deploying Datadoc JAR.\n"
                  "✻✻✻✻✻ At this point Leiningen might say something about"
                  " password prompts after other tasks. If that's the case,"
                  " simply run 'lein datadoc deploy …' again."))
     (try
       (plumbing/mapply
         aether/deploy
         :repository [(lein-deploy/repo-for project-map repo-name)]
         :transfer-listener :stdout
         aether-map)
       (catch Exception e
         (if (instance? NoRepositoryConnectorException (.getCause e))
           (lein/abort
             (str
               "datadoc deploy: Can't connect to the specified repository: '"
               repo-name "' Probably it's not configured in project.clj."
               " Did you mean 'lein datadoc deploy clojars'?"))
           (do
             (lein/info
               (str
                 "■■■■■■■■\n"
                 "About to rethrow the exception that occurred when trying to"
                 " deploy. Potential causes for that exception:\n"
                 " - your credentials are wrong (ReasonPhrase: Unauthorized)\n"
                 " - you're trying to deploy to a group you're not allowed to"
                 " deploy to or have messed up something else about the"
                 " coordinates. (ReasonPhrase: Forbidden)\n"
                 "■■■■■■■■"))
             (throw e))))))))

;; Note: Keep in mind that the Leiningen procedure used here is not really part
;;       of the public API.
(defn clean
  "Remove artifacts and directories created by 'lein datadoc'.

  This removes everything that might have been created by 'lein datadoc collect'
  or 'lein datadoc jar', so please don't edit those files by hand. Support for
  manually supplied data will hopefully be added later and those files won't be
  removed by 'lein datadoc clean'. Promised."
  {:help-arglists []}
  [config]
  (plumbing/letk [[[:datadoc target-path]] config]
    (if (.exists (io/file target-path))
      (do (lein-clean/delete-file-recursively target-path)
        (lein/info "datadoc clean: Removed Datadoc files."))
      (lein/info "datadoc clean: Nothing to remove."))))


;;;; Main task

(def ^:private subtasks [#'collect #'jar #'install #'deploy #'clean])
(def ^:private subtask-for (plumbing/for-map [v subtasks]
                             (name-of-var v) (var-get v)))

(defn datadoc
  "Extract, package and deploy Clojure metadata.

  If no SUB-NAME argument is provided, executes the equivalent of 'lein do
  datadoc collect, datadoc jar, datadoc install'.

  Note: When you change the configuration, you have to rerun 'lein datadoc
  collect' before running 'lein datadoc jar' or 'lein datadoc deploy'."
  {:subtasks subtasks
   :help-arglists ['(sub-name) '("deploy" repo-name)]}
  ([project-map]
   (let [config (make-config project-map)]
     (doseq [subtask-fn [collect jar install]]
       (subtask-fn config))))

  ([project-map sub-name]
   (let [config (make-config project-map)]
     (if-let [subtask-fn (get subtask-for sub-name)]
       (subtask-fn config)
       (lein/abort "Unknown subtask: " sub-name))))

  ([project-map sub-name repo-name]
   (when-not (= "deploy" sub-name)
     (lein/abort
       (str
         "lein datadoc: Unrecognized argument '" repo-name "' for subtask '"
         sub-name "'. Only 'lein datadoc deploy' can take an extra argument.")))
   (let [config (make-config project-map)]
     (deploy config repo-name))))
