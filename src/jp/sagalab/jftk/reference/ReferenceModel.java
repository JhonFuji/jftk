package jp.sagalab.jftk.reference;

import java.util.Arrays;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.Transformable;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.ParametricEvaluable;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;

/**
 * レファレンスモデルを表すクラスです。
 * <p>
 * レファレンスモデルは描き手が線形、円形、楕円形の曲線を描画したと仮定した
 * 場合の仮説ファジィ曲線モデルを持ちます。<br>
 * </p>
 * <p>
 * 仮説ファジィ曲線モデルは一つでも幾何曲線認識を行うことができますが、異なる生成方法
 * で複数の仮説ファジィ曲線モデルを用意した方が、より描き手の意図を反映した認識結果を
 * 得ることができます。<br>
 * </p>
 * <p>
 * 仮説ファジィ曲線モデルは二次有理ベジェ曲線({@link QuadraticBezierCurve})により表現されます。
 * </p>
 * @author Akira Nishikawa
 * @see ReferenceModelGenerator
 */
public abstract class ReferenceModel implements
	ParametricEvaluable<Point>, Transformable<ReferenceModel>{

	/**
	 * コンストラクタ。
	 * @param _curve 二次有理ベジェ曲線列
	 * @exception IllegalArgumentException 曲線列の要素にNullが存在する場合
	 */
	protected ReferenceModel( QuadraticBezierCurve _curve ) {
		if ( Arrays.asList( _curve ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_curves include null." );
		}
		m_curve = _curve;
	}

	@Override
	public Point evaluateAt( double _parameter ) {
		return getCurve().evaluateAt( _parameter );
	}

	@Override
	public Point[] evaluateAll( int _num, EvaluationType _type ) {
		// 評価点数チェック
		if ( _num < 2 ) {
			throw new OutOfRangeException(
				String.format( "_num:%d must be greater than 1", _num ) );
		}

		Point[] points;

		switch ( _type ) {
			case TIME:
				points = evaluateAllByTime( _num );
				break;
			case DISTANCE:
				points = evaluateAllByDistance( _num );
				break;
			default:
				throw new UnsupportedOperationException();
		}

		return points;
	}

	@Override
	public Point evaluateAtStart() {
		return getCurve().evaluateAtStart();
	}

	@Override
	public Point evaluateAtEnd() {
		return getCurve().evaluateAtEnd();
	}

	@Override
	public Range range() {
		return getCurve().range();
	}

	/**
	 * 指定した番号の仮説ファジィ曲線モデルを返します。
	 * @return 仮説ファジィ曲線モデル
	 */
	public final QuadraticBezierCurve getCurve() {
		return m_curve;
	}

	/**
	 * 仮説ファジィ曲線モデルが指定された曲線に含まれているかを評価します。
	 * @param _other 他方の曲線
	 * @param _num 評価点数
	 * @return 区間真理値
	 */
	public TruthValue includedIn( ParametricCurve _other, int _num ) {
		Point[] points = evaluateAll( _num, ParametricCurve.EvaluationType.DISTANCE );
		Point[] otherPoints = _other.evaluateAll( _num, ParametricCurve.EvaluationType.DISTANCE );

		double nec = 1;
		double pos = 1;
		for ( int i = 0; i < _num; ++i ) {
			TruthValue tv = points[i].includedIn( otherPoints[i] );
			nec = Math.min( nec, tv.necessity() );
			pos = Math.min( pos, tv.possibility() );
		}

		return TruthValue.create( nec, pos );
	}

	/**
	 * 制御点を返します。
	 * @return 制御点列
	 */
	public Point[] controlPoint() {
		return m_curve.controlPoints();
	}

	/**
	 * この ReferenceModel と指定された Object が等しいかどうかを比較します。
	 * @param obj この ReferenceModel と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ファジィ仮説曲線モデル群、その数がまったく同じ ReferenceModel である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( !( obj instanceof ReferenceModel ) ) {
			return false;
		}
		final ReferenceModel other = (ReferenceModel) obj;
		return other.getCurve().equals( getCurve() );
	}

	/**
	 * この ReferenceModel のハッシュコードを返します。
	 * @return この ReferenceModel のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + ( this.m_curve != null ? this.m_curve.hashCode() : 0 );
		return hash;
	}

	/**
	 * この ReferenceModel の文字列表現を返します。
	 * @return ファジィ仮説曲線モデル群、その数を表す文字列
	 */
	@Override
	public String toString() {
		return String.format( "curve:%s", m_curve.toString() );
	}

	/**
	 * 等時間間隔で評価点列のマルチファジィ点列を生成します。
	 * @param _num 評価点数
	 * @return 評価点列
	 */
	protected Point[] evaluateAllByTime( int _num ) {
		return m_curve.evaluateAll( _num, EvaluationType.TIME );
	}

	/**
	 * 等距離間隔で評価点列のマルチファジィ点列を生成します。
	 * @param _num 評価点数
	 * @return 評価点列
	 */
	protected Point[] evaluateAllByDistance( int _num ) {
		return m_curve.evaluateAll( _num, EvaluationType.DISTANCE );
	}

	/** レファレンスモデルを構成する曲線群(ファジィ仮説曲線モデル群) */
	private final QuadraticBezierCurve m_curve;
}
