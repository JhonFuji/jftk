package jp.sagalab.jftk.shaper.snapper;

import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.fragmentation.Fragment;
import jp.sagalab.jftk.recognition.RecognitionResult;
import jp.sagalab.jftk.shaper.ShapedResult;

/**
 * 認識結果に対してスナッピングを行うためのインタフェースです。
 * 認識が終わった段階では、曲線は位置的に曖昧な状態です。
 * それをクリスプな曲線に整形します。
 * @author miwa
 */
public interface FuzzyObjectSnapper{

	/**
	 * 認識結果をスナッピングします。
	 * @param _recognition スナッピング対象となる認識結果
	 * @param _gridSpace スナッピングに用いるグリッド
	 * @param _fsc 認識元のファジィスプライン曲線
	 * @return スナッピング結果
	 */
	public ShapedResult snap( RecognitionResult _recognition, GridSpace _gridSpace, SplineCurve _fsc );

	/**
	 * 認識結果全体をスナッピングします。
	 * @param _recognitions スナッピング対象となる認識結果群
	 * @param _fragments スナッピングに用いるグリッド群
	 * @param _gridSpace 格子点が属すグリッド空間
	 * @return スナッピング結果
	 */
	public Map<Fragment, ShapedResult> snap( Map<Fragment,RecognitionResult> _recognitions, List<Fragment> _fragments,GridSpace _gridSpace);
}
