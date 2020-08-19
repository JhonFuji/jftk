package jp.sagalab.jftk.fragmentation;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.curve.ParametricEvaluable.EvaluationType;

/**
 * 区切りフラグメントを表すクラスです。
 * <p>
 * 区切りフラグメントはファジィフラグメンテーション法により区切り単位であると判断されたフラグメントです。<br>
 * また、この区切りフラグメントは同定フラグメント同士の接続関係を表します。
 * そのため本体と呼ばれるファジィ点一つ、接続の足と呼ばれるファジィ点二つをフィールドに持ちます。
 * </p>
 * <p>
 * このクラスはフラグメントの状態をクラスとして表現しています。
 * </p>
 * @author yamaguchi
 */
public class PartitionFragment extends Fragment {
	
	/**
	 * 区切りフラグメントを生成します。
	 * @param _fsc ファジィスプライン曲線
	 * @param _start 始点側の接続点
	 * @param _end 終点側の接続点
	 * @return 区切りフラグメント
	 * @throws IllegalArgumentException ファジィスプライン曲線がnullの場合
	 */
	public static PartitionFragment create( SplineCurve _fsc, Point _start, Point _end ) {
		if ( _fsc == null ) {
			throw new IllegalArgumentException( "_fsc is null." );
		}
		return new PartitionFragment( _fsc, createFocus( _fsc ), _start, _end );
	}

	/**
	 * 区切りフラグメントを生成します。
	 * @param _partitionFragment 区切りフラグメント
	 * @param _start 始点側の接続点
	 * @param _end 終点側の接続点
	 * @return 区切りフラグメント
	 * @throws IllegalArgumentException 区切りフラグメントがnullの場合
	 */
	public static PartitionFragment create( PartitionFragment _partitionFragment, Point _start, Point _end ) {
		if ( _partitionFragment == null ) {
			throw new IllegalArgumentException( "_partitionFragment is null." );
		}
		
		return new PartitionFragment( _partitionFragment.curve(), _partitionFragment.body(), _start, _end );
	}

	/**
	 * 接続の本体を返します。
	 * @return 接続の本体
	 */
	public Point body() {
		return m_body;
	}

	/**
	 * 始点側の接続の足を返します。
	 * @return 始点側の接続の足
	 */
	public Point start() {
		return m_start;
	}

	/**
	 * 終点側の接続の足を返します。
	 * @return 終点側の接続の足
	 */
	public Point end() {
		return m_end;
	}

	/**
	 * この区切りフラグメントと指定された区切りフラグメントを融合します。
	 * @param _other 指定された区切りフラグメント
	 * @param _ratioA 内分比
	 * @param _ratioB 内分比
	 * @return 融合後の区切りフラグメント
	 * @throws IllegalArgumentException 指定された区切りフラグメントがnullの場合
	 */
	public PartitionFragment blend( PartitionFragment _other, double _ratioA, double _ratioB ) {
		if ( _other == null ) {
			throw new IllegalArgumentException( "_other is null" );
		}
		Point body = m_body.internalDivision( _other.m_body, _ratioA, _ratioB );

		// TODO ファジィスプライン曲線の融合が必要か
		return new PartitionFragment( curve(), body, m_start, _other.m_end );
	}
	
	/**
	 * フラグメント列における開始位置の区切りフラグメントであるかどうかを返します。
	 * @return 開始位置にあたる区切りフラグメントであればtrue
	 */
	public boolean isHead(){
		// 始点側の足がnullであり かつ 終点側の足がnullでない 場合に限り
		// 開始位置にあたる区切りフラグメントとなる		
		return ( m_start == null && m_end != null );
	}
	
	/**
	 * フラグメント列における終了位置の区切りフラグメントであるかどうかを返します。
	 * @return 終了位置にあたる区切りフラグメントであればtrue
	 */
	public boolean isTail(){
		// 終点側の足がnullであり かつ 始点側の足がnullでない 場合に限り
		// 終了位置にあたる区切りフラグメントとなる
		return ( m_end == null && m_start != null );
	}

	@Override
	public PartitionFragment invert() {
		return new PartitionFragment( curve().invert(), m_body, m_end, m_start );
	}

	@Override
	public PartitionFragment transform( TransformMatrix _mat ) {
		Point start = null;
		Point end = null;
		if ( m_start != null ) {
			start = m_start.transform( _mat );
		}
		if ( m_end != null ) {
			end = m_end.transform( _mat );
		}
		return new PartitionFragment( curve().transform( _mat ),  m_body.transform( _mat ), start, end );
	}
	
	/**
	 * この PartitionFragment と指定された Object が等しいかどうかを比較します。 
	 * @param obj この PartitionFragment と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 本体、接続の足、ファジィスプライン曲線がまったく同じ PartitionFragment である限りtrue
	 */
	@Override
	public boolean equals(Object obj){
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final PartitionFragment other = (PartitionFragment) obj;
		if ( !m_body.equals( other.m_body ) ) {
			return false;
		}
		if ( !m_start.equals( other.m_start ) ) {
			return false;
		}
		if ( !m_end.equals( other.m_end ) ) {
			return false;
		}

		return super.equals( obj );
	}

	/**
	 * この PartitionFragment のハッシュコードを返します。 
	 * @return この PartitionFragment のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41 * hash + ( this.m_body != null ? this.m_body.hashCode() : 0 );
		hash = 41 * hash + ( this.m_start != null ? this.m_start.hashCode() : 0 );
		hash = 41 * hash + ( this.m_end != null ? this.m_end.hashCode() : 0 );
		return hash;
	}

	/**
	 * この PartitionFragment の文字列表現を返します。
	 * @return 本体、接続の足、ファジィスプライン曲線を表す String
	 */
	@Override
	public String toString(){
		return String.format( "body:%s, start:%s, end:%s, %s",
			m_body.toString(), m_start.toString(), m_end.toString(), super.toString() );
	}
	
		/**
	 * この区切りフラグメントをもとに区切り点を生成します。
	 * 区切り点の座標：フラグメント点列内で最もファジネスが小さい点の座標
	 * 区切り点のファジネス：フラグメント点列の各点のファジネスの平均値
	 * @param _curve ファジィスプライン曲線
	 * @return 区切り点
	 */
	private static Point createFocus( SplineCurve _curve ) {
		Point[] evaluatePoints = _curve.evaluateAll(
			Math.max( 2, (int) Math.ceil( _curve.range().length() / 0.01 ) ), EvaluationType.TIME );

		double x = 0.0;
		double y = 0.0;
		double z = 0.0;
		double sumFuzziness = 0.0;
		double minFuzziness = Double.POSITIVE_INFINITY;
		double length = evaluatePoints.length;
		for ( Point p : evaluatePoints ) {
			if( minFuzziness > p.fuzziness() ){
				x = p.x();
				y = p.y();
				z = p.z();
				minFuzziness = p.fuzziness();
			}
			sumFuzziness += p.fuzziness();
		}

		return Point.createXYZTF(
			x,
			y,
			z,
			Double.NaN,
			sumFuzziness / length );
	}
	
	private PartitionFragment(SplineCurve _fragment, Point _body, Point _start, Point _end ) {
		super(_fragment);
		m_body = _body;
		m_start = _start;
		m_end = _end;
	}
	
	/** 本体 */
	private final Point m_body;
	/** 始点側の接続の足。 */
	private final Point m_start;
	/** 終点側の接続の足。 */
	private final Point m_end;
}