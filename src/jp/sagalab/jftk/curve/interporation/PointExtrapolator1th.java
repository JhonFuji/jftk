package jp.sagalab.jftk.curve.interporation;

import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.BezierCurve;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;

/**
 * 点列の１次外挿を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class PointExtrapolator1th {

	/**
	 * 指定された点列の1次外挿を行います。
	 * @param _points 点列
	 * @param _length 外挿時間長
	 * @param _interval 外挿時間間隔
	 * @return 外挿後の点列
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列の要素数が0以下の場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 外挿時間長、もしくは外挿時間間隔がNaNの場合
	 * @throws IllegalArgumentException 外挿時間長、もしくは外挿時間間隔が0以下の場合
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
		
		// 点列の先頭付近で1次ベジェ曲線補間
		final int num = (int) Math.floor( _length / _interval );
		final double start = _points[ 0 ].time();
		Point[] points = extractPartPoint( _points, Range.create( start, start + 0.1 ) );
		
		BezierCurve guideCurve = BezierCurveInterpolator.interpolate( points, 1 );
		guideCurve = BezierCurve.create(guideCurve.controlPoints(), Range.zeroToOne() );
		Point[] prePoints = new Point[ num ];
		for ( int i = 0; i < num; ++i ) {
			prePoints[ i ] = guideCurve.evaluateOuter( i / ( num - 1.0 ) - 1 );
		}
		prePoints = timeRemap( prePoints, Range.create( start - _length, start ) );

		// 点列の末尾付近で1次ベジェ曲線補間
		final double end = _points[_points.length - 1].time();
		points = extractPartPoint( _points, Range.create( end - 0.1, end ) );

		guideCurve = BezierCurveInterpolator.interpolate( points, 1 );
		guideCurve = BezierCurve.create(guideCurve.controlPoints(), Range.zeroToOne() );
		Point[] postPoints = new Point[ num ];
		for ( int i = 0; i < num; ++i ) {
			postPoints[ i ] = guideCurve.evaluateOuter( i / ( num - 1.0 ) + 1 );
		}
		postPoints = timeRemap( postPoints, Range.create( end, end + _length ) );

		// 統合
		points = new Point[ _points.length + prePoints.length + postPoints.length ];
		System.arraycopy( prePoints, 0, points, 0, prePoints.length );
		System.arraycopy( _points, 0, points, prePoints.length, _points.length );
		System.arraycopy( postPoints, 0, points, prePoints.length + _points.length, postPoints.length );

		return points;
	}

	/**
	 * 指定された点列の部分点列を抽出します。
	 * @param _points 点列
	 * @param _range 時刻の範囲
	 * @return 部分点列
	 */
	static Point[] extractPartPoint( Point[] _points, Range _range ) {
		List<Point> points = new ArrayList<Point>();

		for ( int i = 0; i < _points.length; ++i ) {
			if ( _range.isInner( _points[i].time() ) ) {
				points.add( _points[i] );
			}
		}

		return points.toArray( new Point[ points.size() ] );
	}

	/**
	 * 指定された点列の時間の再割り当てを行います。
	 * @param _points 点列
	 * @param _range 時刻の範囲
	 * @return 時間の再割り当て後の点列
	 */
	static Point[] timeRemap( Point[] _points, Range _range ) {
		Point[] points = new Point[ _points.length ];

		double start = _points[ 0 ].time();
		double end = _points[ _points.length - 1 ].time();
		double length = end - start;

		for ( int i = 0; i < _points.length; ++i ) {
			Point p = _points[ i ];
			double w = ( p.time() - start ) / length;
			points[ i ] = Point.createXYZT( p.x(), p.y(), p.z(), ( 1 - w ) * _range.start() + w * _range.end() );
		}

		return points;
	}
	
	private PointExtrapolator1th(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}