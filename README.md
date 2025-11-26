# bc-passengers-frontend

This is the frontend for the passengers declaration service.

## Prerequisites

This service is written in [Scala](https://www.scala-lang.org/) and the [Play Framework](https://www.playframework.com/), therefore you will need at least a [Java Runtime Environment](https://www.java.com/en/download/) to run it. You will also need [mongodb](https://mongodb.com) by either [locally installing it](https://docs.mongodb.com/guides/server/install/) or running a [mongo docker container](https://hub.docker.com/_/mongo).

## Getting Started

1. Clone this repository with:
   ```
   git clone git@github.com:hmrc/bc-passengers-frontend.git
   ```
2. Run the services (minus the frontend) via [service manager 2](https://github.com/hmrc/sm2) with the following profile:
   ```
   sm2 --start BC_PASSENGERS_ALL
   sm2 --stop BC_PASSENGERS_FRONTEND
   ```
3. Restart the service locally:
   ```
   cd bc-passengers-frontend
   sbt run
   ```
4. For the card payment service to run successfully, you need the internal auth service running and to enable a token by doing the following command:
   ```
   sm2 --start INTERNAL_AUTH INTERNAL_AUTH_FRONTEND --appendArgs '{"INTERNAL_AUTH": ["-Dapplication.router=testOnlyDoNotUseInAppConf.Routes"], "INTERNAL_AUTH_FRONTEND": ["-Dapplication.router=testOnlyDoNotUseInAppConf.Routes"]}'
   ```
   
   And then insert a token using:
   ```
   curl -X POST http://localhost:8470/test-only/token \
      -H "Content-Type: application/json" \
      -d '{
        "token": "123456",
        "principal": "card-payment-frontend",
        "permissions": [
          {
            "resourceType": "card-payment",
            "resourceLocation": "card-payment/*",
            "actions": ["READ", "WRITE"]
          }
        ]
      }'
   ```

   Start pay-frontend with the transitionary toggle off so all journey's go via card-payment-frontend:
   ```
   sm2 -start OPS_ACCEPTANCE --appendArgs '{"PAY_FRONTEND" : ["-Dfeature.percentage-of-users-to-go-use-soap=0"]}'
   ```

5. To get to the first page of the service, you should be able to use the `where-goods-bought` endpoint (i.e. http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought)

## Tests

To format and check the code style, compile code, run tests with coverage, generate a coverage report, and check for dependency updates:

```
./run_all_tests.sh
```


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
