#!/usr/bin/env bash
set -e
if [ "$1" = "--build" ]; then
  docker compose up --build -d
else
  docker compose up -d
fi
docker compose ps
