package jp.sagalab.jftk.curve.interporation;

import java.util.Arrays;
import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.fuzzysplinecurve.FuzzySplineCurveCreater;

/**
 * スプライン曲線補間を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class SplineCurveInterpolator {
	
	/**
	 * 指定された点列に対してスプライン曲線補間を行います。
	 * @param _points 点列
	 * @param _degree 次数
	 * @param _knotInterval 節点間隔
	 * @return スプライン曲線
	 * @throws IllegalArgumentException 次数が0以下の場合
	 * @throws IllegalArgumentException 節点間隔が0以下の場合
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 点列の要素数が1以下の場合
	 * @throws IllegalArgumentException 点列中の時刻がNaN、もしくは無限大の場合
	 * @throws IllegalArgumentException 点列中に時間的に逆行している箇所があった場合
	 */
	public static SplineCurve interpolate( Point[] _points, int _degree, double _knotInterval ) {
		// 次数のチェック
		if ( _degree < 1 ) {
			throw new IllegalArgumentException(" degree is must be greater than 0 ");
		}
		// 節点間隔チェック
		if ( _knotInterval <= 0.0 ) {
			throw new IllegalArgumentException(" knot's interval is must be greater than 0 ");
		}
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null." );
		}
		// 入力点列にnullが混入していないかチェック
		if ( Arrays.asList( _points ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException(" points include null ");
		}
		// 点列の要素数チェック
		if( _points.length < 2 ){
			throw new IllegalArgumentException(" points's length must be greater than 1 ");
		}
		
		// 時系列チェック
		double preTime = Double.NEGATIVE_INFINITY;
		boolean isFuzzy = false;
		for ( Point p : _points ) {
			double t = p.time();
			if ( Double.isNaN( t ) || Double.isInfinite( t ) ) {
				throw new IllegalArgumentException("point's time include NaN or infinite");
			}
			if ( t <= preTime ) {
				throw new IllegalArgumentException("time series is not a positive order");
			}
			if ( !isFuzzy ) {
				isFuzzy = ( p.fuzziness() > 0.0 );
			}
		}

		Range range = Range.create( _points[0].time(), _points[_points.length - 1].time() );

		// 節点系列の生成
		double[] knots = createKnots( range, _degree, _knotInterval );

		// 重み行列の生成
		Matrix wmat = createWeightMatrix( _points, _degree, knots );

		// 制御点列の導出
		Point[] controlPoints = calculateControlPoints( wmat, _points );
		
		//点列中にファジィ点が含まれていた場合はファジィスプライン曲線補間を行う
		if ( isFuzzy ) {
			double[] observations = new double[ _points.length ];
			for ( int i = 0; i < observations.length; ++i ) {
				observations[i] = _points[i].fuzziness();
			}
			double[] fuzzinessElements = FuzzySplineCurveCreater.nnls( wmat, observations );
			for ( int i = 0; i < controlPoints.length; ++i ) {
				controlPoints[i] = Point.createXYZTF( controlPoints[i].x(), controlPoints[i].y(), controlPoints[i].z(),
					controlPoints[i].time(), fuzzinessElements[i] );
			}
		}

		// スプライン曲線構築
		return SplineCurve.create( _degree, controlPoints, knots, range );
	}

	/**
	 * 節点系列を生成します。
	 * @param _range 存在範囲
	 * @param _degree 次数
	 * @param _knotInterval 節点間隔
	 * @return 節点系列
	 */
	private static double[] createKnots( Range _range, int _degree, double _knotInterval ) {
		// 節点系列の生成
		double start = _range.start();
		double end = _range.end();
		// 有効定義域の節点区間数
		int knotIntervalNum = (int) Math.ceil( ( end - start ) / _knotInterval );
		double[] knots = new double[ knotIntervalNum + 2 * _degree - 1 ];

		for ( int i = 0; i < knots.length; ++i ) {
			double w = ( i - _degree + 1 ) / (double) knotIntervalNum;
			knots[i] = ( 1.0 - w ) * start + w * end;
		}

		return knots;
	}

	/**
	 * スプライン曲線の重み行列を生成します。<br>
	 * 生成する行列は行数：入力点数、列数：制御点数となります。
	 * @param _points 入力点列
	 * @param _degree 次数
	 * @param _knots 節点系列
	 * @return 重み行列
	 */
	public static Matrix createWeightMatrix( Point[] _points, int _degree, double[] _knots ) {
		// 生成する行列は行数：入力点数、列数：制御点数
		final int pointsNum = _points.length;
		double[][] elements = new double[ pointsNum ][];

		// 各入力点の時刻での重み列を導出し、重み行列として構成する
		for ( int i = 0; i < pointsNum; ++i ) {
			// ある時刻における重み列（各制御点に対応する重みの列）の導出
			elements[i] = calculateWeights( _knots, _degree, _points[i].time() );
		}

		return Matrix.create( elements );
	}

	/**
	 * ある時刻における重み列を導出します。
	 * @param _knots 節点系列
	 * @param _time 時刻
	 * @return 重み列
	 */
	private static double[] calculateWeights( double[] _knots, int _degree, double _time ) {
		// 時刻に対応する節点番号( _knots[ num ] <= _time <= _knots[ num + 1 ] )の取得
		int num = _degree;
		int end = _knots.length - _degree;
		while ( num < end && _time > _knots[num] ) {
			++num;
		}

		double[] part = new double[]{ 1.0 };

		for ( int i = 1; i <= _degree; ++i ) {
			double[] now = new double[ i + 1 ];
			for ( int j = 0; j <= i; ++j ) {
				double tmp = 0;
				int base = num + j - 1;
				if ( j != 0 ) {
					final double d = _knots[base - i];
					tmp += ( _time - d ) * part[j - 1] / ( _knots[base] - d );
				}
				if ( j != i ) {
					final double d = _knots[base + 1];
					tmp += ( d - _time ) * part[j] / ( d - _knots[base + 1 - i] );
				}
				now[j] = tmp;
			}
			part = now;
		}

		double[] weights = new double[ _knots.length - _degree + 1 ];
		System.arraycopy( part, 0, weights, num - _degree, _degree + 1 );
		return weights;
	}

	/**
	 * 制御点列を導出します。
	 * @param _mat 重み行列
	 * @param _points 通過点列
	 * @return 制御点列
	 */
	private static Point[] calculateControlPoints( Matrix _mat, Point[] _points ) {
		// NtN * d = NtP
		// Nは重み行列、NtはNの転置行列、dは制御点、Pは通過点
		
		Matrix Nt = _mat.transpose();
		Matrix NtN = Nt.product( _mat );
		double[][] elements = new double[ _points.length ][];
		for ( int i = 0; i < _points.length; ++i ) {
			Point p = _points[i];
			elements[i] = new double[]{ p.x(), p.y(), p.z() };
		}
		Matrix NtP = Nt.product( Matrix.create( elements ) );

		Matrix result = NtN.solve( NtP );

		// 制御点列の構成
		Point[] controlPoints = new Point[ result.rowSize() ];
		for ( int i = 0; i < controlPoints.length; ++i ) {
			controlPoints[i] = Point.createXYZ( result.get( i, 0 ), result.get( i, 1 ), result.get( i, 2 ) );
		}

		return controlPoints;
	}
	
	private SplineCurveInterpolator(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}