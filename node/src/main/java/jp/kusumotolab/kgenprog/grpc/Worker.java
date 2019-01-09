package jp.kusumotolab.kgenprog.grpc;

import io.reactivex.Single;

/**
 * クラスタでのワーカー．コーディネータからの依頼を受け，テストを実行する．
 * 
 * @author Ryo Arima
 *
 */
public interface Worker {

  /**
   * テストを実行しその結果を返す
   * 
   * @param request リクエスト
   * @return レスポンス
   */
  Single<GrpcExecuteTestResponse> executeTest(GrpcExecuteTestRequest request);

  /**
   * ワーカーからプロジェクトの登録を解除する
   * 
   * @param request リクエスト
   * @return レスポンス
   */
  Single<GrpcUnregisterProjectResponse> unregisterProject(GrpcUnregisterProjectRequest request);
}
