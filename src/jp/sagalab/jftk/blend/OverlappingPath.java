package jp.sagalab.jftk.blend;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 重複経路を表すクラスです。
 * @author Akira Nishikawa
 */
public class OverlappingPath {

	/**
	 * 位置を表すクラスです。
	 */
	public static class Position {

		/**
		 * 位置を生成します。
		 * @param _x x位置
		 * @param _y y位置
		 */
		public Position( int _x, int _y ) {
			m_x = _x;
			m_y = _y;
		}

		/**
		 * x位置を返します。
		 * @return x位置
		 */
		public int x() {
			return m_x;
		}

		/**
		 * y位置を返します。
		 * @return y位置
		 */
		public int y() {
			return m_y;
		}

		/**
	 * この Position と指定された Object が等しいかどうかを比較します。
	 * @param obj この Position と比較される Object
	 * @return 指定された Object が、このオブジェクトとx位置、y位置がまったく同じ Position である限りtrue
	 */
		@Override
		public boolean equals( Object obj ) {
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			final Position other = (Position) obj;
			if ( this.m_x != other.m_x ) {
				return false;
			}

			return this.m_y == other.m_y;
		}

		/**
		 * この Position のハッシュコードを返します。
		 * @return この Position のハッシュコード
		 */
		@Override
		public int hashCode() {
			int hash = 3;
			hash = 53 * hash + this.m_x;
			hash = 53 * hash + this.m_y;
			return hash;
		}

		/**
		 * この Position の文字列表現を返します。
		 * @return x位置、y位置を表す String
		 */
		@Override
		public String toString() {
			return String.format( "x:%d y:%d", m_x, m_y );
		}

		/** X位置 */
		private final int m_x;
		/** Y位置 */
		private final int m_y;
	}

	/**
	 * DOLコンパレータを表すクラスです。
	 */
	public static class DOLComparator implements Comparator<OverlappingPath> {

		/**
		 * 順序付けのために二つの引数を比較します。<br>
		 * 重複経路の重複度を比較します。
		 * @param _o1 重複経路A
		 * @param _o2 重複経路B
		 * @return 重複経路Aが重複経路Bより重複度が小さい場合は負の整数、
		 * 両方が等しい場合は 0、重複経路Aが重複経路Bより重複度大きい場合は正の整数
		 */
		@Override
		public int compare( OverlappingPath _o1, OverlappingPath _o2 ) {
			if ( _o1.dol() < _o2.dol() ) {
				return -1;
			} else if ( _o1.dol() > _o2.dol() ) {
				return 1;
			}
			return 0;
		}
	}
	
