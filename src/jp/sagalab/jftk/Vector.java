package jp.sagalab.jftk;

import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.transform.Transformable;

/**
 * ベクトルを表すクラスです。
 * @author Akira Nishikawa
 */
public class Vector implements Transformable<Vector>{

	/**
	 * ベクトルを生成します。
	 * @param _x x成分
	 * @param _y y成分
	 * @param _z z成分
	 * @return ベクトル
	 * @throws IllegalArgumentException 各要素にNaNまたはInfiniteが指定された場合
	 */
	public static Vector createXYZ( double _x, double _y, double _z ) {
		if ( Double.isNaN( _x ) || Double.isInfinite( _x ) ) {
			throw new IllegalArgumentException("x element of vector is NaN or Infinite.");
		}
		if ( Double.isNaN( _y ) || Double.isInfinite( _y ) ) {
			throw new IllegalArgumentException("y element of vector is NaN or Infinite.");
		}
		if ( Double.isNaN( _z ) || Double.isInfinite( _z ) ) {
			throw new IllegalArgumentException("z element of vector is NaN or Infinite.");
		}
		return new Vector( _x, _y, _z );
	}

	/**
	 * ベクトルを生成します。
	 * @param _start ベクトルの始点
	 * @param _end ベクトルの終点
	 * @return ベクトル
	 */
	public static Vector createSE( Point _start, Point _end ) {
		double x = _end.x() - _start.x();
		double y = _end.y() - _start.y();
		double z = _end.z() - _start.z();

		return createXYZ( x, y, z );
	}

	/**
	 * 三角形ABCの法線ベクトルを生成します。
	 * <p>
	 * 三点ABCに対し反時計回りを正の方向とします。
	 * </p>
	 * @param _a 点A
	 * @param _b 点B
	 * @param _c 点C
	 * @return 法線ベクトル
	 */
	public static Vector createNormal( Point _a, Point _b, Point _c ) {
		Vector vA = Vector.createSE( _a, _b );
		Vector vB = Vector.createSE( _a, _c );

		Vector normal = vA.cross( vB );

		return normal.normalize();
	}

	/**
	 * X成分を返します。
	 * @return X成分
	 */
	public double x() {
		return m_x;
	}

	/**
	 * Y成分を返します。
	 * @return Y成分
	 */
	public double y() {
		return m_y;
	}

	/**
	 * Z成分を返します。
	 * @return Z成分
	 */
	public double z() {
		return m_z;
	}

	/**
	 * Z成分を返します。
	 * @return F成分
	 */
	public double fuzziness() {
		return m_f;
	}
	/**
	 * 指定されたベクトルとの内積を求めます。
	 * @param _other ベクトル
	 * @return 内積値
	 */
	public double dot( Vector _other ) {
		return m_x * _other.m_x + m_y * _other.m_y + m_z * _other.m_z;
	}

	/**
	 * 自乗を求めます。
	 * @return 自乗値
	 */
	public double square() {
		return m_x * m_x + m_y * m_y + m_z * m_z;
	}

	/**
	 * 長さを返します。
	 * @return 長さ
	 */
	public double length() {
		return Math.sqrt( m_x * m_x + m_y * m_y + m_z * m_z );
	}

	/**
	 * 指定されたベクトルとの合成を行います。
	 * @param _other ベクトル
	 * @return 合成ベクトル
	 */
	public Vector compose( Vector _other ) {
		return Vector.createXYZ( m_x + _other.m_x, m_y + _other.m_y, m_z + _other.m_z );
	}

	/**
	 * 正規化を行います。
	 * @return 正規化後のベクトル
	 */
	public Vector normalize() {
		double length = length();

		double x = m_x / length;
		double y = m_y / length;
		double z = m_z / length;

		if ( Double.isInfinite( x ) || Double.isNaN( x )
			|| Double.isInfinite( y ) || Double.isNaN( y )
			|| Double.isInfinite( z ) || Double.isNaN( z ) ) {
			return this;
		}
		return Vector.createXYZ( x, y, z );
	}

