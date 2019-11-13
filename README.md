# coddled-super-centaurs

A reimplementation of the stateful transducers from `clojure.core`, in a way that opens their states to extension.

### Why?

Suppose you want to use stateful transducers in a context in which you need control over their states, for example to persist their states to disk and restore them.

If you try to do this with the ones in `clojure.core`, you will run into a snag: they construct their states themselves in a lexical scope, usually a value in a `volatile!`, sometimes a java collection... not something that is easy for you to extend from Clojure.

If `volatile!` were a dynamic function, then you could replace its implementation in order to manage the transducer's state yourself.

Since it is not, there is no straightforward way to access a transducer's state, unless the transducer is implemented to allow this. They generally are not open to this sort of extension. The functions in `clojure.core` which return stateful transducers are not. Replicas of all of them are provided in this library (only the arity that returns the stateful transducer).

Their implementations are trivially different from those in `clojure.core`. The main difference is that instead of calling `volatile!` to construct their state, they call a dynamic function `coddled-super-centaurs.core/*state*` and expect it to return a `volatile!`. Also, instead of a `java.util.ArrayList` they use a vector in a `volatile!`.

### Performance

I have not bothered to do any benchmarking, however these are bound to be slower than their `clojure.core` counterparts, since they are identical but for the addition of some indirection and the use of Clojure collections instead of Java collections. You should not use these unless you need control over the states.

### Usage

See their usage in [`noah`](https://github.com/blak3mill3r/noah), which was my motivation for this.

### License

Copyright © 2019 Blake Miller

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
