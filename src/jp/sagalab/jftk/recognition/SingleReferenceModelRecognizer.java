package jp.sagalab.jftk.recognition;

import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.reference.ReferenceModel;
import jp.sagalab.jftk.reference.ReferenceModelGenerator;
import jp.sagalab.jftk.reference.circular.CircularReferenceModelGenerator;
import jp.sagalab.jftk.reference.elliptic.EllipticReferenceModelGenerator;
import jp.sagalab.jftk.reference.linear.LinearReferenceModelGenerator;

/**
 * 単一のレファレンスモデルを用いて幾何曲線認識を行います。
 * @author miwa
 */
public class SingleReferenceModelRecognizer extends PrimitiveCurveRecognizer {

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _fmpsNum FMPSの点数
	 * @return インスタンス
	 */
	public static SingleReferenceModelRecognizer create( int _fmpsNum ) {
		return new SingleReferenceModelRecognizer( _fmpsNum );
	}

	@Override
	ReferenceModel[] constructReductionModels( PrimitiveType _type, SplineCurve _fsc ) {
	// リファレンスモデル構築
		ReferenceModelGenerator generator;
		ReferenceModel[] models;
		switch ( _type ) {
			case CIRCULAR_ARC:
				generator = CircularReferenceModelGenerator.create();
				models = new ReferenceModel[]{ generator.generateQuarterModel( _fsc ),
					generator.generateHalfModel( _fsc ),
					generator.generateThreeQuartersModel( _fsc ) };
				return models;
			case ELLIPTIC_ARC:
				generator = EllipticReferenceModelGenerator.create();
				models = new ReferenceModel[]{ generator.generateQuarterModel( _fsc ),
					generator.generateHalfModel( _fsc ),
					generator.generateThreeQuartersModel( _fsc ) };
				return models;
			default:
				throw new IllegalArgumentException("円弧でも楕円弧でもない");
		}	
	}

	@Override
	ReferenceModel[] constructReferenceModels( SplineCurve _fsc ) {
		ReferenceModelGenerator linearGenerator = LinearReferenceModelGenerator.create();
		ReferenceModelGenerator circularGenerator = CircularReferenceModelGenerator.create();
		ReferenceModelGenerator ellipticGenerator = EllipticReferenceModelGenerator.create();

		ReferenceModel[] models = new ReferenceModel[]{ linearGenerator.generateGeneralModel( _fsc ),
			circularGenerator.generateGeneralModel( _fsc ),
			ellipticGenerator.generateGeneralModel( _fsc ) };
		return models;
	}

	private SingleReferenceModelRecognizer( int _fmpsNum ) {
		super( _fmpsNum );
	}
}
