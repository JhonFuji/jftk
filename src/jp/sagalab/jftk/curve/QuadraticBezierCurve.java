package jp.sagalab.jftk.curve;

import java.util.ArrayList;
import java.util.List;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 二次有理ベジェ曲線を表すクラスです。
 * @author miwa
 */
public class QuadraticBezierCurve extends ParametricCurve{

	/**
	 * 二次有理ベジェ曲線を生成します。
	 * @param _cp0 制御点0
	 * @param _cp1 制御点1
	 * @param _cp2 制御点2
	 * @param _weight 重み
	 * @param _range パラメータ範囲
	 * @return 二次有利べジェ曲線
	 * @throws IllegalArgumentException 制御点がnullの場合
	 * @throws IllegalArgumentException 重みが無限大または非数の場合
	 * @throws OutOfRangeException パラメータ範囲が不正の場合
	 */
	public static QuadraticBezierCurve create( Point _cp0, Point _cp1, Point _cp2, double _weight, Range _range ) {
		if ( _cp0 == null | _cp1 == null || _cp2 == null ) {
			throw new IllegalArgumentException( "control point is null" );
		}
		if ( Double.isInfinite( _weight ) || Double.isNaN( _weight ) ) {
			throw new IllegalArgumentException( "_weight is is illegal number(_weight: " + _weight + ")" );
		}
		if ( _range == null ) {
			throw new IllegalArgumentException( "_renge is null" );
		}
		return new QuadraticBezierCurve( _cp0, _cp1, _cp2, _weight, _range );
	}

	/**
	 * 重みを返します。
	 * @return 重み
	 */
	public double weight() {
		return m_weight;
	}

	/**
	 * 制御点列を返します。
	 * @return 代表点列
	 */
	public Point[] controlPoints() {
		return new Point[]{ m_cp0, m_cp1, m_cp2 };
	}

	/**
	 * 指定された番号の制御点を返します。
	 * @param _i 制御点番号
	 * @return 代表点
	 * @throws ArrayIndexOutOfBoundsException 0未満または3以上の制御点番号を要求した場合
	 */
	public Point controlPoint( int _i ) {
		Point rp = null;
		switch ( _i ) {
			case 0:
				rp = m_cp0;
				break;
			case 1:
				rp = m_cp1;
				break;
			case 2:
				rp = m_cp2;
				break;
			default:
				throw new ArrayIndexOutOfBoundsException( String.format( "_i:%d is out of bound:[ 0 - 2 ]", _i ) );
		}

		return rp;
	}

	@Override
	public double length() {
		return Point.length( evaluateAllByOptimized( (int) Math.ceil( range().length() / 0.1 ), 0.001 ) );
	}

	@Override
	public Point evaluateAt( double _parameter ) {
		return evaluate( _parameter );
	}

	@Override
	protected Point evaluate( double _parameter ) {
		// パラメータを[ 0.0 - 2.0 ]の範囲に正規化
		double t = _parameter % 2.0;
		if ( t < 0 ) {
			t += 2.0;
		}

		double w0, w1, w2;
		if ( t > 1.0 ) {
			t = 2 - t;
			w0 = 1 - t;
			w1 = -2 * ( 1 + m_weight ) * t * ( 1 - t );
			w2 = t;
		} else {
			w0 = ( 1 - t ) * ( 1 - t ) - t * ( 1 - t );
			w1 = 2 * ( 1 + m_weight ) * t * ( 1 - t );
			w2 = t * t - t * ( 1 - t );
		}
		double sum = 1 / ( w0 + w1 + w2 );
		w0 *= sum;
		w1 *= sum;
		w2 *= sum;

		return Point.createXYZTF(
			w0 * m_cp0.x() + w1 * m_cp1.x() + w2 * m_cp2.x(),
			w0 * m_cp0.y() + w1 * m_cp1.y() + w2 * m_cp2.y(),
			w0 * m_cp0.z() + w1 * m_cp1.z() + w2 * m_cp2.z(),
			_parameter,
			Math.abs( w0 ) * m_cp0.fuzziness() + Math.abs( w1 ) * m_cp1.fuzziness() + Math.abs( w2 ) * m_cp2.fuzziness() );
	}

