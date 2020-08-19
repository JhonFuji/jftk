package jp.sagalab.jftk.recognition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.rough.EllipticRoughCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate.SnappingPointType;

/**
 * 楕円・楕円弧の認識結果を表すクラスです。
 * @author nakajima
 */
public class EllipticRecognitionResult extends RecognitionResult{

	/**
	 * 楕円・楕円弧の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	static EllipticRecognitionResult create( EllipticRoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		return new EllipticRecognitionResult( _curve, _type, _gradeList );
	}

	/**
	 * 楕円弧を包含するバウンディングボックスを生成する。
	 * @param _curve 楕円弧を表す二次有理ベジェ曲線
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
		EllipticRoughCurve curve = (EllipticRoughCurve) getCurve();
		QuadraticBezierCurve bezierCurve = (QuadraticBezierCurve) curve.getCurve();
		NQuartersType type = curve.getNQuartersType();
		List<SnappingCandidate> list = new ArrayList<SnappingCandidate>();

		SnappingCandidate partitionCandidate = SnappingCandidate.create(
			new Point[]{ _start, _end },
			new SnappingPointType[]{ SnappingPointType.PARTITION, SnappingPointType.PARTITION },
			createBoundingBox( bezierCurve, type ) );
		list.add( partitionCandidate );

		if ( type != NQuartersType.GENERAL ) {
			// 曲線上のスナッピング候補点の導出
			Point[] diametricalPoints = searchSnappingDiametralPoints( bezierCurve, curve.getNQuartersType() );
			// 曲線外のスナッピング候補点の導出
			Point[] cornerPoints = searchSnappingCornerPoints( bezierCurve, diametricalPoints, curve.getNQuartersType() );
			Point[] sumArrays = new Point[diametricalPoints.length + cornerPoints.length];
			System.arraycopy( diametricalPoints, 0, sumArrays, 0, diametricalPoints.length ); //a→cにコピー
			System.arraycopy( cornerPoints, 0, sumArrays, diametricalPoints.length, cornerPoints.length ); //b→cにコピー
			for ( Point point : sumArrays ) {
				SnappingCandidate partitionPoints = SnappingCandidate.create(
					new Point[]{ _start, _end, point },
					new SnappingPointType[]{ SnappingPointType.PARTITION, SnappingPointType.PARTITION, SnappingPointType.DIAMETER },
					createBoundingBox( bezierCurve, type ) );

				list.add( partitionPoints );
			}
		}
		return list;
	}

	@Override
	public List<SnappingCandidate> getEdgePointsSnappingCandidateList() {
		EllipticRoughCurve curve = (EllipticRoughCurve) getCurve();
		QuadraticBezierCurve bezierCurve = (QuadraticBezierCurve) curve.getCurve();

		List<SnappingCandidate> list = new ArrayList<SnappingCandidate>();
		SnappingCandidate edgePoints = SnappingCandidate.create(
			new Point[]{ bezierCurve.evaluateAtStart(), bezierCurve.evaluateAtEnd() },
			new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER },
			null );
		list.add( edgePoints );
		return list;
	}

	/**
	 * 特徴点列を決定します。
	 * @param _curve ラフ曲線
	 * @return 特徴点列
	 */
	private static List<SnappingCandidate> decideSnappingCandidateLists( RoughCurve _curve ) {
		EllipticRoughCurve roughCurve = (EllipticRoughCurve) _curve;

		QuadraticBezierCurve curve = (QuadraticBezierCurve) roughCurve.getCurve();
		NQuartersType reductionType = roughCurve.getNQuartersType();

		// 曲線上のスナッピング候補点の導出
		Point[] diametricalPoints = searchSnappingDiametralPoints( curve, reductionType );
		// 曲線外のスナッピング候補点の導出
		Point[] cornerPoints = searchSnappingCornerPoints( curve, diametricalPoints, reductionType );

		//バウンディングボックスを生成
		BoundingBox box = createBoundingBox( curve, reductionType );

		// スナッピングする点の組み合わせ
		List<SnappingCandidate> candidateList = new ArrayList<SnappingCandidate>();
		// 3点の組み合わせ
		candidateList.addAll( configureThreePointsCombinationList( diametricalPoints, cornerPoints, box ) );
		// 始終点2点のペアを追加
		if ( candidateList.isEmpty() ) {
			SnappingCandidate edgePair = SnappingCandidate.create(
				new Point[]{ _curve.getStart(), _curve.getEnd() },
				new SnappingPointType[]{ SnappingPointType.EDGE, SnappingPointType.EDGE },
				null );
			candidateList.add( edgePair );
		}

		return candidateList;
	}

