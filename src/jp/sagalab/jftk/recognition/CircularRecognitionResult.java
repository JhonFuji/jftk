package jp.sagalab.jftk.recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.rough.CircularRoughCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate.SnappingPointType;

/**
 * 円・円弧の認識結果を表すクラスです。
 * @author nakajima
 */
public class CircularRecognitionResult extends RecognitionResult{

	/**
	 * 円・円弧の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	static CircularRecognitionResult create( RoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		return new CircularRecognitionResult( _curve, _type, _gradeList );
	}

	/**
	 * 円弧を包含するバウンディングボックスを生成する。
	 * @param _curve 円弧を表す二次有理ベジェ曲線
	 * @param _redReductionType 簡約化された曲線の種類
	 * @return バウンディングボックス
	 */
	public static BoundingBox createBoundingBox( QuadraticBezierCurve _curve, NQuartersType _redReductionType ) {
		// 中心点
		Point center = calcCenter( _curve );
		// 軸点
		Point[] axisPoints = calcBoxAxisPoints( _curve, center, _redReductionType );
		// 角点
		Point[] cornerPoints = calcBoxCornerPoints( _curve, center, axisPoints );

		return BoundingBox.create( center, axisPoints, cornerPoints, _curve.range() );
	}

	@Override
	public List<SnappingCandidate> getSnappingCandidateList() {
		return decideSnappingCandidateLists( getCurve() );
	}

	@Override
	public List<SnappingCandidate> getPartitionSnappingCandidateList( Point _start, Point _end ) {

		CircularRoughCurve curve = (CircularRoughCurve) getCurve();
		QuadraticBezierCurve bezierCurve = (QuadraticBezierCurve) curve.getCurve();

		List<SnappingCandidate> list = new ArrayList<SnappingCandidate>();
		BoundingBox box = null;
		if ( curve.getNQuartersType() != NQuartersType.GENERAL ) {
			box = createBoundingBox( bezierCurve, curve.getNQuartersType() );
		}

		SnappingCandidate edgePoints = SnappingCandidate.create(
			new Point[]{ _start, _end },
			new SnappingPointType[]{ SnappingPointType.PARTITION, SnappingPointType.PARTITION }, box );
		list.add( edgePoints );
		return list;
	}

