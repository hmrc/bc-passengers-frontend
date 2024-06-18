# bc-passengers-frontend

This is the frontend for the passengers declaration service.

## Prerequisites

This service is written in [Scala](https://www.scala-lang.org/) and the [Play Framework](https://www.playframework.com/), therefore you will need at least a [Java Runtime Environment](https://www.java.com/en/download/) to run it. You will also need [mongodb](https://mongodb.com) by either [locally installing it](https://docs.mongodb.com/guides/server/install/) or running a [mongo docker container](https://hub.docker.com/_/mongo).

## Getting Started

1. Clone this repository with:
   ```bash
   git clone git@github.com:hmrc/bc-passengers-frontend.git
   ```
2. Run the services (minus the frontend) via [service manager 2](https://github.com/hmrc/sm2) with the following profile:
   ```bash
   sm2 --start BC_PASSENGERS_ALL
   sm2 --stop BC_PASSENGERS_FRONTEND
   ```
3. Restart the service locally:
   ```bash
   cd bc-passengers-frontend
   sbt run
   ```
4. To get to the first page of the service, you should be able to use the `where-goods-bought` endpoint (i.e. http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought)

## Tests

To format and check the code style, compile code, run tests with coverage, generate a coverage report, and check for dependency updates:

```bash
./run_all_tests.sh
```

## Accessibility Tests

### Prerequisites
Have node installed on your machine

### Execute tests
To run the tests locally, simply run:
```bash
sbt clean A11y/test
```

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
