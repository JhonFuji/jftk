package jp.sagalab.jftk.curve.interporation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;

/**
 * 点列の０次内挿を行うためのクラスです。
 * @author Akira Nishikawa
 */
public final class PointInterpolator0th {
	
	/**
	 * 指定された点列の0次内挿を行います。
	 * @param _points 点列
	 * @param _maxSpan 内挿時間間隔
	 * @return 内挿後の点列
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列の要素数が0以下の場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 内挿時間間隔がNaNの場合
	 * @throws IllegalArgumentException 内挿時間間隔が0以下の場合
	 * @throws IllegalArgumentException 点列中に時間的に逆行している箇所があった場合
	 */
	public static Point[] interpolate( Point[] _points, double _maxSpan ) {
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null." );
		}
		if(_points.length == 0){
			throw new IllegalArgumentException( " _points's length must be greater than 0" );
		}
		if ( Arrays.asList( _points ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException(" _points include null ");
		}
		if( Double.isNaN( _maxSpan ) ){
			throw new IllegalArgumentException(" _maxSpan is NaN ");
		}
		if( _maxSpan <= 0 ){
			throw new IllegalArgumentException(" _maxSpan must be greater than 0 ");
		}
		
		List<Point> fixedPoints = new ArrayList<Point>( _points.length );

		fixedPoints.add( _points[ 0] );
		Point pre = _points[ 0];

		for ( int i = 1; i < _points.length; ++i ) {
			double span = _points[i].time() - pre.time();
			// 時間的に逆行している箇所があった場合に例外発生
			if ( span < 0 ) {
				throw new IllegalArgumentException("time series is not a positive order");
			}
			// 許容する時間的隙間を上回っている箇所に点を追加
			// 一つ前の点と同じ位置
			while ( span >= _maxSpan ) {
				pre = Point.createXYZT( pre.x(), pre.y(), pre.z(), pre.time() + _maxSpan );
				fixedPoints.add( pre );
				span -= _maxSpan;
			}

			pre = _points[i];
			fixedPoints.add( pre );
		}

		return fixedPoints.toArray( new Point[ fixedPoints.size() ] );
	}
	
	private PointInterpolator0th(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}