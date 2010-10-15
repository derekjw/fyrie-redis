# Fyrie Redis Scala client

## Key features of the library

- All commands are pipelined.
- User defined formatting of commands and parsing of replies, defaulting to Strings.
- Uses nonblocking IO and actors for increased throughput and responsiveness while minimizing the use of threads and sockets.

## Information about Redis

Redis is an advanced key-value store. It is similar to memcached but the dataset is not volatile, and values can be strings, exactly like in memcached, but also lists, sets, and ordered sets.

[code.google.com/p/redis](http://code.google.com/p/redis/)

### Key features of Redis

- Fast in-memory store with asynchronous save to disk.
- Key value get, set, delete, etc.
- Atomic operations on sets and lists, union, intersection, trim, etc.

## Requirements

- sbt ([code.google.com/p/simple-build-tool](http://code.google.com/p/simple-build-tool/))
- current Akka snapshot ([akkasource.org](http://akkasource.org))

## All tests are functional tests and require a running instance of Redis

## Usage

Start your redis instance (usually redis-server will do it). Tests default to port 16379, but can be changed back to the default of 6379 by editing src/test/resources/akka.conf

    $ cd fyrie-redis
    $ sbt
    > update
    > test (optional to run the tests)
    > console

And you are ready to start issuing commands to the server(s)

let's connect and get a key:

    scala> import net.fyrie.redis._
    scala> val r = new RedisClient("localhost", 6379)
    scala> r send set("key", "some value")
    scala> r send get("key")

## Acknowledgements

I would like to thank Alejandro Crosa and Debasish Ghosh for the work they have done creating [scala-redis](http://github.com/debasishg/scala-redis). I began this project as a fork of scala-redis, but after my third rewriting of it I decided I should split this off into a seperate project. Without the starting point that their work provided I never would have created this.

## License

This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

