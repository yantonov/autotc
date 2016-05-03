## autotc

tiny [clojure](http://clojure.org)/[clojurescript](https://github.com/clojure/clojurescript)/[react](https://facebook.github.io/react/) app which helps to:  
1. start/stop/reboot multiple [teamcity](https://www.jetbrains.com/teamcity/) agents  
2. copy test name by 1 click  
3. copy stack trace by 1 click  
4. find current problems (failed tests), with respect to last (not finished) build  

## Prerequisites

1. [jdk][1];
2. [maven][2];
3. [leiningen][3];

[1]: http://www.oracle.com/technetwork/java/javase/downloads/index.htmla
[2]: http://maven.apache.org
[3]: https://github.com/technomancy/leiningen

## Install dependencies

```bash
    bin/install/install-dependencies.sh
```

## Running

To start a web server for the application, run:

```bash
    bin/run.sh
```

## How it looks
![screenshot](https://raw.githubusercontent.com/yantonov/autotc/master/docs/screenshot.png)

## License

Copyright © 2015 FIXME
