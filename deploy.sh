#!/bin/bash
mvn clean deploy -Prelease -pl '!multi-redis-spring-boot-sample'
