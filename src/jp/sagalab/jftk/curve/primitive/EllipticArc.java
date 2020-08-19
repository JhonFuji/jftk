package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 楕円弧を表すクラスです。
 * @author ishiguro
 */
public class EllipticArc extends PrimitiveCurve{
	
	/**
	 * 楕円弧を生成します。
	 * @param _center 中心
	 * @param _major 長径
	 * @param _minor 短径
	 * @param _angle 開始角(Radian)
	 * @param _posture 姿勢
	 */
	protected EllipticArc( Point _center, double _major, double _minor, Range _angle,
		TransformMatrix _posture ) {
		super(_angle);
		
		m_center = _center;
		m_major = _major;
		m_minor = _minor;
		m_posture = _posture;
	}
	
	/**
	 * 楕円弧を生成します。
	 * @param _center 中心
	 * @param _major 長径
	 * @param _minor 短径
	 * @param _angle 開始角(Radian)
	 * @param _posture 姿勢
	 * @return インスタンス
	 * @throws IllegalArgumentException _centerがnullの場合
	 * @throws IllegalArgumentException _angleがnullの場合
	 * @throws IllegalArgumentException _postureがnullの場合
	 */
	public static EllipticArc create( Point _center, double _major, double _minor, Range _angle,
		TransformMatrix _posture ){
		if( _center == null ){
			throw new IllegalArgumentException( "_center is null" );
		}
		if( _angle == null ){
			throw new IllegalArgumentException( "_angle is null" );
		}
		if( _posture == null ){
			throw new IllegalArgumentException( "_posture is null" );
		}
		
		return new EllipticArc(_center, _major, _minor, _angle, _posture );
	}

	/**
	 * 指定した角度の座標を返します。
	 * @param _parameter 長径軸を基準とした角度(rad)
	 * @return 座標
	 */
	@Override
	public Point locus( double _parameter ) throws OutOfRangeException {
		if(!range().isInner( _parameter)){
			throw new OutOfRangeException("_parameter is out of range");
		}
		// 真円時の角度に変換
		double x = Math.cos( _parameter ) * m_minor;
		double y = Math.sin( _parameter ) * m_major;
		double angle = Math.atan2( y, x );

		Point p = Point.createXYZ( m_major * Math.cos( angle ), m_minor * Math.sin( angle ), 0 );
		TransformMatrix translation = TransformMatrix.translation( m_center.x(), m_center.y(), m_center.z() );
		p = p.transform( m_posture.product( translation ) );

		return Point.createXYZT( p.x(), p.y(), p.z(), _parameter );
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public double length() {
		BezierCurve[] beziers = convert();
		double length = 0;
		for(BezierCurve b : beziers){
			length += b.length();
		}

		return length;
	}

	@Override
	public BezierCurve[] convert() {
		// 存在範囲（角度）を内包するようなベジェ曲線列を構成
		Range range = range();
		// 開始・終了
		double quater = Math.PI / 2.0;
		int start = (int) Math.floor( range.start() / quater );
		int end = (int) Math.ceil( range.end() / quater );
		// 少なくとも１区間は作る
		if ( end - start == 0 ) {
			++end;
		}
		BezierCurve[] beziers = new BezierCurve[ end - start ];
		// 姿勢・位置から相似変換行列を準備
		Point center = m_center;
		TransformMatrix mat = m_posture.translate( center.x(), center.y(), center.z() );
		Vector axis = Vector.createXYZ( 0, 0, 1 );
		for ( int i = start; i < end; ++i ) {
			BezierCurve bezier = ( i % 2 == 0 ) ? BezierCurve.generateQuadArc( m_major, m_minor ) : BezierCurve.generateQuadArc( m_minor, m_major );
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
	public EllipticArc transform( TransformMatrix _mat ) {
		Point center = m_center.transform( _mat );
		double major = m_major * _mat.scalalize();
		double minor = m_minor * _mat.scalalize();
		TransformMatrix posture = m_posture.product( _mat.rotatalize() );
		return new EllipticArc( center, major, minor, range(), posture );
	}

	@Override
	public EllipticArc invert() {
		TransformMatrix mat = TransformMatrix.rotation( Math.PI, Vector.createXYZ( 0, 1, 0 ) );
		TransformMatrix posture = mat.product( m_posture );
		Range range = range();
		double start = Math.PI - range.end();
		double end = Math.PI - range.start();
		return new EllipticArc(m_center, m_major, m_minor, Range.create( start, end ), posture );
	}
	
	/**
	 * 中心点を返します。
	 * @return 中心点
	 */
	public Point center() {
		return m_center;
	}

	/**
	 * 長径を返します。
	 * @return 長径
	 */
	public double majorRadius() {
		return m_major;
	}

	/**
	 * 短径を返します。
	 * @return 短径
	 */
	public double minorRadius() {
		return m_minor;
	}

	/**
	 * 姿勢を返します。
	 * @return 簡約型
	 */
	public TransformMatrix posture() {
		return m_posture;
	}
	
	/**
	 * この EllipticArc と指定された Object が等しいかどうかを比較します。
	 * @param obj この EllipitcArc と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 中心、長径、短径、姿勢、閉じているかがまったく同じ EllipticArc である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final EllipticArc other = (EllipticArc) obj;
		if ( this.center() != other.center() && ( this.center() == null || !this.center().equals( other.center() ) ) ) {
			return false;
		}
		if ( this.majorRadius() != other.majorRadius() ) {
			return false;
		}
		if ( this.minorRadius() != other.minorRadius() ) {
			return false;
		}
		return this.posture() == other.posture() || ( this.posture() != null && this.posture().equals( other.posture() ) );
	}

	/**
	 * この EllipticArc のハッシュコードを返します。
	 * @return この EllipticArc のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + ( this.center() != null ? this.center().hashCode() : 0 );
		hash = 59 * hash + (int) ( Double.doubleToLongBits( this.majorRadius() ) ^ ( Double.doubleToLongBits( this.majorRadius() ) >>> 32 ) );
		hash = 59 * hash + (int) ( Double.doubleToLongBits( this.minorRadius() ) ^ ( Double.doubleToLongBits( this.minorRadius() ) >>> 32 ) );
		hash = 59 * hash + ( this.posture() != null ? this.posture().hashCode() : 0 );
		return hash;
	}

	/**
	 * この Circle の文字列表現を返します。
	 * @return 中心、長径、短径、範囲(ラジアン)、姿勢、閉じているかを表す String
	 */
	@Override
	public String toString() {
		return String.format(
			"center:%s major:%.3f minor:%.3f angle:%s posture:%s closed:%s",
			center(), majorRadius(), minorRadius(), range(), posture(), false );
	}
	
	/** 中心 */
	private final Point m_center;
	/** 長径 */
	private final double m_major;
	/** 短径 */
	private final double m_minor;
	/** 姿勢 */
	private final TransformMatrix m_posture;
}
