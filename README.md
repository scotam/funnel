
# Funnel [![Build Status](https://api.travis-ci.org/scotam/funnel.png)](http://travis-ci.org/scotam/funnel) [![Dependencies Status](http://clj-deps.herokuapp.com/github/scotam/funnel/status.png)](http://clj-deps.herokuapp.com/github/scotam/funnel)

Funnel is a super-small piece of [Ring](https://github.com/ring-clojure) 
[middleware](https://github.com/ring-clojure/ring/wiki/Concepts#middleware)
for limiting the number of concurrent requests being processed.

## Usage

```clojure
(:require [funnel.core :refer [wrap-funnel]])

(-> app
    (wrap-funnel))
```

## Options

By default a funnel of size 1 will be created, so only one request will be let
through to be processed at a time. To increase the funnel size...

```clojure
(wrap-funnel {:funnel-size 10})
```

Also, when requests block they will wait for 30 seconds before timing out with
an error, you can customize this timeout with...

```clojure
(wrap-funnel {:funnel-wait-timeout 20000})
```

## Timeouts

When the funnel wait timeout is exceeded a *429* response is returned. Handlers
that are wrapped by _wrap-funnel_ are also given a default timeout of 60
seconds. This can be customized using...

```clojure
(wrap-funnel {:funnel-handler-timeout 30000})
```

## Metadata

Funnel adds some metadata onto requests (for the purpose of monitoring, logging
and debugging).

### :funnel-wait-time

This is the amount of time the request had to wait before being processed.

### :funnel-handler-time

This is the amount of time the request took to process (excluding wait time)

