package jp.sagalab.jftk.curve.interporation;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.BezierCurve;

/**
 * 点列の２次外挿を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class PointExtrapolator2th {

	/**
	 * 指定された点列の2次外挿を行います。
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

		// 点列の先頭付近で2次ベジェ曲線補間
		final int num = (int) Math.ceil( _length / _interval );
		final double start = _points[ 0 ].time();
		// 開始時刻から0.1秒進んだ時刻を含まない範囲内の点列を抽出
		Point[] points = PointExtrapolator1th.extractPartPoint( _points, Range.create( start, start + 0.1 - Math.ulp( start + 0.1 ) ) );

		//2点以下の場合、2次ベジェ曲線補間で生成できる点列へ変換
		if ( points.length <= 2 ) {
			Point[] tmp = new Point[ 3 ];
			System.arraycopy( points, 0, tmp, 0, points.length );
			Point p = points[ points.length - 1 ];
			for ( int i = points.length; i < tmp.length; ++i ) {
				tmp[ i ] = Point.createXYZT( p.x(), p.y(), p.z(), start + 0.1 * i / ( tmp.length - 1 ) );
			}
			points = tmp;
		}

		BezierCurve guideCurve = BezierCurveInterpolator.interpolate( points, 2 );
		guideCurve = BezierCurve.create(guideCurve.controlPoints(), Range.zeroToOne() );
		Point[] prePoints = new Point[ num ];
		for ( int i = 0; i < num; ++i ) {
			prePoints[ i ] = guideCurve.evaluateOuter( i / ( num - 1.0 ) - 1 );
		}
		prePoints = PointExtrapolator1th.timeRemap( prePoints, Range.create( start - _length, start ) );

		// 点列の末尾付近で2次ベジェ曲線補間
		final double end = _points[ _points.length - 1 ].time();
		// 終了時刻を含まない範囲内の点列を抽出 
		points = PointExtrapolator1th.extractPartPoint( _points, Range.create( end - 0.1 + Math.ulp( end - 0.1 ), end ) );

		//2点以下の場合、2次ベジェ曲線補間で生成できる点列へ変換
		if ( points.length <= 2 ) {
			Point[] tmp = new Point[ 3 ];
			System.arraycopy( points, 0, tmp, tmp.length - points.length, points.length );
			Point p = points[ 0 ];
			for ( int i = 0; i < tmp.length - points.length; ++i ) {
				tmp[ i ] = Point.createXYZT( p.x(), p.y(), p.z(), end - 0.1 * ( tmp.length - 1.0 - i ) / ( tmp.length - 1.0 ) );
			}
			points = tmp;
		}

		guideCurve = BezierCurveInterpolator.interpolate( points, 2 );
		guideCurve = BezierCurve.create( guideCurve.controlPoints(), Range.zeroToOne() );
		Point[] postPoints = new Point[ num ];
		for ( int i = 0; i < num; ++i ) {
			postPoints[ i ] = guideCurve.evaluateOuter( i / ( num - 1.0 ) + 1 );
		}
		postPoints = PointExtrapolator1th.timeRemap( postPoints, Range.create( end, end + _length ) );

		// 統合
		points = new Point[ _points.length + prePoints.length + postPoints.length ];
		System.arraycopy( prePoints, 0, points, 0, prePoints.length );
		System.arraycopy( _points, 0, points, prePoints.length, _points.length );
		System.arraycopy( postPoints, 0, points, prePoints.length + _points.length, postPoints.length );

		return points;
	}
	
	private PointExtrapolator2th(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}