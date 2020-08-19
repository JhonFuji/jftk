package jp.sagalab.jftk.recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate;

/**
 * 閉自由曲線・開自由曲線の認識結果を表すクラスです。
 * @author nakajima
 */
public class FreeCurveRecognitionResult extends RecognitionResult{

	/**
	 * 閉自由曲線・開自由曲線の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	static FreeCurveRecognitionResult create( RoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		return new FreeCurveRecognitionResult( _curve, _type, _gradeList );
	}

	@Override
	public List<SnappingCandidate> getSnappingCandidateList() {
		return new ArrayList<SnappingCandidate>( 0 );
	}

	@Override
	public List<SnappingCandidate> getPartitionSnappingCandidateList( Point _start, Point _end ) {
		return new ArrayList<SnappingCandidate>( 0 );
	}

	@Override
	public List<SnappingCandidate> getEdgePointsSnappingCandidateList() {
		return new ArrayList<SnappingCandidate>( 0 );
	}

	/**
	 * 閉自由曲線・開自由曲線の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	private FreeCurveRecognitionResult( RoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		super( _curve, _type, _gradeList );
	}
}
