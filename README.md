[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

# clustered kGenProg

kGenProg on a Computer Cluster

## How to build

```
git clone https://github.com/kusumotolab/clustered-kGenProg.git
cd clustered-kGenProg
git submodule update --init
./gradlew installDist
```

## Run Server

```
./node/build/install/node/bin/kGenProg-coordinator \
  --port 50051
```

## Run Client

```
./node/build/install/node/bin/kGenProg-client \
  --host localhost --port 50051 \
  --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
```
