package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.CurveConvertible;
import jp.sagalab.jftk.curve.Invertible;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.Transformable;

/**
 * 幾何曲線を定義する抽象クラスです。
 * @author Akira Nishikawa
 */
public abstract class PrimitiveCurve implements Transformable<PrimitiveCurve>,
	CurveConvertible<BezierCurve>, Invertible<PrimitiveCurve>{

	protected PrimitiveCurve(Range _range){
		m_range = _range;
	}
	
	/**
	 * 指定したパラメータ地点の座標を返します。
	 * @param _t パラメータ
	 * @return 座標
	 * @throws OutOfRangeException 指定したパラメータが曲線の範囲外の場合
	 */
	public abstract Point locus(double _t ) throws OutOfRangeException;

	/**
	 * 閉曲線かどうかを返します。
	 * @return 閉曲線かどうか
	 */
	public abstract boolean isClosed();

	/**
	 * 曲線の長さを返します。
	 * @return 曲線の長さ
	 */
	public abstract double length();

	/**
	 * この曲線をベジェ曲線列へ変換します。
	 * @return ベジェ曲線列
	 */
	@Override
	public abstract BezierCurve[] convert();

	/**
	 * 曲線の範囲を返します。
	 * @return 曲線の範囲
	 */
	public Range range(){
		return m_range;
	}

	/** 曲線の範囲 */
	private final Range m_range;
}