	/**
	 * 重複経路を生成します。
	 * @param _path 重複経路
	 * @param _possibility 重複可能性値
	 * @param _timeRatio 重複時間率
	 * @param _lengthRatio 重複長さ率
	 * @return 重複経路
	 * @throws IllegalArgumentException 重複経路がnullの場合
	 * @throws IllegalArgumentException 重複経路にnullが含まれる場合
	 * @throws IllegalArgumentException 重複経路の長さが0の場合
	 * @throws IllegalArgumentException 重複可能性が0-1の範囲外の場合
	 * @throws IllegalArgumentException 重複可能性がNaNの場合
	 * @throws IllegalArgumentException 重複時間率が0-1の範囲外の場合
	 * @throws IllegalArgumentException 重複時間率がNaNの場合
	 * @throws IllegalArgumentException 重複長さ率が0-1の範囲外の場合
	 * @throws IllegalArgumentException 重複長さ率がNaNの場合
	 */
	public static OverlappingPath create( Position[] _path, double _possibility, double _timeRatio, double _lengthRatio ){
		if ( _path == null ) {
			throw new IllegalArgumentException( "_path is null" );
		}
		if ( Arrays.asList( _path ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_path includ null" );
		}
		if ( _path.length == 0 ) {
			throw new IllegalArgumentException( "_path length is 0" );
		}
		if ( _possibility < 0 || 1 < _possibility ) {
			throw new IllegalArgumentException( "_possibility is less than 0 or more than 1" );
		}
		if ( Double.isNaN( _possibility ) ) {
			throw new IllegalArgumentException( "_possibility is NaN " );
		}
		if ( _timeRatio < 0 || 1 < _timeRatio ) {
			throw new IllegalArgumentException( "_timeRatio is less than 0 or more than 1" );
		}
		if ( Double.isNaN( _timeRatio ) ) {
			throw new IllegalArgumentException( "_timeRatio is NaN " );
		}
		if ( _lengthRatio < 0 || 1 < _lengthRatio ) {
			throw new IllegalArgumentException( "_lengthRatio is less than 0 or more than 1" );
		}
		if ( Double.isNaN( _lengthRatio ) ) {
			throw new IllegalArgumentException( "_lengthRatio is NaN" );
		}
		return new OverlappingPath(_path, _possibility, _timeRatio, _lengthRatio );
	}

	/**
	 * 重複経路を返します。
	 * @return 重複経路
	 */
	public Position[] path() {
		return m_path.clone();
	}

	/**
	 * 重複可能性値を返します。
	 * @return 重複可能性値
	 */
	public double possibility() {
		return m_possibility;
	}

	/**
	 * 重複時間率を返します。
	 * @return 重複時間率
	 */
	public double timeRatio() {
		return m_timeRatio;
	}

	/**
	 * 重複長さ率を返します。
	 * @return 重複長さ率
	 */
	public double lengthRatio() {
		return m_lengthRatio;
	}

	/**
	 * 重複度を返します。
	 * TODO 現時点ではとりあえず重複可能性値と重複長さ率の平均値を返す
	 * @return 重複度
	 */
	public double dol() {
		return ( m_possibility + m_lengthRatio ) / 2.0;
	}

	/**
	 * この OverlappingPath と指定された Object が等しいかどうかを比較します。
	 * @param obj この OverlappingPath と比較される Object
	 * @return 指定された Object が、このオブジェクトと重複経路、重複可能性値、
	 * 重複時間率、重複長さ率がまったく同じ OverlappingPath である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final OverlappingPath other = (OverlappingPath) obj;
		if ( !Arrays.deepEquals( this.m_path, other.m_path ) ) {
			return false;
		}
		if ( this.m_possibility != other.m_possibility ) {
			return false;
		}
		if ( this.m_timeRatio != other.m_timeRatio ) {
			return false;
		}
		return ( this.m_lengthRatio == other.m_lengthRatio );
	}

	/**
	 * この OverlappingPath のハッシュコードを返します。
	 * @return この OverlappingPath のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 29 * hash + Arrays.deepHashCode( this.m_path );
		hash = 29 * hash + (int) ( Double.doubleToLongBits( this.m_possibility ) ^ ( Double.doubleToLongBits( this.m_possibility ) >>> 32 ) );
		hash = 29 * hash + (int) ( Double.doubleToLongBits( this.m_timeRatio ) ^ ( Double.doubleToLongBits( this.m_timeRatio ) >>> 32 ) );
		hash = 29 * hash + (int) ( Double.doubleToLongBits( this.m_lengthRatio ) ^ ( Double.doubleToLongBits( this.m_lengthRatio ) >>> 32 ) );
		return hash;
	}

	/**
	 * この Matrix の文字列表現を返します。
	 * @return 重複経路、重複可能性値、重複時間率、重複長さ率を表す String
	 */
	@Override
	public String toString() {
		return String.format( "path:%s possibility:%f timeRatio:%f lengthRatio:%f", Arrays.toString( m_path ), m_possibility, m_timeRatio, m_lengthRatio );
	}

	/**
	 * 重複経路のコンストラクタ
	 * @param _path 重複経路
	 * @param _possibility 重複可能性値
	 * @param _timeRatio 重複時間率
	 * @param _lengthRatio 重複長さ率
	 */
	private OverlappingPath( Position[] _path, double _possibility, double _timeRatio, double _lengthRatio ) {
		m_path = _path;
		m_possibility = _possibility;
		m_timeRatio = _timeRatio;
		m_lengthRatio = _lengthRatio;
	}

	/** 重複経路 */
	private final Position[] m_path;
	/** 重複可能性値 */
	private final double m_possibility;
	/** 重複時間率 */
	private final double m_timeRatio;
	/** 重複長さ率 */
	private final double m_lengthRatio;
}