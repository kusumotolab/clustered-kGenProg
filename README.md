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

---

## How to run on Kubernetes?

### 起動

1. master ノード（`k8s-master`）にログイン
    - `ssh yourname@172.17.100.14`
    - 初回アクセス時の設定方法等は [sdllog を参照](https://github.com/kusumotolab/sdllog/blob/master/logs/20190125-enpit-iaas-k8s.md#%E5%88%9D%E6%9C%9F%E3%83%A6%E3%83%BC%E3%82%B6%E5%90%8D%E3%83%91%E3%82%B9%E3%83%AF%E3%83%BC%E3%83%89%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6)
1. 本リポジトリを `k8s-master` 上に `git clone`
1. [`kubernetes/deploy.yml`](https://github.com/kusumotolab/clustered-kGenProg/blob/master/kubernetes/deploy.yml) を適用
    ```sh
    $ kubectl apply --record -f /path/to/deploy.yml
    deployment.apps/c-kgp-coordinator created
    service/c-kgp-externalip created
    deployment.apps/c-kgp-workers created
    ```
1. 各 Pod がすべて正しく起動していることを確認
    ```sh
    # STATUS が Running になっていることを確認
    $ kubectl get pods
    NAME                                 READY   STATUS    RESTARTS   AGE
    c-kgp-coordinator-7945594777-lw6vz   1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-b7sjx        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-jlqb7        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-ktb7p        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-mq958        1/1     Running   1          43s
    c-kgp-workers-f8d786bb4-pqplr        1/1     Running   1          43s
    c-kgp-workers-f8d786bb4-pwdj5        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-pxgld        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-tsdz5        1/1     Running   0          43s
    c-kgp-workers-f8d786bb4-w4rfk        1/1     Running   1          43s
    ```
1. クライアントを起動
    - ローカルマシンから k8s 上の coordinator にアクセス
        - 研究室内のネットワークから実行すること
    - `k8s-master` が起動しているノードをホストに指定
    - `deploy.yml` の `metadata.name = c-kgp-coordinator-nodeport` の `spec.ports[].nodePort` の値をポートに指定
    ```sh
    # host, port はコンテキストによって変わるため要確認
    $ ./node/build/install/node/bin/kGenProg-client \
        --host 172.17.100.14 --port 30080 \
        --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
    ```

## 終了

※起動中はリソースを喰うので，忘れずに終了しましょう

1. master ノード（`k8s-master`）にログイン
1. `deploy.yml` を指定して削除
    ```sh
    $ kubectl delete -f /path/to/deploy.yml
    deployment.apps "c-kgp-coordinator" deleted
    service "c-kgp-externalip" deleted
    deployment.apps "c-kgp-workers" deleted
    ```
