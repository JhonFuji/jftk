package jp.sagalab.jftk;

import java.util.Comparator;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.transform.TransformMatrix;
import java.util.List;
import jp.sagalab.jftk.transform.Transformable;

/**
 * 位置の曖昧さを持つ点(ファジィ点)を表すクラスです。
 * <p>
 * ファジィ点は円錐型メンバシップ関数によって特徴づけられる円錐型ファジィ点となります。
 * </p>
 * <p>
 * 位置の曖昧さ(ファジネス)が0の場合は通常の点として扱われます。
 * </p>
 * @author Akira Nishikawa
 * @see <span>「ファジィスプライン曲線同定法」</span>
 */
public class Point extends FuzzySet implements Transformable<Point>{

	/**
	 * このファジィ点の補集合を表すクラスです。
	 */
	public static class Complement {
		
		/**
		 * どの点の補集合であるかを区別するために、Pointクラスの変数を渡します。<BR>
		 * よってこのクラスが持っている情報は、元の点のx座標値、y座標値、z座標値、時間、ファジネスです。
		 * @param _point 元の点情報
		 */
		public Complement( Point _point ) {
			m_point = _point;
		}

		/**
		 * この補集合の補集合を返します。<BR>
		 * つまり元の点を返します。
		 * @return 点の補集合の補集合
		 */
		public Point getComplement() {
			return m_point;
		}

		/**
		 * 指定された点に含まれているかを評価します。
		 * @param _point ファジィ点
		 * @return 区間真理値
		 */
		public TruthValue includedIn( Point _point ) {
			// 可能性値の計算
			// この補集合と他方の点の距離
			double distance = m_point.distance( _point );
			// この補集合と他方の点のファジネスの和
			double fuzzinessSum = m_point.m_f + _point.m_f;
			// 可能性値
			double pos = 0;

			if ( fuzzinessSum < Math.ulp( 0 ) ) {
				if ( distance < Math.ulp( 0 ) ) {
					pos = 1;
				}
			} else {
				pos = Math.min( ( _point.m_f + distance ) / fuzzinessSum, 1 );
			}

			// 必然性値(この点の補集合が他方の点の補集合に含まれる可能性)を計算する
			// 定義より、1 - pos が求める必然性値となる
			// しかし、補集合が補集合に含まれる可能性値は必ず1になるため、
			// この場合の必然性値は nec = 0 となる
			double nec = 0;

			return TruthValue.create( nec, pos );
		}

		/**
		 * 指定された点の補集合に含まれているかを評価します。
		 * @param _complement ファジィ点の補集合
		 * @return 区間真理値
		 */
		public TruthValue includedIn( Point.Complement _complement ) {
			// 可能性値
			// 補集合と補集合の共通集合の最大値は、常に1であるため、可能性値は1となる
			double pos = 1;

			// 他方の点の補集合の補集合との区間真理値を計算
			TruthValue tv = includedIn( _complement.getComplement() );
			// 必然性値 : 定義より 1 - tvの可能性値
			double nec = 1 - tv.possibility();

			return TruthValue.create( nec, pos );
		}
		
		/** 元の点情報 */
		private final Point m_point;
	}
	
	/**
	 * パラメータコンパレータを表すクラスです。
	 */
	public static class ParameterComparator implements Comparator<Point> {

		/**
		 * 順序付けのために二つの引数を比較します。<br>
		 * 点の時刻を比較します。
		 * @param o1 点A
		 * @param o2 点B
		 * @return 点Aの時刻が点Bの時刻より小さい場合は負の整数、
		 *          両方が等しい場合は 0、点Aの時刻が点Bの時刻より大きい場合は正の整数
		 */
		@Override
		public int compare( Point o1, Point o2 ) {
			double t1 = o1.time();
			double t2 = o2.time();
			if ( t1 < t2 ) {
				return -1;
			} else if ( t1 > t2 ) {
				return 1;
			}
			return 0;
		}
	}
	
