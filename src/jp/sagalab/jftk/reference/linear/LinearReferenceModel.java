package jp.sagalab.jftk.reference.linear;

import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.reference.ReferenceModel;

/**
 * 線形レファレンスモデルを表すクラスです。
 * @author Akira Nishikawa
 */
public class LinearReferenceModel extends ReferenceModel{

	/**
	 * 線形レファレンスモデルを生成します。
	 * @param _curve 仮説ファジィ曲線モデル
	 * @return 線形レファレンスモデル
	 * @throws IllegalArgumentException _curvesがnullの場合
	 */
	static LinearReferenceModel create( QuadraticBezierCurve _curve ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}
		return new LinearReferenceModel( _curve );
	}

	@Override
	public LinearReferenceModel transform( TransformMatrix _mat ) {
		QuadraticBezierCurve curve = getCurve();
		QuadraticBezierCurve transformed = curve.transform( _mat );

		return new LinearReferenceModel( transformed );
	}

	@Override
	public Point evaluateAt( double _parameter ) {
		Point[] cp = getCurve().controlPoints();
		Point result = cp[0].internalDivision( cp[2], _parameter, 1 - _parameter );
		result = Point.createXYZTF( result.x(), result.y(), result.z(), _parameter, result.fuzziness() );
		return result;
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

	/**
	 * 等距離間隔で評価点列のマルチファジィ点列を生成します。
	 * @param _num 評価点数
	 * @return 評価点列
	 */
	@Override
	protected Point[] evaluateAllByDistance( int _num ) {
		QuadraticBezierCurve curve = getCurve();
		Range range = curve.range();
		Point start = evaluateAt( range.start() );
		Point end = evaluateAt( range.end() );
		double parameter = range.end() - range.start();
		Point[] points = new Point[_num];
		// 現在の評価点番号
		int n = 1;
		points[0] = start;
		while ( n < _num ) {
			double eNow = n * parameter / ( _num - 1 );
			points[n] = evaluateAt( range.start() + eNow );
			++n;
		}
		points[_num - 1] = end;

		return points;
	}

	/**
	 * 線形レファレンスモデルを生成します。
	 * @param _curve 仮説ファジィ曲線モデル
	 */
	protected LinearReferenceModel( QuadraticBezierCurve _curve ) {
		super( _curve );
	}

}
