package jp.sagalab.jftk.fragmentation;

import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.transform.Transformable;
import jp.sagalab.jftk.curve.Invertible;

/**
 * フラグメントを定義する抽象クラスです。
 * <p>
 * フラグメントはファジィフラグメンテーション法によって分割された単位を表現します。
 * </p>
 * @author yamaguchi
 */
public abstract class Fragment implements Transformable<Fragment>, Invertible<Fragment>{
	
	@Override
	abstract public Fragment invert();

	@Override
	abstract public Fragment transform( TransformMatrix _mat );
	
	/**
	 * このフラグメントを構成するファジィスプライン曲線を返します。
	 * @return ファジィスプライン曲線
	 */
	public SplineCurve curve(){
		return m_fragment;
	}
	
	/**
	 * この Fragment と指定された Object が等しいかどうかを比較します。 
	 * @param obj この Fragment と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ファジィスプライン曲線がまったく同じ Fragment である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Fragment other = (Fragment) obj;

		return m_fragment.equals( other.m_fragment );
	}

	/**
	 * この Fragment のハッシュコードを返します。 
	 * @return この Fragment のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 29 * hash + ( this.m_fragment != null ? this.m_fragment.hashCode() : 0 );
		return hash;
	}
	
	/**
	 * この Fragment の文字列表現を返します。
	 * @return ファジィスプライン曲線を表す String
	 */
	@Override
	public String toString() {
		return String.format( "fragment curve:%s",m_fragment.toString() );
	}
	
	/**
	 * フラグメントを生成
	 * @param _fragment ファジィスプライン曲線
	 */
	protected Fragment(SplineCurve _fragment){
		m_fragment = _fragment;
	}
	
	/** このフラグメントを構成するファジィスプライン曲線 */
	private final SplineCurve m_fragment;
}