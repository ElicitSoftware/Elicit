#!/bin/bash

docker compose up -d
sleep 20
docker compose restart survey
