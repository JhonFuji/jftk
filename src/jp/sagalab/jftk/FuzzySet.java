package jp.sagalab.jftk;

import jp.sagalab.jftk.convex.Polytope;

/**
 * ファジィ集合を定義する抽象クラスです。
 * @author miwa
 */
public abstract class FuzzySet extends Polytope {

	/**
	 * ファジィ集合の種類を定義する列挙子です。
	 */
	public enum FuzzySetType {
		/** シングル */
		SINGLE,
		/** マルチ */
		MULTI;
	}

	/**
	 * 他方のファジィ集合に含まれているかを評価します。
	 * @param _fuzzySet ファジィ集合
	 * @return 区間真理値
	 */
	public TruthValue includedIn( FuzzySet _fuzzySet ) {
		if(_fuzzySet instanceof Point) {
			return includedIn( (Point)_fuzzySet );
		}
		if ( _fuzzySet instanceof MultiPoint) {
			return includedIn( (MultiPoint)_fuzzySet );
		}
		return null;
	}
	
	/**
	 * ファジィ集合の種類を取得します。
	 * @return ファジィ集合
	 */
	abstract public FuzzySetType getFuzzySetType();
	
	/**
	 * 他方のファジィ点に含まれているかを評価します。
	 * @param _point 他方のファジィ点
	 * @return 区間真理値
	 */
	abstract protected TruthValue includedIn( Point _point );

	/**
	 * 他方のマルチファジィ点に含まれているかを評価します。
	 * @param _multiPoint 他方のマルチファジィ点
	 * @return 区間真理値
	 */
	abstract protected TruthValue includedIn( MultiPoint _multiPoint );

}
