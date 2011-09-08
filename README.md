# Fyrie Redis Scala client

## Key features of the library

- All commands are pipelined asynchronously.
- User defined formatting of command parameters and parsing of replies.
- Uses nonblocking IO and actors for increased throughput and responsiveness while minimizing the use of threads and sockets.

## Information about Redis

Redis is an advanced key-value store. It is similar to memcached but the dataset is not volatile, and values can be strings, exactly like in memcached, but also lists, sets, and ordered sets.

[code.google.com/p/redis](http://code.google.com/p/redis/)

### Key features of Redis

- Fast in-memory store with asynchronous save to disk.
- Key value get, set, delete, etc.
- Atomic operations on sets and lists, union, intersection, trim, etc.

## Requirements

Fyrie Redis version numbers match the version of Akka that it is compatible with. The recommended version is currently 1.2, which can be found on the ['akka-1.2' branch](https://github.com/derekjw/fyrie-redis/tree/akka-1.2).

If building from source sbt 0.10 ([github.com/harrah/xsbt/wiki](https://github.com/harrah/xsbt/wiki)) is required.

Releases and snapshots are located at [repo.fyrie.net](http://repo.fyrie.net), which can be used in sbt like this:

    resolvers += "fyrie snapshots" at "http://repo.fyrie.net/snapshots"
    
    libraryDependencies += "net.fyrie" %% "fyrie-redis" % "1.2-SNAPSHOT"

## All tests are functional tests and require a running instance of Redis

## Usage

Start your redis instance (usually redis-server will do it). Tests default to port 6379, but can be changed by editing src/test/resources/akka.conf. Do not run tests on a production redis server as all data on the redis server will be deleted.

    $ cd fyrie-redis
    $ sbt
    > update
    > test (optional to run the tests)
    > console

And you are ready to start issuing commands to the server(s)

let's connect and get a key:

    scala> import net.fyrie.redis._
    scala> val r = new RedisClient("localhost", 6379)
    scala> r.set("key", "some value")
    scala> r.sync.get("key").parse[String]

There are 3 ways to run a command:

- using 'async': result will be returned asynchronously within a Future ([akka.io/docs/akka/snapshot/scala/futures.html](http://akka.io/docs/akka/snapshot/scala/futures.html)).
- using 'sync': will wait for the result. Only recommended when testing.
- using 'quiet': No result will be returned. Command is sent asynchronously. Good for quickly making updates.

The default is 'async' if none of the above are used.

MultiExec commands can also be given:

    val result = r.multi { rq =>
      for {
        _ <- rq.set("testkey1", "testvalue1")
        _ <- rq.set("testkey2", "testvalue2")
        x <- rq.mget(List("testkey1", "testkey2")).parse[String]
      } yield x
    }

The above example will return a 'Future[List[String]]'

Another MultiExec example:

    r.lpush("mylist", "value1")
    r.lpush("mylist", "value2")

    val result = r.multi { rq =>
      for {
        _ <- rq.lpush("mylist", "value3")
        a <- rq.rpop("mylist").parse[String]
        b <- rq.rpop("mylist").parse[String]
        c <- rq.rpop("mylist").parse[String]
      } yield (a, b, c)
    }

This example will return a '(Future[String], Future[String], Future[String])'

## Documentation

The api is published at ([http://derekjw.github.com/fyrie-redis](http://derekjw.github.com/fyrie-redis)), but it's documentation is far from complete.

Until proper documentation is completed, the tests can be used as examples: ([github.com/derekjw/fyrie-redis/tree/master/src/test/scala/net/fyrie/redis](https://github.com/derekjw/fyrie-redis/tree/master/src/test/scala/net/fyrie/redis))

## TODO

- Support clustering
- Documentation

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

