[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

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
