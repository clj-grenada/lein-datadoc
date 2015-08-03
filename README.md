# lein-datadoc

A Leiningen plugin for producing **Datadoc JARs** from your Leiningen projects.

Belongs to the [Grenada project](https://github.com/clj-grenada/grenada-spec).

Only tested with **Leiningen 2.5.1**.

## Getting started

Basic usage doesn't require any configuration. Steps to getting a Datadoc JAR
for **your Leiningen project** on Clojars:

  1. Decide if you want the `lein datadoc` plugin available in all your
     Leiningen projects or only in selected ones.

     a. **all projects** Augment your `.lein/profiles.clj` in the following way:
     ```clojure
     {:user {…
             :dependencies […
                            [org.clojure-grimoire/lein-grim "0.3.8"]]
             :plugins […
                       [org.clj-grenada/lein-datadoc "0.1.0"]]
             …}
       …}

     ```

     Note that this will place the lein-grim dependency on the classpath of all
     your Leiningen projects.

     b. **selected projects** Augment your `project.clj` in the following way:
     ```clojure
     (defproject …
       …
       :profiles
       {…
        :dev {…
              :dependencies […
                             [org.clojure-grimoire/lein-grim "0.3.8"]]
              :plugins […
                        [org.clj-grenada/lein-datadoc "0.1.0"]]
              …}
       …)
     ```

     For more information on profiles, see the [Leiningen
     documentation](https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md#default-profiles).

  3. In your project's root directory, run `lein datadoc install` to create a
     Datadoc JAR and install it into your **local** Maven repository. Or `lein
     datadoc deploy clojars` to create a Datadoc JAR and deploy it to Clojars.
     The **coordinates** in both cases will be the same as your project's.


## More information

Writhing with shame, I have to refer you to the [source
code](src/leiningen/datadoc.clj) and to the documentation emitted by `lein help
datadoc`. In a visible future I will provide better immediate support.

## License

See [`LICENSE.txt`](LICENSE.txt).