	@Override
	public Point[] intersectionWith( Plane _plane ) {
		List<Point> result = new ArrayList<Point>();

		double start = range().start();
		RationalBezierCurve[] beziers = toRationalBeziers();
		Point endIntersection = null;
		for ( int i = 0; i < beziers.length - 1; ++i ) {
			Point[] intersections = beziers[i].intersectionWith( _plane );
			List<Point> added = new ArrayList<Point>( intersections.length );
			double offset = Math.floor( start + i );
			Range bezierRange = beziers[i].range();
			double ratio = bezierRange.length();
			if ( endIntersection != null ) {
				if ( intersections.length == 0
					|| ( intersections.length > 0 && intersections[0].time() > 0.0 ) ) {
					// 前のBezier曲線ではパラメータ1のところで交点が見つかっていて，現在のBezier曲線ではパラメータ0のところで交点が見つからなかったとき
					added.add( endIntersection );
				}
				endIntersection = null;
			}
			for ( Point p : intersections ) {
				double t = offset + bezierRange.start() + ratio * p.time();
				if ( p.time() < 1.0 ) {
					added.add( evaluateAt( t ) );
				} else if ( p.time() == 1.0 ) {
					endIntersection = evaluateAt( t );
				}
			}
			result.addAll( added );
		}
		Point[] intersections = beziers[beziers.length - 1].intersectionWith( _plane );
		List<Point> added = new ArrayList<Point>( intersections.length );
		double offset = Math.floor( start + beziers.length - 1 );
		Range range = beziers[beziers.length - 1].range();
		double ratio = range.length();
		if ( endIntersection != null
			&& ( intersections.length == 0
			|| ( intersections.length > 0 && intersections[0].time() > 0.0 ) ) ) {
			// 前のBezier曲線ではパラメータ1のところで交点が見つかっていて，現在のBezier曲線ではパラメータ0のところで交点が見つからなかったとき
			added.add( endIntersection );
		}
		for ( Point p : intersections ) {
			double t = offset + range.start() + ratio * p.time();
			added.add( evaluateAt( t ) );
		}
		result.addAll( added );
		return result.toArray( new Point[result.size()] );
	}

	@Override
	public QuadraticBezierCurve transform( TransformMatrix _mat ) {
		Point[] controlPoints = controlPoints();
		int size = controlPoints.length;
		Point[] transformed = new Point[size];
		for ( int i = 0; i < size; ++i ) {
			transformed[i] = controlPoints[i].transform( _mat );
		}

		return new QuadraticBezierCurve( transformed[0], transformed[1], transformed[2], weight(), range() );
	}

	@Override
	public ParametricCurve part( Range _range ) {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of range:%s", _range, range() ) );
		}

