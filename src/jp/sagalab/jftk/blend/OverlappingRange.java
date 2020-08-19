package jp.sagalab.jftk.blend;

import java.util.Arrays;
import jp.sagalab.jftk.curve.Range;
import java.util.Comparator;
import jp.sagalab.jftk.Sigmoid;

/**
 * 重複範囲を表すクラスです。
 * @author kaneko
 */
public class OverlappingRange {

	/**
	 * 形状的DOLコンパレータを表すクラスです。
	 */
	public static class FiguralDOLComparator implements Comparator<OverlappingRange> {		
		/**
		 * 形状的DOLコンパレータを生成します。
		 * @param _figuralTrue 言語的真理値「真」
		 * @param _lengthTrue 言語的真理値「偽」
		 */
		public FiguralDOLComparator( Sigmoid _figuralTrue, Sigmoid _lengthTrue ) {
			m_figuralTrue = _figuralTrue;
			m_lengthTrue = _lengthTrue;
		}
		/**
		 * 順序付けのために二つの引数を比較します。<br>
		 * 重複経路の重複度を比較します。
		 * @param _o1 重複経路A
		 * @param _o2 重複経路B
		 * @return 重複経路Aが重複経路Bより重複度が小さい場合は負の整数、
		 * 両方が等しい場合は 0、重複経路Aが重複経路Bより重複度大きい場合は正の整数
		 */
		@Override
		public int compare( OverlappingRange _o1, OverlappingRange _o2 ) {
			double dol1 = _o1.figuralDOL( m_figuralTrue, m_lengthTrue );
			double dol2 = _o2.figuralDOL( m_figuralTrue, m_lengthTrue );
			if ( dol1 < dol2 ) {
				return -1;
			} else if ( dol1 > dol2 ) {
				return 1;
			}
			return 0;
		}
		/** 言語的真理値「真」 */
		private final Sigmoid m_figuralTrue;
		/** 言語的真理値「偽」 */
		private final Sigmoid m_lengthTrue;
	}

	/**
	 * 運動的DOLコンパレータを表すクラスです。
	 */
	public static class KineticDOLComparator implements Comparator<OverlappingRange> {

		/**
		 * 運動的DOLコンパレータを生成します。
		 * @param _figuralTrue 言語的真理値「真」
		 * @param _kineticTrue 言語的真理値「偽」
		 */
		public KineticDOLComparator( Sigmoid _figuralTrue, Sigmoid _kineticTrue ) {
			m_figuralTrue = _figuralTrue;
			m_kineticTrue = _kineticTrue;
		}

		/**
		 * 順序付けのために二つの引数を比較します。<br>
		 * 重複経路の重複度を比較します。
		 * @param _o1 重複経路A
		 * @param _o2 重複経路B
		 * @return 重複経路Aが重複経路Bより重複度が小さい場合は負の整数、
		 * 両方が等しい場合は 0、重複経路Aが重複経路Bより重複度大きい場合は正の整数
		 */
		@Override
		public int compare( OverlappingRange _o1, OverlappingRange _o2 ) {
			double dol1 = _o1.kineticDOL( m_figuralTrue, m_kineticTrue );
			double dol2 = _o2.kineticDOL( m_figuralTrue, m_kineticTrue );
			if ( dol1 < dol2 ) {
				return -1;
			} else if ( dol1 > dol2 ) {
				return 1;
			}
			return 0;
		}
		/** 言語的真理値「真」 */
		private final Sigmoid m_figuralTrue;
		/** 言語的真理値「偽」 */
		private final Sigmoid m_kineticTrue;
	}
	