	/**
	 * ファジィ点を生成します。
	 * @param _x X座標
	 * @param _y Y座標
	 * @param _z Z座標
	 * @return 時刻がNaNかつファジネスが0のファジィ点
	 */
	public static Point createXYZ( double _x, double _y, double _z ) {
		return createXYZTF( _x, _y, _z, Double.NaN, 0 );
	}

	/**
	 * ファジィ点を生成します。
	 * @param _x X座標
	 * @param _y Y座標
	 * @param _z Z座標
	 * @param _time 時刻
	 * @return ファジネスが0のファジィ点
	 */
	public static Point createXYZT( double _x, double _y, double _z, double _time ) {
		return createXYZTF( _x, _y, _z, _time, 0 );
	}

	/**
	 * ファジィ点を生成します。
	 * @param _x X座標
	 * @param _y Y座標
	 * @param _z Z座標
	 * @param _time 時刻
	 * @param _fuzziness 位置のあいまいさ
	 * @return ファジィ点
	 * @throws IllegalArgumentException 各値にNaNまたはInfiniteが指定された場合
	 * @throws OutOfRangeException ファジネスに0未満が指定された場合
	 */
	public static Point createXYZTF( double _x, double _y, double _z, double _time, double _fuzziness ) {
		if ( Double.isNaN( _x ) ) {
			throw new IllegalArgumentException( "_x is NaN." );
		}
		if ( Double.isInfinite( _x ) ) {
			throw new IllegalArgumentException( "_x is Inf." );
		}
		if ( Double.isNaN( _y ) ) {
			throw new IllegalArgumentException( "_y is NaN." );
		}
		if ( Double.isInfinite( _y ) ) {
			throw new IllegalArgumentException( "_y is Inf." );
		}
		if ( Double.isNaN( _z ) ) {
			throw new IllegalArgumentException( "_z is NaN." );
		}
		if ( Double.isInfinite( _z ) ) {
			throw new IllegalArgumentException( "_z is Inf." );
		}
		if ( Double.isInfinite( _time ) ) {
			throw new IllegalArgumentException( "_time is Inf." );
		}
		if ( Double.isNaN( _fuzziness ) ) {
			throw new IllegalArgumentException( "_fuzziness is NaN." );
		}
		if ( Double.isInfinite( _fuzziness ) ) {
			throw new IllegalArgumentException( "_fuzziness is Inf" );
		}
		if ( Double.isInfinite( _z ) ) {
			throw new IllegalArgumentException( "_fuzziness is Inf." );
		}

		if ( _fuzziness < 0 ) {
			throw new OutOfRangeException( "_fuzziness < 0" );
		}
		return new Point( _x, _y, _z, _time, _fuzziness );
	}

	/**
	 * X座標を返します。
	 * @return X座標
	 */
	public double x() {
		return m_x;
	}

	/**
	 * Y座標を返します。
	 * @return Y座標
	 */
	public double y() {
		return m_y;
	}

	/**
	 * Z座標を返します。
	 * @return Z座標
	 */
	public double z() {
		return m_z;
	}

	/**
	 * 時刻を返します。
	 * @return 時刻
	 */
	public double time() {
		return m_t;
	}

	/**
	 * 位置のあいまいさ(ファジネス)を返します。
	 * @return 位置のあいまいさ(ファジネス)
	 */
	public double fuzziness() {
		return m_f;
	}

