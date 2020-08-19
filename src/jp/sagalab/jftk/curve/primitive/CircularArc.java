package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 円弧を表すクラスです。
 * @author ishiguro
 */
public class CircularArc extends PrimitiveCurve{
	
	/**
	 * 円弧を生成します。
	 * @param _center 中心
	 * @param _radius 半径
	 * @param _angle 開始角
	 * @param _posture 姿勢
	 */
	protected CircularArc( Point _center, double _radius, double _angle, TransformMatrix _posture ) {
		super( Range.create( 0, _angle) );
		m_center = _center;
		m_radius = _radius;
		m_posture = _posture;
	}
	
	/**
	 * 円弧を生成します。
	 * @param _center 中心
	 * @param _radius 半径
	 * @param _angle 開始角
	 * @param _posture 姿勢
	 * @return インスタンス
	 * @throws IllegalArgumentException _centerがnullの場合
	 * @throws IllegalArgumentException _postureがnullの場合
	 */
	public static CircularArc create( Point _center, double _radius, double _angle, TransformMatrix _posture ){
		if(_center == null){
			throw new IllegalArgumentException( "_center is null" );
		}
		if(_posture == null){
			throw new IllegalArgumentException( "_posture is null" );
		}
		
		return new CircularArc(_center, _radius, _angle, _posture );
	}
	
	/**
	 * 指定した角度の座標を返します。
	 * @param _parameter 始点からの角度(rad)
	 * @return 座標
	 */
	@Override
	public Point locus( double _parameter ) throws OutOfRangeException {
		if(!range().isInner( _parameter)){
			throw new OutOfRangeException("_parameter is out of range");
		}
		Point pos = Point.createXYZ( m_radius * Math.cos( _parameter ), m_radius * Math.sin( _parameter ), 0 );
		TransformMatrix translation = TransformMatrix.translation( m_center.x(), m_center.y(), m_center.z() );
		pos = pos.transform( m_posture.product( translation ) );
		return Point.createXYZT( pos.x(), pos.y(), pos.z(), _parameter );
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public double length() {
		// (直径)*(π)*((定義域の長さ)/(2π))
		return m_radius * range().length();
	}

	@Override
	public BezierCurve[] convert() {
		// 存在範囲（Radian）を内包するようなベジェ曲線列を構成
		Range range = range();
		// 開始・終了
		double quater = Math.PI / 2.0;
		int start = (int) Math.floor( range.start() / quater );
		int end = (int) Math.ceil( range.end() / quater );
		// 少なくとも１区間は作る
		if ( start == end ) {
			++end;
		}
		BezierCurve[] beziers = new BezierCurve[ end - start ];
		// 姿勢・位置から相似変換行列を準備
		Point center = m_center;
		TransformMatrix mat = m_posture.translate( center.x(), center.y(), center.z() );
		Vector axis = Vector.createXYZ( 0, 0, 1 );
		for ( int i = start; i < end; ++i ) {
			BezierCurve bezier = BezierCurve.generateQuadArc( m_radius, m_radius );
			// 姿勢・位置の適用
			beziers[i - start] = bezier.transform( TransformMatrix.rotation( quater * i, axis ).product( mat ) );
		}

		// 最初と最後のベジェ曲線を切断
		Vector n = Vector.createXYZ( m_posture.get( 0, 2 ), m_posture.get( 1, 2 ), m_posture.get( 2, 2 ) );
		if ( start * quater < range.start() ) {
			Point s = locus( range().start() );
			Plane plane = Plane.create( s, n.cross( Vector.createSE( center, s ) ) );
			Point[] points = beziers[0].intersectionWith( plane );
			if ( points.length > 0 ) {
				beziers[0] = beziers[0].divide( points[0].time() )[1];
			}
		}
		if ( range.end() < end * quater ) {
			Point e = locus( range().end() );
			Plane plane = Plane.create( e, n.cross( Vector.createSE( center, e ) ) );
			Point[] points = beziers[beziers.length - 1].intersectionWith( plane );
			if ( points.length > 0 ) {
				beziers[beziers.length - 1] = beziers[beziers.length - 1].divide( points[0].time() )[0];
			}
		}
		// 始・終点の入れ替え
		Point[] points = beziers[0].controlPoints();
		points[0] = locus( range().start() );
		beziers[0] = BezierCurve.create( points, Range.zeroToOne() );
		points = beziers[beziers.length - 1].controlPoints();
		points[points.length - 1] = locus( range().end() );
		beziers[beziers.length - 1] = BezierCurve.create( points, Range.zeroToOne() );

		return beziers;
	}

	@Override
	public CircularArc transform( TransformMatrix _mat ) {
		Point center = m_center.transform( _mat );
		double radius = m_radius * _mat.scalalize();
		TransformMatrix posture = m_posture.product( _mat.rotatalize() );
		
		return new CircularArc(center, radius, range().end(), posture );
	}

	@Override
	public CircularArc invert() {
		TransformMatrix posture = TransformMatrix.rotation( Math.PI, Vector.createXYZ( 0, 1, 0 ) ).product( m_posture );
		double[][] elements = posture.elements();
		double angle = range().end();
		double start = Math.PI - angle;
		TransformMatrix mat = TransformMatrix.rotation( start, Vector.createXYZ( elements[2][0], elements[2][1], elements[2][2] ) );
		return new CircularArc( m_center, m_radius, angle, posture.product( mat ) );
	}
	
	/**
	 * 中心点を返します。
	 * @return 中心点
	 */
	public Point center() {
		return m_center;
	}

	/**
	 * 半径を返します。
	 * @return 半径
	 */
	public double radius() {
		return m_radius;
	}

	/**
	 * 姿勢行列を返します。
	 * @return 姿勢行列
	 */
	public TransformMatrix posture() {
		return m_posture;
	}
	
	/**
	 * この CircularArc と指定された Object が等しいかどうかを比較します。 
	 * @param obj この CircularArc と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 中心、半径、姿勢、閉じているかがまったく同じ CircularArc である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final CircularArc other = (CircularArc) obj;
		if ( this.center() != other.center() && ( this.center() == null || !this.center().equals( other.center() ) ) ) {
			return false;
		}
		if ( this.radius() != other.radius() ) {
			return false;
		}
		return this.posture() == other.posture() || ( this.posture() != null && this.posture().equals( other.posture() ) );
	}

	/**
	 * この CircularArc のハッシュコードを返します。 
	 * @return この CircularArc のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + ( this.center() != null ? this.center().hashCode() : 0 );
		hash = 47 * hash + (int) ( Double.doubleToLongBits( this.radius() ) ^ ( Double.doubleToLongBits( this.radius() ) >>> 32 ) );
		hash = 47 * hash + ( this.posture() != null ? this.posture().hashCode() : 0 );
		return hash;
	}
	
	@Override
	public String toString() {
		return String.format( "center:%s radius:%.3f angle:%s posture:%s closed:%s", m_center, m_radius, range(), m_posture, false );
	}
	
	/** 中心 */
	private final Point m_center;
	/** 半径 */
	private final double m_radius;
	/** 姿勢 */
	private final TransformMatrix m_posture;
}
