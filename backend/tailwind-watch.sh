#!/bin/bash
# Dev watch: detecta mudanças nos templates e regenera output.css
./tools/tailwindcss \
  -i src/main/resources/static/css/input.css \
  -o src/main/resources/static/css/output.css \
  --watch
