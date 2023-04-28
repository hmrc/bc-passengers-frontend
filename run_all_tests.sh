#!/usr/bin/env bash
sbt clean compile scalafmtAll scalastyleAll coverage test IntegrationTest/test a11y:test dependencyUpdates coverageReport