	@Override
	public List<SnappingCandidate> getEdgePointsSnappingCandidateList() {

		CircularRoughCurve curve = (CircularRoughCurve) getCurve();
		QuadraticBezierCurve bezierCurve = (QuadraticBezierCurve) curve.getCurve();

		List<SnappingCandidate> list = new ArrayList<SnappingCandidate>();
//		BoundingBox box=createBoundingBox( bezierCurve, NQuartersType.NO_REDUCTION);
		SnappingCandidate edgePoints = SnappingCandidate.create(
			new Point[]{ bezierCurve.evaluateAtStart(), bezierCurve.evaluateAtEnd() },
			new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER }, null );
		list.add( edgePoints );
		return list;
	}

	/**
	 * 代表点列を決定します。
	 * @param _curve ラフ曲線
	 * @return 代表点列
	 */
	private static List<SnappingCandidate> decideSnappingCandidateLists( RoughCurve _curve ) {
		CircularRoughCurve roughCurve = (CircularRoughCurve) _curve;

		QuadraticBezierCurve curve = (QuadraticBezierCurve) roughCurve.getCurve();
		NQuartersType reductionType = roughCurve.getNQuartersType();

		Point[] diametricalPoints = searchSnappingDiametralPoints( curve, NQuartersType.GENERAL );
		BoundingBox box = createBoundingBox( curve, NQuartersType.GENERAL );

		// スナッピングする2点の組み合わせの導出
		List<SnappingCandidate> candidateList = configureTwoPointsCombinationList( diametricalPoints, box );

		//リダクションモデルの場合特有のスナッピング候補を追加
		if ( reductionType != NQuartersType.GENERAL ) {
			Point[] diametricalPointsForReductionModel = searchSnappingDiametralPoints( curve, reductionType );
			BoundingBox boxForReducitonModel = createBoundingBox( curve, reductionType );

			List<SnappingCandidate> list = configureTwoPointsCombinationList( diametricalPointsForReductionModel,
				boxForReducitonModel );

			candidateList.addAll( list );
		} else {
			// 始終点2点のペアを追加
			candidateList.add( SnappingCandidate.create(
				new Point[]{ _curve.getStart(), _curve.getEnd() },
				new SnappingPointType[]{ SnappingPointType.EDGE, SnappingPointType.EDGE },
				null ) );
		}
		return candidateList;
	}

	/**
	 * 円弧の交点を求めるために用いる平面を生成します。
	 * @param _curve 二次有理ベジェ曲線
	 * @return 平面
	 */
	private static Plane[] createDiametralPlanes( QuadraticBezierCurve _curve ) {
		Point modelStart = _curve.evaluateAtStart();
		Point modelEnd = _curve.evaluateAtEnd();

		// 曲線上の中点、あるいはその反対側の点
		Vector vector = Vector.createSE( modelStart, modelEnd );
		Point base = modelStart.internalDivision( modelEnd, 1, 1 );
		Point mid;
		if ( Double.isInfinite( 1.0 / vector.length() ) ) {
			mid = base;
		} else {
			Plane plane = Plane.create( base, vector );
			Point[] mids = _curve.intersectionWith( plane );
			mid = mids[0];
		}
		//中心
		Point center = calcCenter( _curve );

		vector = Vector.createSE( center, mid );
		Plane plane = Plane.create( center, vector );
		List<Plane> planes = new ArrayList<Plane>();
		planes.add( plane );
		QuadraticBezierCurve oval = toOval( _curve );
		Point[] intersections = oval.intersectionWith( plane );
		if ( intersections.length > 0 ) {
			vector = Vector.createSE( center, intersections[0] );
			planes.add( Plane.create( center, vector ) );
		}
		return planes.toArray( new Plane[planes.size()] );
	}

	/**
	 * スナッピングに用いる経軸の特徴点を探索します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _reductionType 簡約化された曲線の種類
	 * @return 経軸の特徴点列
	 */
	private static Point[] searchSnappingDiametralPoints( QuadraticBezierCurve _curve, NQuartersType _reductionType ) {
		Point[] diametralPoints;
		switch ( _reductionType ) {
			case QUARTER:
				//径点の算出
				diametralPoints = new Point[2];
				diametralPoints[0] = _curve.evaluateAt( 0 );
				diametralPoints[1] = _curve.evaluateAt( 1 );
				break;
			case HALF:
				// 径点の算出
				diametralPoints = new Point[3];
				diametralPoints[0] = _curve.evaluateAt( 0 );
				diametralPoints[1] = _curve.evaluateAt( 0.5 );
				diametralPoints[2] = _curve.evaluateAt( 1 );
				break;
			case THREE_QUARTERS:
				// 径点の算出
				diametralPoints = new Point[4];
				diametralPoints[0] = _curve.evaluateAt( 0 );
				diametralPoints[1] = _curve.evaluateAt( Math.sqrt( 2 ) - 1 );
				diametralPoints[2] = _curve.evaluateAt( 2 - Math.sqrt( 2 ) );
				diametralPoints[3] = _curve.evaluateAt( 1 );
				break;
			default:
				// スナッピング候補の点の導出
				Plane[] planes = createDiametralPlanes( _curve );

				diametralPoints = searchIntersectionPoints( _curve, planes );
		}
		return diametralPoints;
	}

	/**
	 * 2点列の組み合わせを設定します。
	 * @param _diametricalPoints 径点
	 * @return 2点列のリスト
	 */
	private static List<SnappingCandidate> configureTwoPointsCombinationList( Point[] _diametricalPoints, BoundingBox _box ) {
		List<SnappingCandidate> snappingPointPairs;
		snappingPointPairs = new ArrayList<SnappingCandidate>();
		for ( int i = 0; i < _diametricalPoints.length; ++i ) {
			for ( int j = i + 1; j < _diametricalPoints.length; ++j ) {
				SnappingCandidate diameterPair = SnappingCandidate.create(
					new Point[]{ _diametricalPoints[i], _diametricalPoints[j] },
					new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER },
					_box );
				snappingPointPairs.add( diameterPair );
			}
		}

		return snappingPointPairs;
	}

	/**
	 * 円弧を包含するバウンディングボックスの軸点列を計算します。
	 * @param _curve スナッピング後の曲線
	 * @param _type 簡約化された曲線の種類
	 * @return 軸点列
	 */
	private static Point[] calcBoxAxisPoints( QuadraticBezierCurve _curve, Point _center, NQuartersType _type ) {
		Point[] axisPoints;
		double w = _curve.weight();
		Point[] cp = _curve.controlPoints();
		Point m = cp[0].internalDivision( cp[2], 1, 1 );
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
		switch ( _type ) {
			case QUARTER:
				//径点の算出
				axisPoints = new Point[2];
				axisPoints[0] = _curve.evaluateAt( 0 );
				axisPoints[1] = _curve.evaluateAt( 1 );

				break;
			case HALF:
				// 径点の算出
				axisPoints = new Point[3];
				axisPoints[0] = _curve.evaluateAt( 0 );
				axisPoints[1] = _curve.evaluateAt( 0.5 );
				axisPoints[2] = _curve.evaluateAt( 1 );

				break;
			case THREE_QUARTERS:
				axisPoints = new Point[4];
				axisPoints[0] = _curve.evaluateAt( 0 );
				axisPoints[1] = _curve.evaluateAt( Math.sqrt( 2 ) - 1 );
				axisPoints[2] = _curve.evaluateAt( 2 - Math.sqrt( 2 ) );
				axisPoints[3] = _curve.evaluateAt( 1 );
				break;
			default:
				Point majorPoint = cp[1];

				Vector minor = Vector.createSE( _center, majorPoint );
				Plane minorPlane = Plane.create( _center, minor );

				//軸点を探索する時のベジェ曲線のレンジ
				//レンジの値は探索でのエラーを回避するための決め打ちの値
				Range range = Range.create( 0, 2.0 );
				QuadraticBezierCurve oval = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], w, range );

				Point[] minorPoints = oval.intersectionWith( minorPlane );

				List<Point> axisPointsList = new ArrayList<Point>();

				//重み係数が0以上（中心角180°以下）の時の軸点
				if ( w >= 0 ) {

					axisPointsList.add( _curve.evaluateAt( minorPoints[0].time() ) );
					axisPointsList.add( majorPoint );
					axisPointsList.add( _curve.evaluateAt( minorPoints[1].time() ) );
				} else {
					//重み係数が0より小さい（中心角180°より大きい）時の軸点
					axisPointsList.add( _curve.evaluateAt( -0.5 ) );
					axisPointsList.add( _curve.evaluateAt( minorPoints[0].time() ) );
					axisPointsList.add( _curve.evaluateAt( 0.5 ) );
					axisPointsList.add( _curve.evaluateAt( minorPoints[1].time() ) );
					axisPointsList.add( _curve.evaluateAt( 1.5 ) );
				}
				axisPoints = axisPointsList.toArray( new Point[axisPointsList.size()] );
				break;
		}
		return axisPoints.clone();
	}

	/**
	 * 円弧を包含するバウンディングボックスの角点列を計算します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _axisPoints 軸点列
	 * @return 角点列
	 */
	private static Point[] calcBoxCornerPoints( QuadraticBezierCurve _curve, Point _center, Point[] _axisPoints ) {
		Point[] cornerPoints;
		Point[] cp = _curve.controlPoints();
		double w = Math.sqrt( 2 ) * 0.5;
		int size = _axisPoints.length;
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
		List<Point> corners = new ArrayList<Point>();

		for ( int i = 1; i < size; ++i ) {
			Point pre = _axisPoints[i - 1];
			Point post = _axisPoints[i];

			Point mid = pre.internalDivision( post, 1, 1 );

			Plane plane = Plane.create( mid, Vector.createSE( mid, _center ).cross( normal ) );

			Range partRange = Range.create( pre.time(), pre.time() + 2.0 );

			QuadraticBezierCurve oval = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], _curve.weight(), partRange );

			Point[] intersections = oval.intersectionWith( plane );

			if ( mid.distance( intersections[0] ) > mid.distance( intersections[1] ) ) {
				corners.add( _center.internalDivision( intersections[1], 2 + 2 * w, -1 ) );
			} else {
				corners.add( _center.internalDivision( intersections[0], 2 + 2 * w, -1 ) );
			}
		}
		cornerPoints = corners.toArray( new Point[corners.size()] );

		return cornerPoints.clone();
	}

	/**
	 * 円・円弧の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 * @param _isClose 曲線が閉じているかどうか
	 */
	private CircularRecognitionResult( RoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		super( _curve, _type, _gradeList );
	}
}
