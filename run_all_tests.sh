#!/usr/bin/env bash

sbt clean compile scalafmtAll scalastyleAll coverage test IntegrationTest/test dependencyUpdates coverageReport
