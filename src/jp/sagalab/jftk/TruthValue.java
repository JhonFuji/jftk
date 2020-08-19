package jp.sagalab.jftk;

import jp.sagalab.jftk.curve.OutOfRangeException;

/**
 * 区間真理値を表すクラスです。
 * @author Akira Nishikawa
 */
public class TruthValue {
	
	/**
	 * 区間真理値を生成します。
	 * @param _necessity 必然性値
	 * @param _possibility 可能性値
	 * @return 区間真理値
	 * @throws IllegalArgumentException 必然性値、可能性値にNaNが指定された場合
	 * @throws IllegalArgumentException 可能性値が必然性値より小さい場合
	 * @throws OutOfRangeException 必然性値が０より小さい場合、または可能性値が１より大きい場合
	 */
	public static TruthValue create( double _necessity, double _possibility ){
		// 非数のチェック
		if ( Double.isNaN( _necessity ) ) {
			throw new IllegalArgumentException("Necessity is NaN.");
		}
		if ( Double.isNaN( _possibility ) ) {
			throw new IllegalArgumentException("Possobility is NaN.");
		}
		// 値の範囲チェック
		if ( _necessity < 0 ) {
			throw new OutOfRangeException("Necessity less than 0.");
		}
		if ( 1 < _possibility ) {
			throw new OutOfRangeException("Possibility more than 1.");
		}
		// 必然性値と可能性値の大小関係チェック
		if ( _possibility < _necessity ) {
			throw new IllegalArgumentException("Possibility less than Necessity.");
		}
		return new TruthValue( _necessity, _possibility );
	}
	
	/**
	 * 必然性値を返します。
	 * @return 必然性値
	 */
	public double necessity() {
		return m_nec;
	}

	/**
	 * 可能性値を返します。
	 * @return 可能性値
	 */
	public double possibility() {
		return m_pos;
	}

	/**
	 * この TruthValue と指定された Object が等しいかどうかを比較します。
	 * @param obj この TruthValue と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 必然性値、可能性値がまったく同じ TruthValue である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final TruthValue other = (TruthValue) obj;
		if ( this.m_nec != other.m_nec ) {
			return false;
		}

		return this.m_pos == other.m_pos;
	}

	/**
	 * この TruthValue のハッシュコードを返します。
	 * @return この TruthValue のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + (int) ( Double.doubleToLongBits( this.m_nec ) ^ ( Double.doubleToLongBits( this.m_nec ) >>> 32 ) );
		hash = 53 * hash + (int) ( Double.doubleToLongBits( this.m_pos ) ^ ( Double.doubleToLongBits( this.m_pos ) >>> 32 ) );
		return hash;
	}

	/**
	 * この TruthValue の文字列表現を返します。
	 * @return 必然性値、可能性値を表す String
	 */
	@Override
	public String toString() {
		return String.format( "n:%.3f p:%.3f", m_nec, m_pos );
	}
	
	private TruthValue( double _necessity, double _possibility ) {		
		m_nec = _necessity;
		m_pos = _possibility;
	}

	/** 必然性値 */
	private final double m_nec;
	/** 可能性値 */
	private final double m_pos;
}
