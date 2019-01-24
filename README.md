[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg&circle-token=7de79fe88bdd8eff9a276a35b460d988cc7a6100)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

# clustered kGenProg

kGenProg on a Computer Cluster

## How to build

```
git clone https://github.com/kusumotolab/clustered-kGenProg.git
cd clustered-kGenProg
git submodule update --init
./gradlew installDist
```

## Run Coordinator

```
./node/build/install/node/bin/kGenProg-coordinator \
  --port 50051
```

## Run Worker (As many as you want)

```
./node/build/install/node/bin/kGenProg-worker \
  --host <Coordinator's Host> --port 50051
```

## Run Client

```
./node/build/install/node/bin/kGenProg-client \
  --host <Worker's Host> --port 50051 \
  --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
```
