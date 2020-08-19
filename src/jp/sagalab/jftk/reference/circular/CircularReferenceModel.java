package jp.sagalab.jftk.reference.circular;

import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.reference.ReferenceModel;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 円形レファレンスモデルを表すクラスです。
 *
 * @author Akira Nishikawa
 */
public class CircularReferenceModel extends ReferenceModel implements NQuarterable {

	/**
	 * 円形レファレンスモデルを生成します。
	 * @param _curve 仮説ファジィ曲線モデル
	 * @return 円形レファレンスモデル
	 * @throws IllegalArgumentException 仮説ファジィ曲線モデルがnullの場合
	 */
	static CircularReferenceModel create( QuadraticBezierCurve _curve ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}
		return new CircularReferenceModel( _curve, NQuartersType.GENERAL );
	}

	/**
	 * 円形レファレンスモデルを曲線の簡約型を指定して生成します。
	 * @param _curve 仮説ファジィ曲線モデル
	 * @param _redutionType 曲線の簡約型
	 * @return 円形レファレンスモデル
	 * @throws IllegalArgumentException 仮説ファジィ曲線モデルがnullの場合
	 * @throws IllegalArgumentException 曲線の簡約型がnullの場合
	 */
	static CircularReferenceModel create( QuadraticBezierCurve _curve, NQuartersType _reductionType ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}
		if ( _reductionType == null ) {
			throw new IllegalArgumentException( "_reduction type is null" );
		}
		return new CircularReferenceModel( _curve, _reductionType );
	}

	@Override
	public CircularReferenceModel transform( TransformMatrix _mat ) {
		QuadraticBezierCurve curve = getCurve();
		QuadraticBezierCurve transformed = curve.transform( _mat );
		return CircularReferenceModel.create( transformed, getNQuartersType() );
	}

	@Override
	public NQuartersType getNQuartersType() {
		return m_reductionType;
	}

	private CircularReferenceModel( QuadraticBezierCurve _curve, NQuartersType _reductionType ) {
		super( _curve );
		m_reductionType = _reductionType;
	}

	final NQuartersType m_reductionType;
}
