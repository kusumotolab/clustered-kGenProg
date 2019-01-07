[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg&circle-token=7de79fe88bdd8eff9a276a35b460d988cc7a6100)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

# clustered kGenProg

kGenProg on a Computer Cluster

## How to build

```
git clone https://github.com/kusumotolab/clustered-kGenProg.git
cd clustered-kGenProg
./gradlew installDist
```

## Run Server

```
./node/build/install/node/bin/kGenProg-coordinator
```

## Run Client

```
./node/build/install/node/bin/kGenProg-client
```
