package jp.sagalab.jftk.curve.interporation;

import java.util.Arrays;
import jp.sagalab.jftk.Point;

/**
 * 点列の0次外挿を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class PointExtrapolator0th {

	/**
	 * 指定された点列の０次外挿を行います。
	 * @param _points 点列
	 * @param _length 外挿時間長
	 * @param _interval 外挿時間間隔
	 * @return 外挿後の点列
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列の要素数が0以下の場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 外挿時間長、もしくは外挿時間間隔がNaNの場合
	 * @throws IllegalArgumentException 外挿時間長、もしくは外挿時間間隔が0以下の場合
	 * 
	 */
	public static Point[] extrapolate( Point[] _points, double _length, double _interval ) {
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null." );
		}
		if(_points.length == 0){
			throw new IllegalArgumentException( " _points's length must be greater than 0" );
		}
		if ( Arrays.asList( _points ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException(" _points include null ");
		}
		if( Double.isNaN(_length) || Double.isNaN(_interval) ){
			throw new IllegalArgumentException(" _length or _interval is NaN ");
		}
		if( _length <= 0 || _interval <= 0 ) {
			throw new IllegalArgumentException(" _length and _interval must be greater than 0 ");
		}
		
		// 最初の点
		Point p = _points[ 0 ];
		double[] times = createExtrapolationTimes( _points[ 0 ].time(), _length, -_interval );
		Point[] prePoints = createExtraPoints( p, times );

		// 最後の点
		p = _points[ _points.length - 1 ];
		times = createExtrapolationTimes( _points[ _points.length - 1 ].time(), _length, _interval );
		Point[] postPoints = createExtraPoints( p, times );

		// 統合
		Point[] points = new Point[ _points.length + prePoints.length + postPoints.length ];
		System.arraycopy( prePoints, 0, points, 0, prePoints.length );
		System.arraycopy( _points, 0, points, prePoints.length, _points.length );
		System.arraycopy( postPoints, 0, points, prePoints.length + _points.length, postPoints.length );

		return points;
	}

	/**
	 * 外挿時間列を生成します。
	 * @param _start 開始時刻
	 * @param _length 外挿時間長
	 * @param _step 外挿時間間隔
	 * @return 外挿時間列
	 */
	private static double[] createExtrapolationTimes( double _start, double _length, double _step ) {
		int num = (int) Math.floor( _length / Math.abs( _step ) );
		double[] times = new double[ num ];

		for ( int i = 0; i < num; ++i ) {
			times[i] = _start + _step * ( i + 1 );
		}

		return times;
	}

	/**
	 * 外挿点列を生成します。
	 * @param _p 開始点
	 * @param _times 外挿時間列
	 * @return 外挿点列
	 */
	private static Point[] createExtraPoints( Point _p, double[] _times ) {
		Point[] points = new Point[ _times.length ];

		for ( int i = 0; i < _times.length; ++i ) {
			points[i] = Point.createXYZT( _p.x(), _p.y(), _p.z(), _times[i] );
		}

		return points;
	}
	
	private PointExtrapolator0th(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}