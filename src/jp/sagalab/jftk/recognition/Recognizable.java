package jp.sagalab.jftk.recognition;

import java.util.Map;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.fragmentation.IdentificationFragment;

/**
 * 幾何曲線を認識を行うためのインタフェースです。
 * <p>
 * FSCIでは雑に描画するほど単純な幾何曲線を選択するという戦略の基、幾何曲線認識を行います。
 * これにより書き手は、描画の雑さを制御することで幾何曲線をかき分けることができます。<br>
 * この戦略を表現した規則をファジィ推論規則と呼びます。
 * ファジィ推論規則の戦略自体を変更することはありませんが、{@link FuzzyRule} で定義される推論規則を
 * 用いることで各幾何曲線の認識率を調節することができます。
 * </p>
 * @author nakajima
 */
public interface Recognizable {

	/**
	 * 指定されたファジィスプライン曲線を幾何曲線へ認識します。
	 * @param _identificationFragment 同定単位フラグメント
	 * @param _fsc 延長後のファジィスプライン曲線
	 * @param _rule 推論規則
	 * @return 認識結果
	 */
	public abstract RecognitionResult recognize( IdentificationFragment _identificationFragment, 
		SplineCurve _fsc, Map<String, Sigmoid> _rule );
}