	/**
	 * 重複範囲を生成します。
	 * @param _existedRange 既存曲線のレンジ
	 * @param _existedKnots 既存曲線の節点
	 * @param _overlappedRange 重ね書き曲線のレンジ
	 * @param _overlappedKnots 重ね書き曲線の節点
	 * @param _figuralPos 形状的重複度
	 * @param _kineticRatio 運動的重複率
	 * @param _lengthRatio 長さ重複率
	 * @return 重複範囲
	 * @throws IllegalArgumentException 既存曲線のレンジがnullの場合
	 * @throws IllegalArgumentException 既存曲線の節点がnullである、またはInf,NaNが含まれる場合、もしくは時系列が不正な場合
	 * @throws IllegalArgumentException 重ね書き曲線のレンジがnullの場合
	 * @throws IllegalArgumentException 重ね書き曲線の節点がnullである、またはInf,NaNが含まれる場合、もしくは時系列が不正な場合
	 * @throws IllegalArgumentException 形状的重複度が0-1の範囲外の場合
	 * @throws IllegalArgumentException 運動的重複率が0-1の範囲外の場合
	 * @throws IllegalArgumentException 長さ重複率が0-1の範囲外の場合
	 */
	public static OverlappingRange create(Range _existedRange, double[] _existedKnots,
		Range _overlappedRange, double[] _overlappedKnots,
		double _figuralPos, double _kineticRatio, double _lengthRatio){
		
		if ( _existedRange == null ) {
			throw new IllegalArgumentException( "_existedRange is null" );
		}
		if ( _existedKnots == null ) {
			throw new IllegalArgumentException( "_existedKnots is null" );
		}
		// 節点系列のチェック
		double pre = _existedKnots[ 0 ];
		for ( double d : _existedKnots ) {
			if ( Double.isInfinite( d ) || Double.isNaN( d ) ) {
				throw new IllegalArgumentException( "_existedKnots is included in infinity or NaN." );
			}
			if ( d < pre ) {
				throw new IllegalArgumentException( "There are counter flowed _existedKnots." );
			}
			pre = d;
		}
		if ( _overlappedRange == null ) {
			throw new IllegalArgumentException( "_overlappedRange is null" );
		}
		if ( _overlappedKnots == null ) {
			throw new IllegalArgumentException( "_overlappedKnots is null" );
		}
		// 節点系列のチェック
		pre = _overlappedKnots[ 0 ];
		for ( double d : _overlappedKnots ) {
			if ( Double.isInfinite( d ) || Double.isNaN( d ) ) {
				throw new IllegalArgumentException( "_overlappedKnots is included in infinity or NaN." );
			}
			if ( d < pre ) {
				throw new IllegalArgumentException( "There are counter flowed _overlappedKnots." );
			}
			pre = d;
		}
		if ( _figuralPos < 0 || 1 < _figuralPos ) {
			throw new IllegalArgumentException( "_figuralPos is less than 0 more than 1" );
		}
		if ( _kineticRatio < 0 || 1 < _kineticRatio ) {
			throw new IllegalArgumentException( "_kineticRatio is less than 0 more than 1" );
		}
		if ( _lengthRatio < 0 || 1 < _lengthRatio ) {
			throw new IllegalArgumentException( "_lengthRatio is less than 0 more than 1" );
		}
		return new OverlappingRange(_existedRange, _existedKnots, _overlappedRange, _overlappedKnots, _figuralPos, _kineticRatio, _lengthRatio );
	}
	
	/**
	 * 重複区間を返します。
	 * @return 重複区間
	 */
	public Range[] rangePair() {
		return new Range[]{ m_existedRange, m_overlappedRange };
	}

	/**
	 * 挿入節点列ペアを返します。
	 * @return 挿入節点列ペア
	 */
	public double[][] knotsPair() {
		return new double[][]{ m_existedKnots.clone(), m_overlappedKnots.clone() };
	}

	/**
	 * 形状的重複可能性値を返します。
	 * @return 形状的重複可能性値
	 */
	public double figuralPossibility() {
		return m_figuralPos;
	}

	/**
	 * 運動的重複率を返します。
	 * @return 運動的重複率
	 */
	public double kineticRatio() {
		return m_kineticRatio;
	}

	/**
	 * 長さ重複率を返します。
	 * @return 長さ重複率
	 */
	public double lengthRatio() {
		return m_lengthRatio;
	}

	/**
	 * 形状的重複度を返します。
	 * @param _figuralTrue 形状的重複度における言語的真理値「真」
	 * @param _lengthTrue 形状的重複度における言語的真理値「偽」
	 * @return 形状的重複度
	 */
	public double figuralDOL( Sigmoid _figuralTrue, Sigmoid _lengthTrue ) {
		return Math.min( _figuralTrue.calculate( m_figuralPos ), _lengthTrue.calculate( m_lengthRatio ) );
	}

