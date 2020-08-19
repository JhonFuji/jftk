package jp.sagalab.jftk;

/**
 * 平面を表すクラスです。
 * @author Akira Nishikawa
 */
public class Plane {

	/**
	 * 平面を生成します。
	 * @param _p 平面上の任意の点
	 * @param _normal 面法線
	 * @return 平面
	 * @exception IllegalArgumentException 面法線の長さが０の場合にスローします。
	 */
	public static Plane create( Point _p, Vector _normal ) {
		if ( _p == null ) {
			throw new IllegalArgumentException( "_p is null" );
		}
				Vector normal = _normal.normalize();
		if ( Double.isInfinite( 1 / normal.length() )) {
			throw new IllegalArgumentException( "_normal length is zero." );
		}
		return new Plane( _p, normal );
		}

	/**
	 * 平面を生成します。
	 * @param _alpha 平面上の任意の点A
	 * @param _beta 平面上の任意の点B
	 * @param _gamma 平面上の任意の点C
	 * @return 平面
	 */
	public static Plane create( Point _alpha, Point _beta, Point _gamma ) {
		return Plane.create( _alpha, Vector.createNormal( _alpha, _beta, _gamma ) );
	}

	/**
	 * 面の法線を返します。
	 * @return 法線
	 */
	public Vector normal() {
		return m_normalVector;
	}

	/**
	 * 平面上の基準点を返します。
	 * @return 平面上の基準点
	 */
	public Point base() {
		return m_base;
	}

	/**
	 * 指定した点への距離を返します。
	 * @param _p 点
	 * @return 距離
	 */
	public double distance( Point _p ) {
		Vector v = Vector.createSE( m_base, _p );
		return m_normalVector.dot( v );
	}

	/**
	 * 指定した点の射影を返します。
	 * @param _p 点
	 * @return 射影
	 */
	public Point project( Point _p ) {
		return _p.move( m_normalVector.magnify( -distance( _p ) ) );
	}

	/**
	 * 指定された始点と終点を通る直線との交点を導出します。<br>
	 * もし交差していなければnullを返します。
	 * @param _start 始点
	 * @param _end 終点
	 * @return 交点
	 */
	public Point intersectWith( Point _start, Point _end ) {
		// 交点
		Point ip = null;

		Vector v = Vector.createSE( _start, _end );
		double distance = m_normalVector.x() * m_base.x() + m_normalVector.y() * m_base.y() + m_normalVector.z() * m_base.z();
		double t = ( distance - m_normalVector.dot( Vector.createXYZ( _start.x(), _start.y(), _start.z() ) ) ) / m_normalVector.dot( v );

		if ( !Double.isNaN( t ) && !Double.isInfinite( t ) ) {
			ip = _start.internalDivision( _end, t, 1 - t );
		}

		return ip;
	}

	/**
	 * この Plane と指定された Object が等しいかどうかを比較します。
	 * @param obj この Plane と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 平面上の基準点、面の法線がまったく同じ Plane である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Plane other = (Plane) obj;
		if ( this.m_normalVector != other.m_normalVector
			&& ( this.m_normalVector == null || !this.m_normalVector.equals( other.m_normalVector ) ) ) {
			return false;
		}

		return this.m_base == other.m_base || ( this.base().equals( other.m_base ) );
	}

	/**
	 * この Plane のハッシュコードを返します。
	 * @return この Plane のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + ( this.m_normalVector != null ? this.m_normalVector.hashCode() : 0 );
		hash = 67 * hash + ( this.m_base != null ? this.m_base.hashCode() : 0 );
		return hash;
	}

	/**
	 * この Plane の文字列表現を返します。
	 * @return 平面上の基準点、面の法線を表す String
	 */
	@Override
	public String toString() {
		return String.format( "base:[%s] normalVector:[%s]", m_base, m_normalVector );
	}

	private Plane( Point _base, Vector _normal ) {
		m_base = _base;
		m_normalVector = _normal;
	}
	/** 平面上の基準点 */
	private final Point m_base;
	/** 面の法線 */
	private final Vector m_normalVector;
}
