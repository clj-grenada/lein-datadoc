[![Build Status](https://travis-ci.org/clj-grenada/lein-datadoc.svg?branch=devel)](https://travis-ci.org/clj-grenada/lein-datadoc)

# lein-datadoc

A Leiningen plugin for producing **Datadoc JARs** from your Leiningen projects.

If you **don't know** about Datadoc JARs: they are a way of packaging and
publishing documentation for your project/library. Good documentation increases
adoption, so why not *do the data doc*? Most likely, all you need to do is
follow the two steps described [below](#getting-started).

Belongs to the [Grenada project](https://github.com/clj-grenada/grenada-spec).
For an **overview** of Grenada documentation, [see
here](https://github.com/clj-grenada/lib-grenada/blob/devel/doc/overview.md).

Only tested with **Leiningen 2.5.1**.

## Getting started

These two steps produce a Datadoc JAR from **your Leiningen project** and deploy
it to Clojars. The Datadoc JAR will **contain** roughly the same information as
API documentation pages produced by
[Codox](https://github.com/weavejester/codox) or
[Autodoc](https://github.com/tomfaulhaber/autodoc). See [below](#cmetadata-bars)
for how to provide additional information.

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
                               [org.clojars.rmoehn/lein-grim "0.3.10"]]
                :plugins […
                          [org.clj-grenada/lein-datadoc "1.0.0-rc.2"]}}
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
                             [org.clojars.rmoehn/lein-grim "0.3.10"]]
              :plugins […
                        [org.clj-grenada/lein-datadoc "1.0.0-rc.2"]]})
     ```

  2. In your project's root directory, run `lein datadoc install` to create a
     Datadoc JAR and install it into your **local** Maven repository. Or `lein
     datadoc deploy clojars` to create a Datadoc JAR and deploy it to Clojars.
     The **coordinates** in both cases will be the same as your project's.

     Note that **Clojars** doesn't know about Datadoc JARs (yet), so it will
     display your Datadoc JAR in an odd way or not at all. But don't worry, if
     you didn't see an error while deploying, everything will be where you want
     it to be. If Datadoc JARs become popular, Clojars will probably start
     supporting them.

## Cmetadata Bars

Sometimes a doc string is not enough. – You want to provide **additional
structured information** about your [concrete
things](https://github.com/clj-grenada/grenada-spec/blob/master/NewModel.md).
For example, you want to specify when a concrete thing was added or deprecated,
or simply the markup language used for its doc string. In order to do this, you
can attach Bars in a concrete Thing's
[Cmetadata](https://github.com/clj-grenada/grenada-spec/blob/master/NewModel.md#a-new-data-model).
Looks like this:

```clojure
(defn identity
  "Returns its argument `x` unchanged."
  {:grenada.cmeta/bars {:grenada.bars/lifespan {:added "1.0.4"
                                                :deprecated nil}
                        :poomoo.bars/markup :common-mark}}
  [x]
  x)
```

See the namespaces
[`grenada.bars`](https://github.com/clj-grenada/lib-grenada/blob/master/src/clj/grenada/bars.clj)
and
[`poomoo.bars`](https://github.com/clj-grenada/poomoo/blob/master/src/poomoo/bars.clj)
for the currently **available Bar types**. If you don't find a Bar type fit to
hold your information, you can [define your
own](https://github.com/clj-grenada/poomoo/blob/master/src/poomoo/bars.clj).

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

The drawback is that you can't use **lein do** with this setup. `lein do datadoc
this, datadoc that` will not do what you want in certain cases.

For more information on profiles, see the [Leiningen
documentation](https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md#default-profiles).

## More information

Writhing with shame, I have to refer you to the [source
code](src/leiningen/datadoc.clj) and to the documentation emitted by `lein help
datadoc`. In a visible future I will provide better immediate support.

## License

See [`LICENSE.txt`](LICENSE.txt).
