package jp.sagalab.jftk.reference;

import jp.sagalab.jftk.curve.ParametricCurve;

/**
 * リファレンスモデルを生成するためのインタフェースです。
 * @author oyoshi
 */
public interface ReferenceModelGenerator {

	/**
	 * 一般形状のリファレンスモデルを生成します。
	 * @param _curve パラメトリック曲線
	 * @return 一般形状のリファレンスモデル
	 */
	ReferenceModel generateGeneralModel( ParametricCurve _curve );

	/**
	 * 1/4形状のリファレンスモデルを生成します。
	 * @param _curve パラメトリック曲線
	 * @return 1/4形状のリファレンスモデル
	 */
	ReferenceModel generateQuarterModel( ParametricCurve _curve );

	/**
	 * 2/4形状のリファレンスモデルを生成します。
	 * @param _curve パラメトリック曲線
	 * @return 2/4形状のリファレンスモデル
	 */
	ReferenceModel generateHalfModel( ParametricCurve _curve );

	/**
	 * 3/4形状のリファレンスモデルを生成します。
	 * @param _curve パラメトリック曲線
	 * @return 3/4形状のリファレンスモデル
	 */
	ReferenceModel generateThreeQuartersModel( ParametricCurve _curve );
}
