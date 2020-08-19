package jp.sagalab.jftk.recognition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.rough.LinearRoughCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate.SnappingPointType;

/**
 * 線分の認識結果を表すクラスです。
 * @author nakajima
 */
public class LinearRecognitionResult extends RecognitionResult {

	/**
	 * 線分の認識結果を生成します。
	 * @param _curve 曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	static LinearRecognitionResult create(
		RoughCurve _curve, PrimitiveType _type, Map<PrimitiveType, Double> _gradeList ) {
		return new LinearRecognitionResult(_curve, _type, _gradeList );
	}

	@Override
	public List<SnappingCandidate> getSnappingCandidateList() {
		return decideSnappingCandidateLists( getCurve() );
	}
	
	@Override
	public List<SnappingCandidate> getPartitionSnappingCandidateList( Point _start, Point _end ) {
		SnappingCandidate partitionPoints =  SnappingCandidate.create( 
			new Point[]{_start,_end}, 
			new SnappingPointType[]{ SnappingPointType.PARTITION, SnappingPointType.PARTITION },
			null );
		return Collections.singletonList( partitionPoints );
	}

	@Override
	public List<SnappingCandidate> getEdgePointsSnappingCandidateList() {
		LinearRoughCurve curve = (LinearRoughCurve) getCurve();
		QuadraticBezierCurve bezierCurve = (QuadraticBezierCurve) curve.getCurve( );

		SnappingCandidate edgePoints =  SnappingCandidate.create(
			new Point[]{bezierCurve.evaluateAtStart(),bezierCurve.evaluateAtEnd()},
			new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER },
			null );
		return Collections.singletonList( edgePoints );
	}
	/**
	 * 特徴点列を決定する。
	 * @param _curve ラフ曲線
	 * @return 特徴点列
	 */
	private static List<SnappingCandidate> decideSnappingCandidateLists( RoughCurve _curve ) {
		// スナッピング前の点列
		SnappingCandidate edgePair = SnappingCandidate.create(
			new Point[]{ _curve.getStart(), _curve.getEnd() },
			new SnappingPointType[]{ SnappingPointType.EDGE, SnappingPointType.EDGE },
			null );
		return Collections.singletonList( edgePair );
	}

	/**
	 * 線分の認識結果を生成します。
	 * @param _curve 曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	private LinearRecognitionResult(
		RoughCurve _curve, PrimitiveType _type, Map<PrimitiveType, Double> _gradeList ) {
		super( _curve, _type, _gradeList );
	}
}