	/**
	 * 楕円弧の長径軸と短径軸の交点を求めるために用いる平面を生成します。
	 * @param _major 長径点
	 * @param _minor 短径点
	 * @param _center 中心点
	 * @return 平面
	 */
	private static Plane[] createDiametralPlanes( Point _major, Point _minor, Point _center ) {
		//長径軸と短径軸の交点計算
		Point major = _major;
		Point minor = _minor;
		Point center = _center;
		Plane[] planes = new Plane[]{
			Plane.create( center, Vector.createSE( center, minor ) ),
			Plane.create( center, Vector.createSE( center, major ) )
		};

		return planes;
	}

	/**
	 * スナッピングに用いる経軸の特徴点列を探索します。
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
				Point center = calcCenter( _curve );
				Point major = majorFeaturePoint( _curve, center, _reductionType );
				Point minor = minorFeaturePoint( _curve, center, major, _reductionType );

				// スナッピング候補の点の導出
				Plane[] planes = createDiametralPlanes( major, minor, center );

				diametralPoints = searchIntersectionPoints( _curve, planes );
		}
		return diametralPoints;
	}

	/**
	 * スナッピングに用いる経軸の特徴点を探索します。
	 * @param _curve 楕円形のラフ曲線
	 * @return 角点列
	 */
	private static Point[] searchSnappingDiametralMultiPoints( EllipticRoughCurve _curve ) {
		// 多重ファジィ楕円弧の経軸の特徴点探索はサポートされていません。
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	/**
	 * 特徴点の長径を内包する長方形の隅の点を探索します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _diametricalPoints 楕円弧の経軸上の特徴点列
	 * @return 長径を内包する長方形の隅の点
	 */
	private static Point[] searchSnappingCornerPoints( QuadraticBezierCurve _curve,
		Point[] _diametricalPoints, NQuartersType _reductionType ) {

		Point fuzzyCenter = calcCenter( _curve );
		//クリスプな中心点の生成
		Point center = Point.createXYZT( fuzzyCenter.x(), fuzzyCenter.y(), fuzzyCenter.z(), fuzzyCenter.time() );
		Point[] cp = _curve.controlPoints();

		// 長短径点をパラメータが昇順になるようにソート
		List<Point> diametricalPoints = Arrays.asList( _diametricalPoints );
		Collections.sort( diametricalPoints, new Point.ParameterComparator() );
		double w = Math.sqrt( 2 ) * 0.5;
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );

		int size = diametricalPoints.size();
		List<Point> corners = new ArrayList<Point>();
		//短長径点から生成される長方形の角を算出
		//ただし，短長径点が存在する区間でしか角が存在しないこととする
		//TODO 1/4楕円弧の場合，角が１点も算出不可能になっている
		//TODO 角の点の存在定義について明確にする
		for ( int i = 1; i < size; ++i ) {
			Point pre = diametricalPoints.get( i - 1 );
			Point post = diametricalPoints.get( i );
			Point mid = pre.internalDivision( post, 1, 1 );
			Plane plane = Plane.create( mid, Vector.createSE( mid, center ).cross( normal ) );
			Range range = Range.create( pre.time(), post.time() );
			Point[] intersections = _curve.part( range ).intersectionWith( plane );

			Point farPoint = intersections[0];
			Point result;
			if ( _reductionType == NQuartersType.QUARTER ) {
				result = mid.internalDivision( farPoint, 1 + w, -1 );
			} else {
				result = center.internalDivision( farPoint, 2 + 2 * w, -1 );
			}
			corners.add( result );
		}
		return corners.toArray( new Point[corners.size()] );
	}

