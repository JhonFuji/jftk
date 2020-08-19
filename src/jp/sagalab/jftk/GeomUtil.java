package jp.sagalab.jftk;

/**
 * 幾何学的な計算を行うユーティリティクラスです。
 * @author akira
 */
public final class GeomUtil {
	
	/**
	 * 点と線分の距離を求めます。
	 * @param _start 線分の始点
	 * @param _end 線分の終点
	 * @param _p 点
	 * @return 点と線分の距離
	 */
	public static double distance( Point _start, Point _end, Point _p ) {
		Vector v = Vector.createSE( _start, _p );
		Vector direction = Vector.createSE( _start, _end );
		double t = v.dot( direction );
		double distance;
		if ( t <= 0 ) {
			distance = _start.distance( _p );
		} else {
			double u = direction.square();
			if ( t >= u ) {
				distance = _end.distance( _p );
			} else {
				distance = Math.sqrt( v.square() - t * t / u );
			}
		}
		return distance;
	}
	
	/**
	 * 点と直線の距離を求めます。
	 * <p>
	 * 直線と点の距離の求め方は「ゲームプログラミングのための3Dグラフィックス数学」参照。
	 * </p>
	 * @param _base 直線の基点
	 * @param _direction 直線の方向ベクトル
	 * @param _p 点
	 * @return 点と直線の距離
	 * @see <span>「ゲームプログラミングのための3Dグラフィックス数学」</span>
	 */
	public static double distanceWithPointAndLine( Point _base, Vector _direction, Point _p ) {
		Vector v = Vector.createSE( _base, _p );
		double t = v.dot( _direction );
		double inv = 1.0 / _direction.square();
		return Math.sqrt( v.square() - t * t * inv );
	}
	
	/**
	 * 指定された点列の時間軸の拡大縮小を行います。
	 * @param _points 点列
	 * @param _scaleRatio 拡大縮小率
	 * @return 時間軸の拡大縮小後の点列
	 */
	public static Point[] timeScaling( Point[] _points, double _scaleRatio ) {
		Point[] points = new Point[ _points.length ];
		double startTime = _points[0].time();
		for ( int i = 0; i < _points.length; ++i ) {
			Point p = _points[i];
			double time = ( p.time() - startTime ) * _scaleRatio + startTime;
			points[i] = Point.createXYZTF( p.x(), p.y(), p.z(), time, p.fuzziness() );
		}
		return points;
	}
	
	private GeomUtil(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}