	/**
	 * 指定された点と内分計算を行います。
	 * <p>
	 * 自身と他方の点を _tA : _tB に内分する点を求めます。<br>
	 * また、内部で自動的に内分比の合計が１になるように正規化します。<br>
	 * ex) 「1 : 4」 は 「0.2 : 0.8」 となります。<br>
	 * また、内分比の合計が0になる場合は自身の点を返します。
	 * </p>
	 * @param _other 点
	 * @param _tA 内分比A
	 * @param _tB 内分比B
	 * @return 内分点
	 * @throws IllegalArgumentException 内分比にNaNまたはInfiniteが指定された場合
	 */
	public Point internalDivision( Point _other, double _tA, double _tB ) {
		// 内分比のチェック
		// 非数のチェック
		if ( Double.isNaN( _tA ) || Double.isNaN( _tB ) ) {
			throw new IllegalArgumentException("Internal ratio is NaN.");
		}
		// 無限大のチェックと補正
		if ( Double.isInfinite( _tA ) || Double.isInfinite( _tB ) ) {
			throw new IllegalArgumentException("Internal ratio is Infinite.");
		}

		// 内分比の合計
		double sumOfRatio = _tA + _tB;

		// 内分計算
		// ファジネスの項だけは内分比を絶対値にする
		double x = ( _tB * m_x + _tA * _other.m_x ) / sumOfRatio;
		double y = ( _tB * m_y + _tA * _other.m_y ) / sumOfRatio;
		double z = ( _tB * m_z + _tA * _other.m_z ) / sumOfRatio;
		double time = ( _tB * m_t + _tA * _other.m_t ) / sumOfRatio;
		double fuzziness = ( Math.abs( _tB ) * m_f + Math.abs( _tA ) * _other.m_f ) / sumOfRatio;

		if ( Double.isInfinite( x ) || Double.isNaN( x )
			|| Double.isInfinite( y ) || Double.isNaN( y )
			|| Double.isInfinite( z ) || Double.isNaN( z )
			|| Double.isInfinite( fuzziness ) || Double.isNaN( fuzziness ) ) {
			return this;
		}

		return createXYZTF( x, y, z, time, fuzziness );
	}

	/**
	 * 指定された点との距離を導出します。
	 * @param _other 点
	 * @return 二点間の距離
	 */
	public double distance( Point _other ) {
		double dx = m_x - _other.m_x;
		double dy = m_y - _other.m_y;
		double dz = m_z - _other.m_z;

		return Math.sqrt( dx * dx + dy * dy + dz * dz );
	}

	/**
	 * 指定された点との修正距離を導出します。
	 * ( 二点間の距離 / 他方点のファジネス )
	 * @param _other 点
	 * @return 二点間の修正距離
	 */
	public double modifiedDistance( Point _other ) {
		return distance( _other ) / _other.m_f;
	}

	/**
	 * この点を移動します。
	 * @param _v ベクトル
	 * @return 移動後の点
	 */
	public Point move( Vector _v ) {
		return createXYZTF( m_x + _v.x(), m_y + _v.y(), m_z + _v.z(), m_t, m_f );
	}

	/**
	 * この点を移動します。
	 * @param _x x方向の移動量
	 * @param _y y方向の移動量
	 * @param _z z方向の移動量
	 * @return 移動後の点
	 */
	public Point move( double _x, double _y, double _z ) {
		return createXYZTF( m_x + _x, m_y + _y, m_z + _z, m_t, m_f );
	}

	@Override
	public TruthValue includedIn( Point _other ) {
		double distance = distance( _other );
		double fuzzinessSum = m_f + _other.m_f;

		double nec;
		double pos;
		if ( Double.isInfinite( fuzzinessSum ) ) {
			nec = 0;
			pos = 1;
		} else {
			nec = Math.max( ( _other.m_f - distance ) / fuzzinessSum, 0 );
			pos = Math.max( ( fuzzinessSum - distance ) / fuzzinessSum, 0 );
		}
		if ( Double.isNaN( nec ) && Double.isNaN( pos ) ) {
			nec = 0.5;
			pos = 1;
		}

		return TruthValue.create( nec, pos );
	}

