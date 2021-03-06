# pazuzu-ui
[![Travis BuildStatus](https://travis-ci.org/zalando/pazuzu-ui.svg?branch=master)](https://travis-ci.org/zalando/pazuzu-ui)

[![Stories in Ready](https://badge.waffle.io/zalando/pazuzu-ui.png?label=ready&title=Ready)](http://waffle.io/zalando/pazuzu-ui)

<img align="right" height="300" src="/pazuzu-logo.png">

What is Pazuzu?
---------------
Pazuzu is a tool that builds Docker images from feature snippets, while
resolving all dependencies between them. One of the common use cases is
Continuous Integration environment, where jobs require specific tooling present
for building and testing. Pazuzu can significantly ease that process, by letting user
choose from a wide selection of predefined Dockerfile snippets that represent
those dependencies (e.g. Golang, Python, Android SDK, customized NPM installs).

What is Pazuzu UI?
------------------------

UI for Pazuzu Registry where you will be able to create new features

This is single-page application implemented in ClojureScript using
[`Reagent`](https://github.com/reagent-project/reagent) as rendering engine and
[`re-frame`](https://github.com/Day8/re-frame) as a data-flow managing framework.

What is current stage?
----------------------
Project is still in early stages - this file will be updated to reflect the
progress and document usage and functionality

Features
--------
* [ ] not implemented
* [x] implemented

### **As ANONYMOUS user**
*Currently **no** functionality is available without authentication.
All below operations are available for ADMIN user*
 * [ ] I can **view/search list of features**
 * [ ] I can **fetch specific feature** with their dependencies
 * [ ] I can **create** new features

### **As ADMIN user**
  * [x] I can **do everything ANONYMOUS use can do**
  * [ ] I can **see the list of features to approve**
  * [ ] I can **approve** a feature
  * [x] I can **modify** a feature
  * [x] I can **delete** a feature

TODO
----
- Routing
- Auth and rol management for features
- ...

Development
-----------

### nginx

In order to work with the OAuth authentication we need an SSL terminating proxy
between the locally running UI process. There is a Docker container prepared
that you can simply build locally:

    $ cd nginx
    $ make build

In order to get the container running correctly the backend IP needs to be set
correctly. By default it tries to read the IP using `ip addr show en0`. In order
to overwrite this set the environment variable `BACKEND_IP`.

    $ # optional:
    $ export BACKEND_IP=123.456.789.0
    $ make run

### LESS compiling

To build and continuously watch and compile less styles

    lein less auto

### Starting in development mode

To start hacking checkout this repo and then to start a web server with live
reload run:

    lein figwheel dev

Developing locally without authentication is possible using a locally running
registry in dev profile and with:

    lein figwheel dev-noauth

### EMACS

If you choose to run the REPL inside emacs don't forget to add this to your
emacs config:

    (setq cider-cljs-lein-repl
          "(do (use 'figwheel-sidecar.repl-api) (start-figwheel!) (cljs-repl))")

Then just call `cider-jack-in-clojurescript`.

### Registry

To get content in for the UI you need to have running
[`pazuzu-registry`](https://github.com/zalando/pazuzu-registry).

### Tokens

When developing locally the authentication tokens need to be fetched from
`mint`. For this run:

    berry -a pazuzu-ui-dev -m $MINT_BUCKET --once -f /dev/null credentials

This will store the `client.json` and `user.json` in the `credentials`
directory from where the backend handler will set the `client_id` as cookie.

Developing locally
------------------

This is a small guide that hopefully demonstrates how to get the
[pazuzu-registry](https://github.com/zalando/pazuzu-registry) and the
[pazuzu-ui](https://github.com/zalando/pazuzu-ui) running locally with OAuth
enabled.

1. Clone both repositories

In one base directory run:

    git clone git@github.com:zalando/pazuzu-ui.git
    git clone git@github.com:zalando/pazuzu-registry.git

2. Create NGINX docker container

In order to get the SSL working we need nginx to terminate the ssl requests and
forward them to our running UI and registry processes.

    cd pazuzu-ui/nginx
    sudo make build
    sudo BACKEND_IP=$YOUR_EXTERNAL_IP make run

This will create an nginx proxy that forwards traffic from
`https://localhost:8080` to `http://$YOUR_EXTERNAL_IP:3449`, i.e. the running
figwheel repl. It will also forward traffic from `https://localhost:8081` to
`http://$YOUR_EXTERNAL_IP:8082`, i.e. the running registry.

3. Request client tokens from mint

In order to get both running we need client ids from mint:

    berry -a pazuzu-registry -m $MINT_BUCKET --once -f /dev/null ~/.berry/pazuzu-registry
    cd pazuzu-ui
    berry -a pazuzu-ui-dev -m $MINT_BUCKET --once -f /dev/null credentials

4. Start the registry

First, make sure your LDAP uid is in
`pazuzu-registry/src/main/resources/config/application-dev.yml`:

    pazuzu:
        registry:
            admins:
                dtruemper

Now build and start the registry:

    mvn package
    java -Dserver.port=8082 -Dspring.profiles.active=dev,oauth

You can also run the registry inside your IDE with the equivalent command line
args.

5. Start the UI

Inside EMACS simply call `cider-jack-in-clojurescript`. In the command line
this will result in similar experience:

    lein figwheel dev

Happy hacking!

Packaging
---------

To build a standalone runnable jar

    export BACKEND_ENPOINT=https://backend-registry-endpoint.ok
    lein uberjar
    java -jar ./target/pazuzu-ui.jar

if no BACKEND_ENDPOINT environment variable is specified,
the default backend endpoint will be http://localhost:8080


License
-------

The MIT License (MIT)
Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
