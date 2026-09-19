#!/bin/bash
# Build the Elicit Superset image (official apache/superset plus Authlib, Playwright,
# Pillow, and the Elicit configuration). Run from this directory.
docker build -t elicitsoftware/superset:latest .