	/**
	 * 運動的重複度を返します。
	 * @param _figuralTrue 運動的重複度における言語的真理値「真」
	 * @param _kineticTrue 運動的重複度における言語的真理値「偽」
	 * @return 運動的重複度
	 */
	public double kineticDOL( Sigmoid _figuralTrue, Sigmoid _kineticTrue ) {
		return Math.min( _figuralTrue.calculate( m_figuralPos ), _kineticTrue.calculate( m_kineticRatio ) );
	}

	/**
	 * この OverlappingRange と指定された Object が等しいかどうかを比較します。
	 * @param obj この OverlappingRange と比較される Object
	 * @return 指定された Object が、このオブジェクトと重複区間、挿入すべき節点、
	 * 重複可能性値、重複時間率、重複長さ率がまったく同じ OverlappingRange である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final OverlappingRange other = (OverlappingRange) obj;
		if ( !this.m_existedRange.equals( other.m_existedRange ) ) {
			return false;
		}
		if ( !this.m_overlappedRange.equals( other.m_overlappedRange ) ) {
			return false;
		}
		if ( this.m_figuralPos != other.m_figuralPos ) {
			return false;
		}
		if ( this.m_kineticRatio != other.m_kineticRatio ) {
			return false;
		}
		return ( this.m_lengthRatio == other.m_lengthRatio );
	}

	/**
	 * この OverlappingRange のハッシュコードを返します。
	 * @return この OverlappingRange のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 31 * hash + ( this.m_existedRange != null ? this.m_existedRange.hashCode() : 0 );
		hash = 31 * hash + ( this.m_overlappedRange != null ? this.m_overlappedRange.hashCode() : 0 );
		hash = 31 * hash + (int) ( Double.doubleToLongBits( this.m_figuralPos ) ^ ( Double.doubleToLongBits( this.m_figuralPos ) >>> 32 ) );
		hash = 31 * hash + (int) ( Double.doubleToLongBits( this.m_kineticRatio ) ^ ( Double.doubleToLongBits( this.m_kineticRatio ) >>> 32 ) );
		hash = 31 * hash + (int) ( Double.doubleToLongBits( this.m_lengthRatio ) ^ ( Double.doubleToLongBits( this.m_lengthRatio ) >>> 32 ) );
		return hash;
	}

	/**
	 * この OverlappingRange の文字列表現を返します。
	 * @return 重複区間、挿入すべき節点、重複可能性値、重複時間率、重複長さ率を表す String
	 */
	@Override
	public String toString() {
		return String.format( "range0:%s range1:%s possibility:%f timeRatio:%f lengthRatio:%f", m_existedRange, m_overlappedRange, m_figuralPos, m_kineticRatio, m_lengthRatio );
	}

	/**
	 * 重複範囲のコンストラクタ
	 * @param _existedRange 既存曲線のレンジ
	 * @param _existedKnots 既存曲線の節点
	 * @param _overlappedRange 重ね書き曲線のレンジ
	 * @param _overlappedKnots 重ね書き曲線の節点
	 * @param _figuralPos 形状的重複度
	 * @param _kineticRatio 運動的重複率
	 * @param _lengthRatio 長さ重複率
	 */
	private OverlappingRange( Range _existedRange, double[] _existedKnots,
		Range _overlappedRange, double[] _overlappedKnots,
		double _figuralPos, double _kineticRatio, double _lengthRatio ) {
		
		m_existedRange = _existedRange;
		m_existedKnots = _existedKnots;
		m_overlappedRange = _overlappedRange;
		m_overlappedKnots = _overlappedKnots;
		m_figuralPos = _figuralPos;
		m_kineticRatio = _kineticRatio;
		m_lengthRatio = _lengthRatio;
	}

	/** 既存曲線の重複区間 */
	private final Range m_existedRange;
	/** 重ね書き曲線の重複区間 */
	private final Range m_overlappedRange;
	/** 既存曲線に挿入すべき節点 */
	private final double[] m_existedKnots;
	/** 重ね書き曲線に挿入すべき節点 */
	private final double[] m_overlappedKnots;
	/** 重複可能性値 */
	private final double m_figuralPos;
	/** 重複時間率 */
	private final double m_kineticRatio;
	/** 重複長さ率 */
	private final double m_lengthRatio;
}