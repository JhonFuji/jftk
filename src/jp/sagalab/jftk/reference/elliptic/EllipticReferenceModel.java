package jp.sagalab.jftk.reference.elliptic;

import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.reference.ReferenceModel;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 楕円形レファレンスモデルを表すクラスです。
 *
 * @author Akira Nishikawa
 */
public class EllipticReferenceModel extends ReferenceModel implements NQuarterable{

	/**
	 * 楕円形レファレンスモデルを生成します。
	 * @param _curve 仮説ファジィモデルを構成する曲線
	 * @return 楕円形レファレンスモデル	 
	 * @throws IllegalArgumentException 仮説ファジィモデルを構成する曲線がnullの場合
	 */
	static EllipticReferenceModel create( QuadraticBezierCurve _curve ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curves is null" );
		}
		return new EllipticReferenceModel( _curve, NQuartersType.GENERAL );
	}

	/**
	 * 楕円形レファレンスモデルを生成します．
	 * @param _curve 仮説ファジィモデルを構成する曲線
	 * @param _redutionType 曲線の簡約型
	 * @return 楕円形レファレンスモデル
	 * @throws IllegalArgumentException 仮説ファジィモデルを構成する曲線がnullの場合
	 * @throws IllegalArgumentException 曲線の簡約型がnullの場合
	 */
	static EllipticReferenceModel create( QuadraticBezierCurve _curve, NQuartersType _redutionType ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curves is null" );
		}
		if ( _redutionType == null ) {
			throw new IllegalArgumentException( "_reductionType is null" );
		}
		return new EllipticReferenceModel( _curve, _redutionType );
	}

	@Override
	public EllipticReferenceModel transform( TransformMatrix _mat ) {
		QuadraticBezierCurve curve = getCurve();
		QuadraticBezierCurve transformed = curve.transform( _mat );
		return EllipticReferenceModel.create( transformed, getNQuartersType() );
	}

	@Override
	public NQuartersType getNQuartersType() {
		return m_reductionType;
	}

	private EllipticReferenceModel( QuadraticBezierCurve _curve, NQuartersType _reductionType ) {
		super( _curve );
		m_reductionType = _reductionType;
	}

	/** 簡約型 */
	final NQuartersType m_reductionType;
}
