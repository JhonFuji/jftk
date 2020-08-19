package jp.sagalab.jftk.fuzzysplinecurve;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.SplineCurve;

/**
 * ファジィスプライン曲線(FuzzySplineCurve, FSC)を生成するためのインタフェースです。
 * @author nagase
 */
public interface FSCCreater {
	
	/**
	 * 指定された入力点列を元にファジィスプライン曲線を生成します。
	 * @param _points 入力点列
	 * @return ファジィスプライン曲線
	 */
	public SplineCurve createFSC(Point[] _points );
}