	/**
	 * 指定された点の補集合に含まれているかを評価します。
	 * @param _complement 点の補集合
	 * @return 区間真理値
	 */
	public TruthValue includedIn( Point.Complement _complement ) {
		// 可能性値の計算
		// この点と補集合との距離
		double distance = distance( _complement.m_point );
		// この点と補集合のファジネスの和
		double fuzzinessSum = m_f + _complement.m_point.m_f;
		// 可能性値
		double pos = 0;

		if ( fuzzinessSum < Math.ulp( 0 ) ) {
			if ( distance < Math.ulp( 0 ) ) {
				pos = 1;
			}
		} else {
			pos = Math.min( ( m_f + distance ) / fuzzinessSum, 1 );
		}

		// 必然性値の計算
		// 他方の点の補集合の補集合との区間真理値を計算
		TruthValue tv = this.includedIn( _complement.getComplement() );
		// 必然性値 : 定義より 1 - tvの可能性値
		double nec = 1 - tv.possibility();

		// 区間真理値を生成
		return TruthValue.create( nec, pos );
	}

	@Override
	public TruthValue includedIn( MultiPoint _multiPoint ) {
		// 他方のマルチファジィ点の個数
		int length = _multiPoint.length();
		// マルチファジィ点の要素を持つリストを取得
		List<Point> multiPoint = _multiPoint.getMultiPointForList();
		// マルチファジィ点にこの点を追加し、新しいマルチファジィ点を構成する
		multiPoint.add( this );
		MultiPoint tmpMultiPoint = MultiPoint.create(multiPoint.toArray( new Point[ 0 ] ) );
		// 新たに構成したマルチファジィ点の頂点
		Point tmpMultiPointVertex = tmpMultiPoint.getVertex();
		// 許容誤差を設定
		// TODO 許容誤差をどのような値とするか (現状は1.0E-10に設定)
		double tolerance = 1.0E-10;
		// 可能性値
		double pos = 1.0;
		// ファジィ点がファジィ点に含まれる可能性値の導出方法と同様の考え方で可能性値を求める
		// 新たに構成したマルチファジィ点の各要素と頂点との距離を取り、頂点の位置でのファジィ点のグレードを算出する
		// そのグレード値の最小値が可能性値となる
		for ( int i = 0; i < tmpMultiPoint.length(); ++i ) {
			Point point = tmpMultiPoint.getElement( i );
			double tmpPos;
			// ファジィ点と頂点との距離
			double distance = point.distance( tmpMultiPointVertex );
			// ある点がクリスプな点(ファジネスが許容誤差以下)である場合
			if( point.fuzziness() < tolerance ){
				// クリスプな点と頂点の距離が0(許容誤差以下)の場合
				if( distance < tolerance ){
					// クリスプな点なのでグレード値は1
					tmpPos = 1.0;
				}
				else{
					// それ以外はグレード値0
					tmpPos = 0.0;
				}
			}
			// ある点がファジィ点である場合
			else{
				tmpPos = ( point.fuzziness() - distance ) / point.fuzziness();
				// 「ファジネス < 距離」の場合はグレード値が負になるので、その場合は0とする
				tmpPos = Math.max( tmpPos, 0.0 );
			}
			pos = ( tmpPos < pos ) ? tmpPos : pos;
		}

		// この点と、マルチファジィ点の補集合との可能性値
		double complementaryPos = 0;
		// この点と、マルチファジィ点の要素の補集合との可能性値の計算
		for ( int i = 0; i < length; ++i ) {
			TruthValue tv = this.includedIn( _multiPoint.getElement( i ).getComplement() );
			// 求めた可能性値のうち、最大のものを選択
			complementaryPos = Math.max( tv.possibility(), complementaryPos );
		}

		// 導出された可能性値を1から引く
		// これによって、マルチファジィ点と他方の点との必然性値が計算できる
		double nec = 1 - complementaryPos;

		// 区間真理値のインスタンスを生成
		return TruthValue.create( nec, pos );
	}

