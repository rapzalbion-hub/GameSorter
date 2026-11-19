#!/bin/bash
# Compile lalu jalankan. Butuh JDK 11 atau lebih baru (javac harus ada).
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/*.java && java -cp out GameApp
