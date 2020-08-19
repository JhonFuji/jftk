package jp.sagalab.jftk.curve.interporation;

import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.BezierCurve;

/**
 * ベジェ曲線補間を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class BezierCurveInterpolator {

	/**
	 * 指定された点列に対してベジェ曲線補間を行います。
	 * @param _points 点列
	 * @param _degree 次数
	 * @return ベジェ曲線
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列数が次数+1より小さい、もしくは次数が0以下の場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 点列中の時刻がNaN、もしくは無限大の場合
	 */
	public static BezierCurve interpolate( Point[] _points, int _degree ) {
		if ( _points == null ) {
			throw new IllegalArgumentException("_points is null");
		}
		if( _points.length < _degree + 1 || _degree < 1 ) {
			throw new IllegalArgumentException("degree is not appropriate. "
				+ "points's length must be grater than degree+1 and degree must be grater than 0");
		}
		for(Point p: _points){
			if( p == null ){
				throw new IllegalArgumentException(" points include null ");
			}
			if( Double.isNaN( p.time() ) || Double.isInfinite( p.time() ) ){
				throw new IllegalArgumentException(" point's time include NaN or infinite ");
			}
		}

		double[] times = createNormalizedTimes( _points );
		// 重み行列の生成
		Matrix wMat = createWeightMatrix( times, _degree );
		// 最小自乗法による制御点列の導出
		Point[] cp = calculateControlPoints( wMat, _points );

		return BezierCurve.create(cp, Range.zeroToOne() );
	}

	/**
	 * 正規化時刻列を生成します。
	 * 開始時刻を0.0、終了時刻を1.0になるように各点列の時刻を正規化します。
	 * @param _points 点列
	 * @return 正規化時刻列
	 */
	private static double[] createNormalizedTimes( Point[] _points ) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for ( Point p : _points ) {
			double t = p.time();
			min = Math.min( min, t );
			max = Math.max( max, t );
		}
		double length = max - min;

		double[] times = new double[ _points.length ];
		for ( int i = 0; i < _points.length; ++i ) {
			times[i] = ( _points[i].time() - min ) / length;
		}

		return times;
	}

	/**
	 * 重み行列を生成します。
	 * @param _times 正規化された時刻列
	 * @param _degree 次数
	 * @return 重み行列
	 */
	private static Matrix createWeightMatrix( double[] _times, int _degree ) {
		double[][] elements = new double[ _times.length ][];

		for ( int i = 0; i < _times.length; ++i ) {
			elements[i] = calculateWeights( _times[i], _degree );
		}

		return Matrix.create( elements );
	}

	/**
	 * 指定したパラメータに対する重み列を導出します。
	 * @param _t パラメータ
	 * @param _degree 次数
	 * @return 重み列
	 */
	private static double[] calculateWeights( double _t, int _degree ) {
		double[] weights = new double[ _degree + 1 ];

		for ( int i = 0; i < _degree + 1; ++i ) {
			double[] tmp = new double[ _degree + 1 ];
			tmp[i] = 1;
			for ( int j = 0; j < _degree; ++j ) {
				for ( int k = 0; k < _degree - j; ++k ) {
					tmp[k] = ( 1 - _t ) * tmp[k] + _t * tmp[k + 1];
				}
			}
			weights[i] = tmp[0];
		}

		return weights;
	}

	/**
	 * 制御点列を導出します。
	 * @param _mat 重み行列
	 * @param _points 点列
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
		int rowSize = result.rowSize();
		Point[] controlPoints = new Point[ rowSize ];
		for ( int i = 0; i < rowSize; ++i ) {
			controlPoints[i] = Point.createXYZ( result.get( i, 0 ), result.get( i, 1 ), result.get( i, 2 ) );
		}

		return controlPoints;
	}
	
	private BezierCurveInterpolator(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}