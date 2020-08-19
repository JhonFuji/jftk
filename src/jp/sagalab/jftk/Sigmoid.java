package jp.sagalab.jftk;

/**
 * シグモイド関数を表すクラスです。
 * @author Akira Nishikawa
 */
public class Sigmoid{

	/**
	 * シグモイド関数を生成します。
	 * @param _weight 重み
	 * @param _theta 位相
	 * @return シグモイド関数
	 * @throws IllegalArgumentException 重みが非数か無限大の場合
	 * @throws IllegalArgumentException 位相が非数か無限大の場合
	 */
	public static Sigmoid create( double _weight, double _theta ) {
		// 重みと位相が非数ではないか
		if ( Double.isNaN( _weight ) ) {
			throw new IllegalArgumentException( "_weight is NaN." );
		}
		if ( Double.isNaN( _theta ) ) {
			throw new IllegalArgumentException( "_theta is NaN." );
		}
		// 重みと位相が無限大ではないか
		if ( Double.isInfinite( _weight ) ) {
			throw new IllegalArgumentException( "_weight is Infinite." );
		}
		if ( Double.isInfinite( _theta ) ) {
			throw new IllegalArgumentException( "_theta is Infinite." );
		}
		return new Sigmoid( _weight, _theta );
	}

	/**
	 * 重みを返します。
	 * @return 重み
	 */
	public double weight() {
		return m_weight;
	}

	/**
	 * 位相を返します。
	 * @return 位相
	 */
	public double theta() {
		return m_theta;
	}

	/**
	 * 指定したパラメータで値を導出します。
	 * @param _parameter パラメータ
	 * @return 値
	 */
	public double calculate( double _parameter ) {
		return 1.0 / ( 1.0 + Math.exp( -( m_weight * _parameter + m_theta ) ) );
	}

	/**
	 * この Sigmoid と指定された Object が等しいかどうかを比較します。
	 * @param obj この Sigmoid と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 重み、位相がまったく同じ Sigmoid である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Sigmoid other = (Sigmoid) obj;
		if ( this.m_weight != other.m_weight ) {
			return false;
		}

		return this.m_theta == other.m_theta;
	}

	/**
	 * この Sigmoid のハッシュコードを返します。
	 * @return この Sigmoid のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 61 * hash + (int) ( Double.doubleToLongBits( this.m_weight ) ^ ( Double.doubleToLongBits( this.m_weight ) >>> 32 ) );
		hash = 61 * hash + (int) ( Double.doubleToLongBits( this.m_theta ) ^ ( Double.doubleToLongBits( this.m_theta ) >>> 32 ) );
		return hash;
	}

	/**
	 * この Sigmoid の文字列表現を返します。
	 * @return 重みと位相を表す文字列
	 */
	@Override
	public String toString() {
		return String.format( "w:%.3f t:%.3f", m_weight, m_theta );
	}

	private Sigmoid( double _weight, double _theta ) {
		m_weight = _weight;
		m_theta = _theta;
	}

	/** 重み */
	private final double m_weight;
	/** 位相 */
	private final double m_theta;
}