	@Override
	public Point transform( TransformMatrix _mat ) {
		double x = _mat.get( 0, 0 ) * m_x + _mat.get( 0, 1 ) * m_y + _mat.get( 0, 2 ) * m_z + _mat.get( 0, 3 );
		double y = _mat.get( 1, 0 ) * m_x + _mat.get( 1, 1 ) * m_y + _mat.get( 1, 2 ) * m_z + _mat.get( 1, 3 );
		double z = _mat.get( 2, 0 ) * m_x + _mat.get( 2, 1 ) * m_y + _mat.get( 2, 2 ) * m_z + _mat.get( 2, 3 );
		double w = _mat.get( 3, 0 ) * m_x + _mat.get( 3, 1 ) * m_y + _mat.get( 3, 2 ) * m_z + _mat.get( 3, 3 );
		double f = Math.abs( _mat.scalalize() * m_f );
		return Point.createXYZTF( x / w, y / w, z / w, m_t, f );
	}

	/**
	 * 点列の各点間の距離合計を計測します。
	 * @param _points 点列
	 * @return 距離合計
	 * @throws IllegalArgumentException 引数がnullである場合
	 */
	public static double length( Point[] _points ) {
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null" );
		}

		double length = 0;

		for ( int i = 1; i < _points.length; ++i ) {
			length += _points[i - 1].distance( _points[i] );
		}

		return length;
	}

	/**
	 * このファジィ点の補集合を返します。
	 * @return 点の補集合
	 */
	public Point.Complement getComplement() {
		return new Point.Complement( this );
	}

	@Override
	public FuzzySetType getFuzzySetType() {
		return FuzzySetType.SINGLE;
	}
	
	@Override
	protected Point support( Vector _vector ) {
		return this;
	}

	/**
	 * この Point と指定された Object が等しいかどうかを比較します。
	 * @param obj この Point と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * x座標、y座標、z座標、時刻、位置の曖昧さ(ファジネス)がまったく同じ Point である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Point other = (Point) obj;
		if ( this.m_x != other.m_x ) {
			return false;
		}
		if ( this.m_y != other.m_y ) {
			return false;
		}
		if ( this.m_z != other.m_z ) {
			return false;
		}
		if ( Double.isNaN( this.m_t ) ) {
			if ( !Double.isNaN( other.m_t ) ) {
				return false;
			}
		} else if ( this.m_t != other.m_t ) {
			return false;
		}

		return this.m_f == other.m_f;
	}

	/**
	 * この Point のハッシュコードを返します。
	 * @return この Point のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + (int) ( Double.doubleToLongBits( this.m_x ) ^ ( Double.doubleToLongBits( this.m_x ) >>> 32 ) );
		hash = 67 * hash + (int) ( Double.doubleToLongBits( this.m_y ) ^ ( Double.doubleToLongBits( this.m_y ) >>> 32 ) );
		hash = 67 * hash + (int) ( Double.doubleToLongBits( this.m_z ) ^ ( Double.doubleToLongBits( this.m_z ) >>> 32 ) );
		hash = 67 * hash + (int) ( Double.doubleToLongBits( this.m_t ) ^ ( Double.doubleToLongBits( this.m_t ) >>> 32 ) );
		hash = 67 * hash + (int) ( Double.doubleToLongBits( this.m_f ) ^ ( Double.doubleToLongBits( this.m_f ) >>> 32 ) );
		return hash;
	}

	/**
	 * この Point の文字列表現を返します。
	 * @return x座標、y座標、z座標、時刻、位置の曖昧さ(ファジネス)を表す String
	 */
	@Override
	public String toString() {
		return String.format( "x:%.3f y:%.3f z:%.3f t:%.3f f:%.3f", m_x, m_y, m_z, m_t, m_f );
	}

	/**
	 * ファジィ点を生成します。
	 * @param _x X座標
	 * @param _y Y座標
	 * @param _z Z座標
	 * @param _t 時刻
	 * @param _f 位置のあいまいさ
	 */
	private Point( double _x, double _y, double _z, double _t, double _f ) {
		m_x = _x;
		m_y = _y;
		m_z = _z;
		m_t = _t;
		m_f = _f;
	}
	/** X座標 */
	private final double m_x;
	/** Y座標 */
	private final double m_y;
	/** Z座標 */
	private final double m_z;
	/** 時刻 */
	private final double m_t;
	/** 位置のあいまいさ（ファジネス） */
	private final double m_f;
}
