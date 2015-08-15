# lein-datadoc

A Leiningen plugin for producing **Datadoc JARs** from your Leiningen projects.

If you **don't know** about Datadoc JARs: they are a way of packaging and
publishing documentation for your project/library. Good documentation increases
adoption, so why not *do the data doc*? Most likely, all you need to do is
follow the two steps described [below](#getting-started).

Belongs to the [Grenada project](https://github.com/clj-grenada/grenada-spec).
For an **overview** of Grenada documentation, [see
here](https://github.com/clj-grenada/lib-grenada/doc/overview.md).

Only tested with **Leiningen 2.5.1**.

## Getting started

Steps to getting a Datadoc JAR for **your Leiningen project** on Clojars:

  1. Decide if you want the `lein datadoc` plugin available in all your
     Leiningen projects or only in selected ones.

     a. **all projects** Augment your `.lein/profiles.clj` in the following way:
     ```clojure
     {…
      :user {…
             :aliases {…
                       "datadoc" ["with-profile" "datadoc" "datadoc"]}}
      :datadoc {…
                :dependencies […
                               [org.clojure-grimoire/lein-grim "0.3.8"]]
                :plugins […
                          [org.clj-grenada/lein-datadoc "0.1.0"]}}
     ```

     Yes, this introduces a new profile. See [below](#why-profile-and-alias) why
     this necessary.

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
                        [org.clj-grenada/lein-datadoc "0.1.0"]]})
     ```

  3. In your project's root directory, run `lein datadoc install` to create a
     Datadoc JAR and install it into your **local** Maven repository. Or `lein
     datadoc deploy clojars` to create a Datadoc JAR and deploy it to Clojars.
     The **coordinates** in both cases will be the same as your project's.

     Note that **Clojars** doesn't know about Datadoc JARs (yet), so it will
     display your Datadoc JAR in an odd way or not at all. But don't worry, if
     you didn't see an error while deploying, everything will be where you want
     it to be. If Datadoc JARs become popular, Clojars will probably start
     supporting them.

## Why profile and alias?

There's a problem with AOT compilation and some bugs somewhere. See Leiningen
[#1563](https://github.com/technomancy/leiningen/issues/1563) and
[#1739](https://github.com/technomancy/leiningen/issues/1739) for details. As a
workaround they suggest putting `:exclusions` in the plugin dependency, but in
this case we actually need `org.clojure/core.cache`, so we can't exclude it.

Adding a `:datadoc` profile keeps the Datadoc stuff out of the way and the
alias makes it easy to type. An extra benefit is that lein-grim doesn't end up
in all your projects' classpaths, which it would do if you put it in the `:user`
profile.

For more information on profiles, see the [Leiningen
documentation](https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md#default-profiles).

## More information

Writhing with shame, I have to refer you to the [source
code](src/leiningen/datadoc.clj) and to the documentation emitted by `lein help
datadoc`. In a visible future I will provide better immediate support.

## License

See [`LICENSE.txt`](LICENSE.txt).
