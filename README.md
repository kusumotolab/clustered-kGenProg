[![CircleCI](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master.svg?style=svg&circle-token=7de79fe88bdd8eff9a276a35b460d988cc7a6100)](https://circleci.com/gh/kusumotolab/clustered-kGenProg/tree/master)

# clustered-kGenProg

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
    ...
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


### 終了

※起動中はリソースを喰うので，忘れずに終了しましょう．

1. master ノード（`k8s-master`）にログイン
1. `deploy.yml` を指定して削除
    ```sh
    $ kubectl delete -f /path/to/deploy.yml
    deployment.apps "c-kgp-coordinator" deleted
    service "c-kgp-externalip" deleted
    deployment.apps "c-kgp-workers" deleted
    ```


### TIPS: Namespace 切り替え

k8s には **Namespace** という概念があります．  
自分用の Namespace を設定することで他人に勝手にサービスを止めらる危険性を減らせます．  
また，同様の構成を持つサービスを複数同時に起動したい際にも有効です．

なお，デフォルトでは `default` という Namespace を操作します．

1. Namespace 作成
    ```sh
    # <NAMESPACE> にユニークな名前を登録
    $ kubectl kubectl create namespace <NAMESPACE>
    namespace/my-cluster created
    ```
1. Namespace が作成されたことを確認
    ```sh
    $ kubectl get namespaces
    NAME          STATUS   AGE
    default       Active   5d
    kube-public   Active   5d
    kube-system   Active   5d
    <NAMESPACE>   Active   6m2s
    # ※ default, kube-{public,system} はデフォルトで作成されます
    ```
1. Namespace を指定してコマンドを実行  
    `kubectl get`, `apply`, `delete` 等のコマンドは Namespace を指定して実行することができます：
    ```sh
    $ kubectl --namespace <NAMESPACE> apply ...
    ```
    毎回指定するのが面倒であれば，操作する Namespace を永続的に変更することも可能です：
    ```sh
    $ kubectl config set-context $(kubectl config current-context) --namespace <NAMESPACE>
    ```


#### 注： NodePort サービスのポート番号に注意

Namespace によって仮想的に環境が分離されるため，通常は Coordinator や Worker のポート番号を変更する必要はありません．  
しかし，NodePort は k8s 環境の外からアクセスしてくる Client 用のポート番号を開けるための特別なサービスであり，これだけはどうしても Namespace を超えて衝突していまします．

複数の c-KGP を Namespace を分けて立ち上げるような使い方をする際は，以下のように `nodePort` をユニークにしてください：

```diff
diff --git a/kubernetes/deploy.yml b/kubernetes/deploy.yml
index 28d0f99..ccec30a 100644
--- a/kubernetes/deploy.yml
+++ b/kubernetes/deploy.yml
@@ -43,7 +43,7 @@ spec:
       protocol: "TCP"
       port: 50051
       targetPort: 50051
-      nodePort: 30080
+      nodePort: 30081
   selector:
     app: c-kgp-coordinator
```

Client のオプションももちろん変わります：

```sh
# host, port はコンテキストによって変わるため要確認
$ ./node/build/install/node/bin/kGenProg-client \
    --host 172.17.100.14 --port 30081 \
    --kgp-args '--config main/example/CloseToZero01/kgenprog.toml'
```
