#!/bin/bash

docker compose up -d
sleep 20
docker restart elicit-survey-1
