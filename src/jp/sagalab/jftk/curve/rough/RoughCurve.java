package jp.sagalab.jftk.curve.rough;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;

/**
 * ラフな曲線を定義する抽象クラスです。<br>
 * <p>
 * 幾何曲線認識により認識された曲線は、曲線種は決定していますが曲線の明確な形状は定ま
 * っていません。ラフな曲線はそのような不完全な曲線を表現するための曲線モデルです。
 * </p>
 * <p>
 * ラフな曲線は、同じ曲線種ではありながらその他の曲線情報がことなる曲線群をフィールド
 * に持ちます。このラフな曲線をグリッドスナッピングすることによって、それらの曲線群の
 * 位置の曖昧さ(ファジネス)、形状を考慮した一つの曲線を得ることができます。
 * </p>
 * @author miwa
 */
public abstract class RoughCurve{

	/**
	 * 指定されたパラメーターに対応する点を評価します。
	 * @param _parameter パラメーター
	 * @return 評価点
	 */
	protected Point evaluate( double _parameter ) {
		return m_curve.evaluateAt( _parameter );
	}

	/**
	 * この曲線を構成するパラメトリック曲線を返します。
	 * @return 曲線
	 */
	public ParametricCurve getCurve() {
		return m_curve;
	}

	/**
	 * 始点を返します。
	 * @return 始点
	 */
	public Point getStart() {
		return m_curve.evaluateAtStart();
	}

	/**
	 * 終点を返します。
	 * @return 終点
	 */
	public Point getEnd() {
		return m_curve.evaluateAtEnd();
	}

	/**
	 * この曲線が閉じているかを返します。
	 * @return 閉じている場合はtrue
	 */
	public boolean isClosed() {
		return m_isClosed;
	}

	/**
	 * 指定された番号の曲線をプリミティブ曲線化します。
	 * @return プリミティブ曲線
	 */
	abstract public PrimitiveCurve toPrimitive();

	/**
	 * 曲線をスナッピングし整形します。<br>
	 * スナッピング前の点列とスナッピング後の点列から変換行列を生成出来なかった場合はnullを返します。
	 * TODO 変換行列がなぜ生成できないのかの理由の検討
	 * @param _snapping スナッピング前の特徴点
	 * @param _snapped スナッピング後の特徴点
	 * @param _snappedNormal スナッピング後の法線ベクトル
	 * @return 整形後の曲線を表す二次有理ベジェ曲線
	 * @throws IllegalArgumentException スナッピング前の点列数、またはスナッピング後の点列数が不正の場合
	 */
	abstract public QuadraticBezierCurve toSnappedModel( Point[] _snapping, Point[] _snapped, Vector _snappedNormal );

	/**
	 * 曲線をスナッピングし整形します。<br>
	 * スナッピング前の点列とスナッピング後の点列から変換行列を生成出来なかった場合はnullを返します。
	 * @param _snapping スナッピング前の特徴点
	 * @param _snapped スナッピング後の特徴点
	 * @param _snappedNormal スナッピング後の法線ベクトル
	 * @return 整形後の曲線を表すプリミティブ曲線
	 * @throws IllegalArgumentException スナッピング前の点列数、またはスナッピング後の点列数が不正の場合
	 */
	abstract public PrimitiveCurve toSnappedPrimitive( Point[] _snapping, Point[] _snapped, Vector _snappedNormal );

	/**
	 * この RoughCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この RoughCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと曲線群がまったく同じ RoughCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final RoughCurve other = (RoughCurve) obj;
		if ( !m_curve.equals( other.m_curve ) ) {
			return false;
		}
		return other.m_isClosed == m_isClosed;
	}

	/**
	 * この RoughCurve のハッシュコードを返します。
	 * @return この RoughCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 67 * hash + ( this.m_curve != null ? this.m_curve.hashCode() : 0 );
		hash = 67 * hash + ( this.m_isClosed ? 1 : 0 );
		return hash;
	}

	/**
	 * この RoughCurve の文字列表現を返します。
	 * @return 曲線群を表す String
	 */
	@Override
	public String toString() {
		return String.format( "curves:%s isClosed:%s", m_curve.toString(), Boolean.toString( m_isClosed ) );
	}

	/**
	 * ラフな曲線のインスタンスを生成します。
	 * @param _curve 曲線
	 * @param _isClosed この曲線が閉じているか
	 */
	protected RoughCurve( ParametricCurve _curve, boolean _isClosed ) {
		m_curve = _curve;
		m_isClosed = _isClosed;
	}

	/** 曲線 */
	private final ParametricCurve m_curve;
	/** この曲線が閉じているか */
	private final boolean m_isClosed;
}