	/**
	 * 拡大縮小を行います。
	 * @param _ratio 拡大縮小率
	 * @return 拡大縮小後のベクトル
	 */
	public Vector magnify( double _ratio ) {
		if ( Double.isNaN( _ratio ) || Double.isInfinite( _ratio ) ) {
			throw new IllegalArgumentException();
		}
		return createXYZ( m_x * _ratio, m_y * _ratio, m_z * _ratio );
	}

	/**
	 * 指定されたベクトルと外積を行います。
	 * @param _other ベクトル
	 * @return 外積値
	 */
	public Vector cross( Vector _other ) {
		double x = m_y * _other.m_z - m_z * _other.m_y;
		double y = m_z * _other.m_x - m_x * _other.m_z;
		double z = m_x * _other.m_y - m_y * _other.m_x;
		return createXYZ( x, y, z );
	}

	/**
	 * 指定されたベクトルとの相対角度を導出します。
	 * <p>
	 * 二つのベクトルにおける方向成分の相対角度を導出します。<br>
	 * ただし、どちらかのベクトルの方向が不定であれば、結果も不定（NaN）が返ります。
	 * </p>
	 * @param _other ベクトル
	 * @return 相対角度[ 0 - Math.PI ]
	 */
	public double angle( Vector _other ) {
		double thisLength = length();
		double otherLength = _other.length();

		double angle = Double.NaN;

		if ( thisLength > 0 && otherLength > 0 ) {
			angle = Math.atan2( cross( _other ).length(), dot( _other ) ) ;
		}

		return angle;
	}

	/**
	 * 方向を逆転します。
	 * @return 逆転後のベクトル
	 */
	public Vector reverse() {
		return Vector.createXYZ( -m_x, -m_y, -m_z );
	}

	@Override
	public Vector transform( TransformMatrix _mat ) {
		double x = _mat.get( 0, 0 ) * m_x + _mat.get( 0, 1 ) * m_y + _mat.get( 0, 2 ) * m_z + _mat.get( 0, 3 );
		double y = _mat.get( 1, 0 ) * m_x + _mat.get( 1, 1 ) * m_y + _mat.get( 1, 2 ) * m_z + _mat.get( 1, 3 );
		double z = _mat.get( 2, 0 ) * m_x + _mat.get( 2, 1 ) * m_y + _mat.get( 2, 2 ) * m_z + _mat.get( 2, 3 );
		return Vector.createXYZ( x, y, z );
	}

	/**
	 * この Vector と指定された Object が等しいかどうかを比較します。
	 * @param obj この Vector と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * x成分、y成分、z成分がまったく同じ Vector である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Vector other = (Vector) obj;
		if ( this.m_x != other.m_x ) {
			return false;
		}
		if ( this.m_y != other.m_y ) {
			return false;
		}

		return this.m_z == other.m_z;
	}

	/**
	 * この Vector のハッシュコードを返します。
	 * @return この Vector のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + (int) ( Double.doubleToLongBits( this.m_x ) ^ ( Double.doubleToLongBits( this.m_x ) >>> 32 ) );
		hash = 89 * hash + (int) ( Double.doubleToLongBits( this.m_y ) ^ ( Double.doubleToLongBits( this.m_y ) >>> 32 ) );
		hash = 89 * hash + (int) ( Double.doubleToLongBits( this.m_z ) ^ ( Double.doubleToLongBits( this.m_z ) >>> 32 ) );
		return hash;
	}

	/**
	 * この Vector の文字列表現を返します。
	 * @return x成分、y成分、z成分を表す String
	 */
	@Override
	public String toString() {
		return String.format( "x:%.3f y:%.3f z:%.3f", m_x, m_y, m_z );
	}

	/**
	 * ベクトルを生成します。
	 * このコンストラクタは継承のためにprotectedが指定されています。
	 * @param _x x成分
	 * @param _y y成分
	 * @param _z z成分
	 */
	public Vector( double _x, double _y, double _z ) {
		m_x = _x;
		m_y = _y;
		m_z = _z;
		m_f = 0.0;
	}

	public Vector( double _x, double _y, double _z, double _f ) {
		m_x = _x;
		m_y = _y;
		m_z = _z;
		m_f = _f;
	}

	/** X成分 */
	private final double m_x;
	/** Y成分 */
	private final double m_y;
	/** Z成分 */
	private final double m_z;
	/** F成分 */
	private final double m_f;
}
