package jp.sagalab.jftk.curve.primitive;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 開自由曲線を表すクラスです。
 * @author ishiguro
 */
public class OpenFreeCurve extends PrimitiveCurve{

	/**
	 * 開自由曲線を生成します。
	 * @param _beziers ベジェ曲線列
	 * @return インスタンス
	 * @throws IllegalArgumentException _beziersがnullの場合
	 * @throws IllegalArgumentException _beziersにnullが含まれていた場合
	 */
	public static OpenFreeCurve create( BezierCurve[] _beziers ) {
		if ( Arrays.asList( _beziers ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_beziers include null" );
		}

		return new OpenFreeCurve( _beziers );
	}

	@Override
	public Point locus( double _parameter ) throws OutOfRangeException {
		if ( !range().isInner( _parameter ) ) {
			throw new OutOfRangeException( "_parameter is out of range" );
		}
		int n = Math.min( (int) Math.floor( _parameter ), m_beziers.length - 1 );
		Point p = m_beziers[n].evaluateAt( _parameter - n );
		return Point.createXYZT( p.x(), p.y(), p.z(), _parameter );
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public double length() {
		double length = 0;
		for ( BezierCurve bezier : m_beziers ) {
			length += bezier.length();
		}

		return length;
	}

	@Override
	public BezierCurve[] convert() {
		// 区間[ start - end ]を決定し、抽出（さらに最初と最後の区間は分割）
		Range range = range();
		int sNum = Math.min( (int) Math.floor( range.start() ), m_beziers.length - 1 );
		int eNum = Math.min( (int) Math.floor( range.end() ), m_beziers.length - 1 );
		BezierCurve[] beziers = new BezierCurve[eNum - sNum + 1];
		System.arraycopy( m_beziers, sNum, beziers, 0, beziers.length );
		beziers[0] = beziers[0].divide( range.start() - sNum )[1];
		beziers[beziers.length - 1] = beziers[beziers.length - 1].divide(
			sNum < eNum ? range.end() - eNum : range.length() )[0];

		return beziers;
	}

	@Override
	public OpenFreeCurve transform( TransformMatrix _mat ) {
		BezierCurve[] beziers = new BezierCurve[m_beziers.length];

		for ( int i = 0; i < beziers.length; ++i ) {
			beziers[i] = m_beziers[i].transform( _mat );
		}

		return new OpenFreeCurve( beziers );
	}

	@Override
	public OpenFreeCurve invert() {
		BezierCurve[] beziers = new BezierCurve[m_beziers.length];
		for ( int i = 0; i < beziers.length; ++i ) {
			beziers[beziers.length - i - 1] = m_beziers[i].invert();
		}
		return new OpenFreeCurve( beziers );
	}

	/**
	 * この自由曲線を構成するベジェ曲線列を返します。
	 * @return ベジェ曲線列
	 */
	public BezierCurve[] beziers() {
		return m_beziers.clone();
	}

	/**
	 * この自由曲線を構成するベジェ曲線の次数を返します。
	 * @return 次数
	 */
	public int degree() {
		return m_degree;
	}

	/**
	 * この OpenFreeCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この OpenFreeCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ベジェ曲線列、閉じているかがまったく同じ OpenFreeCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final OpenFreeCurve other = (OpenFreeCurve) obj;
		return Arrays.deepEquals( this.m_beziers, other.m_beziers );
	}

	/**
	 * この OpenFreeCurve のハッシュコードを返します。
	 * @return この OpenFreeCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Arrays.deepHashCode( this.m_beziers );
		return hash;
	}

	/**
	 * この OpenFreeCurve の文字列表現を返します。
	 * @return ベジェ曲線列、閉じているか、範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "beziers:%s closed:%s %s", Arrays.toString( m_beziers ), false, super.toString() );
	}

	/**
	 * 開自由曲線を生成します。
	 * @param _beziers ベジェ曲線列
	 */
	protected OpenFreeCurve( BezierCurve[] _beziers ) {
		super( Range.create( 0, _beziers.length ) );
		m_beziers = _beziers;
		int degree = 0;
		for ( BezierCurve bezier : _beziers ) {
			degree = Math.max( bezier.degree(), degree );
		}
		m_degree = degree;
	}

	/** ベジェ曲線列 */
	private final BezierCurve[] m_beziers;
	/** 次数 */
	private final int m_degree;
}
