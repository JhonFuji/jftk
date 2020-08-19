package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.OutOfRangeException;

/**
 * 線分を表すクラスです。
 * @author Akira Nishikawa
 */
public class Line extends PrimitiveCurve {
	
	/**
	 * 線分を生成します。
	 * @param _start 始点
	 * @param _end 終点
	 */
	protected Line( Point _start, Point _end ) {
		super(Range.zeroToOne());
		
		m_start = _start;
		m_end = _end;
	}
	
	/**
	 * このクラスのコンストラクタ。
	 * @param _start 始点
	 * @param _end 終点
	 * @return このクラスのインスタンス
	 * @throws IllegalArgumentException _startがnullの場合
	 * @throws IllegalArgumentException _endがnullの場合
	 */
	public static Line create( Point _start, Point _end ){
		if(_start == null){
			throw new IllegalArgumentException( "_Start is null" );
		}
		if(_end == null){
			throw new IllegalArgumentException( "_end is null" );
		}
		// _start と _end のファジネスを0にする
		Point start = Point.createXYZTF( _start.x(), _start.y(), _start.z(), _start.time(), 0 );
		Point end = Point.createXYZTF( _end.x(), _end.y(), _end.z(), _end.time(), 0 );
		
		return new Line(start, end );
	}
	
	/**
	 * 指定したパラメータの座標を返します。
	 * @param _parameter パラメータ
	 * @return 座標
	 */
	@Override
	public Point locus( double _parameter ) {
		if(!range().isInner( _parameter)){
			throw new OutOfRangeException("_parameter is out of range");
		}
		return m_start.internalDivision( m_end, _parameter, 1 - _parameter );
	}

	@Override
	public Line invert() {
		return new Line( m_end, m_start );
	}

	@Override
	public Line transform( TransformMatrix _mat ) {
		Point start = start().transform( _mat );
		Point end = end().transform( _mat );
		return new Line( start, end );
	}

	@Override
	public BezierCurve[] convert() {
		Point start = start();
		Point end = end();
		return new BezierCurve[]{
				BezierCurve.create( new Point[]{
					start,
					start.internalDivision( end, 1, 2 ),
					start.internalDivision( end, 2, 1 ),
					end
				}, Range.zeroToOne() )
			};
	}

	@Override
	public boolean isClosed() {
		return false;
	}
	
	/**
	 * 始点を返します。
	 * @return 始点
	 */
	public Point start(){
		return m_start;
	}
	
	/**
	 * 終点を返します。
	 * @return 終点
	 */
	public Point end(){
		return m_end;
	}
	
	@Override
	public double length() {
		return start().distance( end() );
	}

	/**
	 * この Line と指定された Object が等しいかどうかを比較します。 
	 * @param obj この Line と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 始点、終点、閉じているかがまったく同じ Line である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Line other = (Line) obj;
		if ( this.m_start != other.m_start && ( this.m_start == null || !this.m_start.equals( other.m_start ) ) ) {
			return false;
		}
		if ( this.m_end != other.m_end && ( this.m_end == null || !this.m_end.equals( other.m_end ) ) ) {
			return false;
		}
		return true;
	}

	/**
	 * この Line のハッシュコードを返します。 
	 * @return この Line のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + ( this.m_start != null ? this.m_start.hashCode() : 0 );
		hash = 79 * hash + ( this.m_end != null ? this.m_end.hashCode() : 0 );
		return hash;
	}

	/**
	 * この Line の文字列表現を返します。
	 * @return 始点、終点、閉じているか、範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "start:%s end:%s closed:%s %s", m_start, m_end, Boolean.toString( isClosed() ), super.toString() );
	}
	
	/** 始点 */
	private final Point m_start;
	/** 終点 */
	private final Point m_end;
}
