[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg&circle-token=7de79fe88bdd8eff9a276a35b460d988cc7a6100)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

# cluster-basded kGenProg

kGenProg with cluster computing

## How to build

```
$ git clone git@github.com:kusumotolab/clustered-kGenProg.git
$ cd clustered-kGenProg
$ git submodule update --init
$ ./gradlew installDist
```

## How to use manually
Please execute each command on each machine.  
(If you want to try the tool quickly, while you can execute each command on the same machine, the loads are not dispersed)
### Run Coordinator

```
$ ./node/build/install/node/bin/kGenProg-coordinator \
  --port 50051
```

### Run Worker (As many as you want)

```
$ ./node/build/install/node/bin/kGenProg-worker \
  --host <Coordinator's Host> --port 50051
```

## Run Client with kGenProg

```
$ ./node/build/install/node/bin/kGenProg-client \
  --host <Coordinator's Host> --port 50051 \
  --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
```

---

## How to use with Kubernetes
Please setup Kubernetes (with [kubeadm](https://kubernetes.io/docs/setup/independent/create-cluster-kubeadm/)).

### Run Coordinator and Worker

```sh
# in Master Node of k8s
$ git clone git@github.com:kusumotolab/clustered-kGenProg.git
$ cd clustered-kGenProg
$ kubectl apply --record -f kubernetes/deploy.yml
 deployment.apps/c-kgp-coordinator created
 service/c-kgp-externalip created
 deployment.apps/c-kgp-workers created
```

### Run Client with kGenProg

```sh
# in your PC
$ ./node/build/install/node/bin/kGenProg-client \
    --host <Master Node's Host> --port 30080 \
    --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
```
