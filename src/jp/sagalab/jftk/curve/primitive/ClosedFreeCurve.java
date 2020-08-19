package jp.sagalab.jftk.curve.primitive;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 閉自由曲線を表すクラスです。
 * @author ishiguro
 */
public class ClosedFreeCurve extends OpenFreeCurve{

	/**
	 * 閉自由曲線を生成します。
	 * @param _beziers ベジェ曲線列
	 * @return インスタンス
	 * @throws IllegalArgumentException _beziersが閉じていない場合
	 */
	public static ClosedFreeCurve create( BezierCurve[] _beziers ) {
		BezierCurve[] beziers = _beziers.clone();
		Point[] firstCP = beziers[0].controlPoints();
		Point[] lastCP = beziers[beziers.length - 1].controlPoints();
		if ( !lastCP[lastCP.length - 1].equals( firstCP[0] ) ) {
			throw new IllegalArgumentException( "_beziers is not closed" );
		}

		return new ClosedFreeCurve( _beziers );
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public ClosedFreeCurve transform( TransformMatrix _mat ) {
		BezierCurve[] beziers = new BezierCurve[beziers().length];

		for ( int i = 0; i < beziers.length; ++i ) {
			beziers[i] = beziers()[i].transform( _mat );
		}

		return new ClosedFreeCurve( beziers );
	}

	@Override
	public ClosedFreeCurve invert() {
		BezierCurve[] beziers = new BezierCurve[beziers().length];
		for ( int i = 0; i < beziers.length; ++i ) {
			beziers[beziers.length - i - 1] = beziers()[i].invert();
		}
		return new ClosedFreeCurve( beziers );
	}

	/**
	 * この ClosedFreeCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この ClosedFreeCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ベジェ曲線列、閉じているかがまったく同じ ClosedFreeCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final ClosedFreeCurve other = (ClosedFreeCurve) obj;
		return Arrays.deepEquals( this.beziers(), other.beziers() );
	}

	/**
	 * この ClosedFreeCurve のハッシュコードを返します。
	 * @return この ClosedFreeCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Arrays.deepHashCode( this.beziers() );
		return hash;
	}

	/**
	 * この ClosedFreeCurve の文字列表現を返します。
	 * @return ベジェ曲線列、閉じているか、範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "beziers:%s closed:%s %s", Arrays.toString( beziers() ), false, super.toString() );
	}

	/**
	 * 閉自由曲線を生成します。
	 * @param _beziers ベジェ曲線列
	 */
	private ClosedFreeCurve( BezierCurve[] _beziers ) {
		super( _beziers );
	}

}
