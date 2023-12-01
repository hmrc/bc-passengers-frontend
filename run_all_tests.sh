#!/usr/bin/env bash

sbt clean compile scalafmtAll scalastyleAll coverage test it/test dependencyUpdates coverageReport
