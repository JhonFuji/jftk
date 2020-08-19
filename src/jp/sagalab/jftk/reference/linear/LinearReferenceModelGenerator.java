package jp.sagalab.jftk.reference.linear;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.reference.ReferenceModel;
import jp.sagalab.jftk.reference.ReferenceModelGenerator;

/**
 * 線形リファレンスモデルを生成するためのクラスです。
 * @author Akira Nishikawa
 */
public class LinearReferenceModelGenerator implements ReferenceModelGenerator {

	/**
	 * このクラスのインスタンスを生成します。
	 * @return このクラスのインスタンス
	 */
	public static LinearReferenceModelGenerator create(){
		return new LinearReferenceModelGenerator();
	}

	/**
	 * 与えられたパラメトリック曲線を用いて線形リファレンスモデルを生成します。
	 * <p>
	 * パラメトリック曲線の始点と終点を用います。
	 * </p>
	 * @param _curve パラメトリック曲線
	 * @return 線形リファレンスモデル
	 */
	@Override
	public LinearReferenceModel generateGeneralModel( ParametricCurve _curve ) {
		Point start = _curve.evaluateAtStart();
		Point end = _curve.evaluateAtEnd();
		Point mid = start.internalDivision( end, 1, 1 );
		return LinearReferenceModel.create(
			 QuadraticBezierCurve.create(	start, mid, end, 0, Range.zeroToOne() ) );
	}

	@Override
	public ReferenceModel generateQuarterModel( ParametricCurve _curve ) {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ReferenceModel generateHalfModel( ParametricCurve _curve ) {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ReferenceModel generateThreeQuartersModel( ParametricCurve _curve ) {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}
	
	private LinearReferenceModelGenerator() {
	}
}