		return new QuadraticBezierCurve( controlPoint( 0 ), controlPoint( 1 ), controlPoint( 2 ), weight(), _range );
	}

	@Override
	public ParametricCurve invert() {
		Range range = range();
		double base = Math.ceil( range.end() );
		Range invertedRange = Range.create( base - range.end(), base - range.start() );

		return new QuadraticBezierCurve( controlPoint( 0 ), controlPoint( 1 ), controlPoint( 2 ), weight(), invertedRange );
	}

	/**
	 * 有理Bezier曲線列化します。 変換後の曲線の曖昧さの大きさは0になります。
	 * @return 有理Bezier曲線列
	 */
	public RationalBezierCurve[] toRationalBeziers() {
		Range range = range();
		double start = range.start();
		int num = Math.max( (int) Math.ceil( range.end() ) - (int) Math.floor( start ), 1 );
		RationalBezierCurve[] beziers = new RationalBezierCurve[num];

		Point mid = m_cp0.internalDivision( m_cp2, 1, 1 );
		Point[] rectoWCP = new Point[]{
			Point.createXYZ( m_cp0.x(), m_cp0.y(), m_cp0.z() ),
			Point.createXYZ(
			( m_weight + 1 ) * m_cp1.x() - mid.x(),
			( m_weight + 1 ) * m_cp1.y() - mid.y(),
			( m_weight + 1 ) * m_cp1.z() - mid.z() ),
			Point.createXYZ( m_cp2.x(), m_cp2.y(), m_cp2.z() )
		};
		Point[] versoWCP = new Point[]{
			Point.createXYZ( m_cp2.x(), m_cp2.y(), m_cp2.z() ),
			Point.createXYZ(
			mid.x() - ( m_weight + 1 ) * m_cp1.x(),
			mid.y() - ( m_weight + 1 ) * m_cp1.y(),
			mid.z() - ( m_weight + 1 ) * m_cp1.z() ),
			Point.createXYZ( m_cp0.x(), m_cp0.y(), m_cp0.z() )
		};
		double[] rectoWeights = new double[]{ 1.0, m_weight, 1.0 };
		double[] versoWeights = new double[]{ 1.0, -m_weight, 1.0 };

		for ( int i = 0; i < beziers.length; ++i ) {
			// 区間幅
			double rangeLength = Math.min( Math.floor( start + 1 ) - start, range.end() - start );
			// 始点パラメータを[ 0.0 - 2.0 ]の範囲に正規化
			double min = start % 2.0;
			if ( min < 0.0 ) {
				min += 2.0;
			}

			Point[] wcp;
			double[] weights;
			if ( min < 1.0 ) {
				wcp = rectoWCP;
				weights = rectoWeights;
			} else {
				// 始点パラメータを[ 0.0 - 1.0 ]の範囲に正規化
				min = ( min - 1.0 ) % 1.0;
				if ( min < 0.0 ) {
					min += 1.0;
				}
				wcp = versoWCP;
				weights = versoWeights;
			}
			beziers[i] = RationalBezierCurve.create( wcp, weights, Range.create( min, min + rangeLength ) );
			start += rangeLength;
		}

		return beziers;
	}

	/**
	 * この QuadraticBezierCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この QuadraticBezierCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 各制御点、重み、パラメータ範囲がまったく同じ QuadraticBezierCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( !( obj instanceof QuadraticBezierCurve ) ) {
			return false;
		}
		final QuadraticBezierCurve other = (QuadraticBezierCurve) obj;
		if ( this.m_cp0 != other.m_cp0 && ( this.m_cp0 == null || !this.m_cp0.equals( other.m_cp0 ) ) ) {
			return false;
		}
		if ( this.m_cp1 != other.m_cp1 && ( this.m_cp1 == null || !this.m_cp1.equals( other.m_cp1 ) ) ) {
			return false;
		}
		if ( this.m_cp2 != other.m_cp2 && ( this.m_cp2 == null || !this.m_cp2.equals( other.m_cp2 ) ) ) {
			return false;
		}
		if ( this.m_weight != other.m_weight ) {
			return false;
		}
		return super.equals( obj );
	}

	/**
	 * この QuadraticBezierCurve のハッシュコードを返します。
	 * @return この QuadraticBezierCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + super.hashCode();
		hash = 47 * hash + ( this.m_cp0 != null ? this.m_cp0.hashCode() : 0 );
		hash = 47 * hash + ( this.m_cp1 != null ? this.m_cp1.hashCode() : 0 );
		hash = 47 * hash + ( this.m_cp2 != null ? this.m_cp2.hashCode() : 0 );
		hash = 47 * hash + (int) ( Double.doubleToLongBits( this.m_weight ) ^ ( Double.doubleToLongBits( this.m_weight ) >>> 32 ) );
		return hash;
	}

	/**
	 * この QuadraticBezierCurve の文字列表現を返します。
	 * @return 各制御点、重み、パラメータ範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "cp0:%s cp1:%s cp2:%s w:%.3f %s", m_cp0, m_cp1, m_cp2, m_weight, super.toString() );
	}

	private QuadraticBezierCurve( Point _cp0, Point _cp1, Point _cp2, double _weight, Range _range ) {
		super( _range );
		m_cp0 = Point.createXYZTF( _cp0.x(), _cp0.y(), _cp0.z(), 0.0, _cp0.fuzziness() );
		m_cp1 = Point.createXYZTF( _cp1.x(), _cp1.y(), _cp1.z(), 0.5, _cp1.fuzziness() );
		m_cp2 = Point.createXYZTF( _cp2.x(), _cp2.y(), _cp2.z(), 1.0, _cp2.fuzziness() );
		m_weight = _weight;
	}

	/** 制御点0 */
	private final Point m_cp0;
	/** 制御点1 */
	private final Point m_cp1;
	/** 制御点2 */
	private final Point m_cp2;
	/** 重み */
	private final double m_weight;
}