	/**
	 * 特徴点の3点列の組み合わせを設定します。
	 * @param _diametricalPoints 径点
	 * @param _cornerPoints ガイドライン上の角の点
	 * @return スナッピングする3点列のリスト
	 */
	private static List<SnappingCandidate> configureThreePointsCombinationList(
		Point[] _diametricalPoints, Point[] _cornerPoints, BoundingBox _box ) {
		List<SnappingCandidate> snappingPointSets = new ArrayList<SnappingCandidate>();

		for ( int i = 0; i < _diametricalPoints.length; ++i ) {
			for ( int j = i + 1; j < _diametricalPoints.length; ++j ) {
				for ( Point corner : _cornerPoints ) {
					SnappingCandidate pointSet = SnappingCandidate.create(
						new Point[]{ _diametricalPoints[i], _diametricalPoints[j], corner },
						new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER, SnappingPointType.CORNER },
						_box );
					snappingPointSets.add( pointSet );
				}
				for ( int k = j + 1; k < _diametricalPoints.length; ++k ) {
					SnappingCandidate pointSet = SnappingCandidate.create(
						new Point[]{ _diametricalPoints[i], _diametricalPoints[j], _diametricalPoints[k] },
						new SnappingPointType[]{ SnappingPointType.DIAMETER, SnappingPointType.DIAMETER, SnappingPointType.DIAMETER },
						_box );
					snappingPointSets.add( pointSet );
				}
			}
		}
		return snappingPointSets;
	}

	/**
	 * 楕円弧の長径点を返します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _center 中心
	 * @param _reductionType 簡約化された曲線の種類
	 * @return 長径の特徴点
	 */
	private static Point majorFeaturePoint( QuadraticBezierCurve _curve, Point _center, NQuartersType _reductionType ) {
		switch ( _reductionType ) {
			case QUARTER: {
				Point cp0 = _curve.controlPoint( 0 );
				Point cp2 = _curve.controlPoint( 2 );
				return _center.distance( cp0 ) > _center.distance( cp2 ) ? cp0 : cp2;
			}
			case HALF:
			case THREE_QUARTERS: {
				Point cp0 = _curve.controlPoint( 0 );
				Point cp1 = _curve.controlPoint( 1 );
				return _center.distance( cp0 ) > _center.distance( cp1 ) ? cp0 : cp1;
			}
			default:
				return calcMajorPoint( _curve, _center );
		}
	}

	/**
	 * 楕円弧の短径点を返します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _center 中心
	 * @param _major 長径点
	 * @param _reductionType 簡約化された曲線の種類
	 * @return 短径の特徴点
	 */
	private static Point minorFeaturePoint( QuadraticBezierCurve _curve, Point _center, Point _major, NQuartersType _reductionType ) {
		switch ( _reductionType ) {
			case QUARTER: {
				Point cp0 = _curve.controlPoint( 0 );
				Point cp2 = _curve.controlPoint( 2 );
				return _center.distance( cp0 ) < _center.distance( cp2 ) ? cp0 : cp2;
			}
			case HALF:
			case THREE_QUARTERS: {
				Point cp0 = _curve.controlPoint( 0 );
				Point cp1 = _curve.controlPoint( 1 );
				return _center.distance( cp0 ) < _center.distance( cp1 ) ? cp0 : cp1;
			}
			default: {
				return calcMinorPoint( _curve, _center, _major );
			}
		}
	}

	/**
	 * 楕円弧の長径点を計算します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _center 中心
	 * @return 長径点
	 */
	private static Point calcMajorPoint( QuadraticBezierCurve _curve, Point _center ) {
		// モデルの全長に対する相対値で許容誤差を設定
		double threshold = _curve.length() * 1e-8;
		// 最遠点
		Point farPoint = _curve.evaluateAtStart();
		double preDistance = _center.distance( farPoint );
		double step = _curve.range().length() / 20;
		// 誤差
		double delta = Double.POSITIVE_INFINITY;

		// TODO 計算が粗すぎる？
		int i = 0;
		// 最初の最大値を目指して収束計算開始
		double distance;
		while ( farPoint.time() < 2.0 && delta > threshold ) {
			farPoint = _curve.evaluateAt( farPoint.time() + step );
			distance = _center.distance( farPoint );
			if ( distance <= preDistance ) {
				step *= -0.5;
				delta = preDistance - distance;
				if ( i > 10 ) {
					break;
				}
				++i;
			}
			preDistance = distance;
		}
		return farPoint;
	}

	/**
	 * 楕円弧の短径点を計算します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _center 中心
	 * @return 長径点
	 */
	private static Point calcMinorPoint( QuadraticBezierCurve _curve, Point _center, Point _major ) {
		Plane plane = Plane.create( _center, Vector.createSE( _center, _major ) );
		Point[] cp = _curve.controlPoints();
		Range range = Range.create( _curve.range().start(), _curve.range().start() + 2 );
		QuadraticBezierCurve oval = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], _curve.weight(), range );
		Point[] intersections = oval.intersectionWith( plane );
		if ( intersections.length == 0 ) {
			throw new RuntimeException();
		}
		return intersections[0];
	}

	/**
	 * 楕円弧を包含するバウンディングボックスの軸点列を計算します。
	 * @param _curve 二次有理ベジェ曲線
	 * @param _reductionType 簡約化された曲線の種類
	 * @return 軸点列
	 */
	private static Point[] calcBoxAxisPoints( QuadraticBezierCurve _curve, Point _center, NQuartersType _reductionType ) {
		Point[] axisPoints;
		switch ( _reductionType ) {
			case QUARTER:
			case HALF:
			case THREE_QUARTERS:
				axisPoints = searchSnappingDiametralPoints( _curve, _reductionType );
				break;
			default:
				// リダクションモデルでない場合の径点の算出
				Point majorPoint = calcMajorPoint( _curve, _center );
				Point minorPoint = calcMinorPoint( _curve, _center, majorPoint );

				Vector minor = Vector.createSE( _center, majorPoint );
				Vector major = Vector.createSE( _center, minorPoint );
				Plane minorPlane = Plane.create( _center, minor );
				Plane majorPlane = Plane.create( _center, major );
				Range range = Range.create( _curve.range().start(), _curve.range().start() + 2 );
				Point[] cp = _curve.controlPoints();
				QuadraticBezierCurve oval = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], _curve.weight(), range );

				Point[] intersectionMi = oval.intersectionWith( minorPlane );
				Point[] intersectionMa = oval.intersectionWith( majorPlane );

				//時間順に並び替え
				List<Point> ovalList = new ArrayList<Point>();
				ovalList.add( intersectionMa[0] );
				ovalList.add( intersectionMi[0] );
				ovalList.add( intersectionMa[1] );
				ovalList.add( intersectionMi[1] );

				Collections.sort( ovalList, new Point.ParameterComparator() );

				Point[] intersectionsMinor = _curve.intersectionWith( minorPlane );
				Point[] intersectionsMajor = _curve.intersectionWith( majorPlane );

				List<Point> intersectionPointList = new ArrayList<Point>();
				intersectionPointList.addAll( Arrays.asList( intersectionsMajor ) );
				intersectionPointList.addAll( Arrays.asList( intersectionsMinor ) );
				Collections.sort( intersectionPointList, new Point.ParameterComparator() );

				List<Point> axisPointsList = new ArrayList<Point>();

				int num = intersectionPointList.size();
				if ( num > 0 ) {//角点が存在
					if ( num > 4 ) {//角点が4つ以上なら4つに。
						num = 4;
					}
					axisPointsList.add( _curve.evaluateAt( ovalList.get( 3 ).time() ) );//２つめの端点を最初にセット
					for ( int i = 0; i < num; ++i ) {
						axisPointsList.add( _curve.evaluateAt( ovalList.get( i ).time() ) );
					}
					if ( num < 4 ) {
						axisPointsList.add( _curve.evaluateAt( ovalList.get( num ).time() ) );
					}
				}
				axisPoints = axisPointsList.toArray( new Point[axisPointsList.size()] );
				break;
		}
		return axisPoints.clone();
	}

	/**
	 * 楕円弧を包含するバウンディングボックスの角点列を計算します。
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
	 * 楕円・楕円弧の認識結果を生成します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 */
	private EllipticRecognitionResult( EllipticRoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		super( _curve, _type, _gradeList );
	}

}
