package jp.sagalab.jftk.fragmentation;

import jp.sagalab.jftk.curve.SplineCurve;

/**
 * ファジィフラグメンテーションを行うためのインタフェースです。
 * <p>
 * ファジィフラグメンテーション法はファジィスプライン曲線を同定単位と区切り単位に分割する手法です。
 * </p>
 * @author aburaya
 */
public interface FuzzyFragmentation {
	
	/** フラグメントの状態を表す識別子です。 */
	static enum State {
		/** 移動 */
		MOVE,
		/** 停止 */
		STAY,
		/** 不明 */
		UNKNOWN
	}
	
	/**
	 * 指定されたファジィスプライン曲線を元にフラグメント列を生成します。
	 * @param _splineCurve ファジィスプライン曲線
	 * @return フラグメント列
	 */
	public Fragment[] createFragment( SplineCurve _splineCurve );
	//TODO 処理に足を切除する処理を最後に同一に行っている。これをinterfaceで実装したいが、java8になるまで保留
	
	/**
	 * 指定されたファジィスプライン曲線を元に分割したファジィスプライン曲線列を生成します。
	 * @param _splineCurve ファジィスプライン曲線
	 * @return 分割されたファジィスプライン曲線
	 */
	public SplineCurve[] divide( SplineCurve _splineCurve );
}
