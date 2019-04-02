# bc-passengers-frontend

This is the frontend for the passengers declaration service.

## Builds

You can view either the [pipeline build](https://build.tax.service.gov.uk/job/Passengers/job/bc-passengers-frontend-pipeline/) or the specific [bc-passengers-frontend build](https://build.tax.service.gov.uk/job/Passengers/job/bc-passengers-frontend/) on the [jenkins build environment](https://build.tax.service.gov.uk/)

## Prerequisites

This service is written in [Scala](https://www.scala-lang.org/) and the [Play Framework](https://www.playframework.com/), therefore you will need at least a [Java Runtime Environment](https://www.java.com/en/download/) to run it. You will also need [mongodb](https://mongodb.com) by either [locally installing it](https://docs.mongodb.com/guides/server/install/) or running a [mongo docker container](https://hub.docker.com/_/mongo).

## Getting Started

1. Clone this repository with:
   ```bash
   git clone git@github.com:hmrc/bc-passengers-frontend.git
   ```
2. Run the services (minus the frontend) via [service manager](https://github.com/hmrc/service-manager) with the following profile:
   ```bash
   sm --start BC_PASSENGERS_ALL -f
   sm --stop BC_PASSENGERS_FRONTEND
   ```
3. Restart the service from your local repository on the appropriate port, for example:
   ```bash
   cd bc-passengers-frontend
   sbt 'run 9008'
   ```
4. To get to the first page of the service, you should be able to use the `where-goods-bought` endpoint (i.e. http://localhost:9008/check-tax-on-goods-you-bring-into-the-uk/where-goods-bought)

## Tests

To run the tests locally, simply run the `sbt test` command in the root of the repository
