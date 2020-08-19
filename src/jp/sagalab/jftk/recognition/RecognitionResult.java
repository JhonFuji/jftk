package jp.sagalab.jftk.recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.shaper.snapper.SnappingCandidate;

/**
 * 幾何曲線の認識結果を定義する抽象クラスです。
 * <p>
 * このクラスは幾何曲線認識後でありスナッピング前の状態であるというFSCI上での状態をクラスとして明確に表現しています。
 * (デザインパターンのStateパターンではありません。)<br>
 * またクラスとしてFSCI上の状態を表現する以外にも、特徴点列を返すという役割があります。
 * 特徴点はスナッピング候補点として扱われます。
 * </p>
 * @author nakajima
 */
public abstract class RecognitionResult {

	/**
	 * 幾何曲線の認識結果を返します。
	 * @param _curve ラフ曲線
	 * @param _type 曲線種
	 * @param _gradeList 推論結果のグレード値
	 * @throws IllegalArgumentException ラフ曲線がNullである場合
	 * @throws IllegalArgumentException 曲線種がNullである場合
	 * @throws IllegalArgumentException 推論結果のグレード値の要素が 0 を下回るか 1 を上回った場合
	 */
	protected RecognitionResult( RoughCurve _curve, PrimitiveType _type,
		Map<PrimitiveType, Double> _gradeList ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null." );
		}
		if ( _type == null ) {
			throw new IllegalArgumentException( "_type is null." );
		}
		// _gradeListの中身に適切な値が入っているか調べる
		for ( Map.Entry<PrimitiveType, Double> t : _gradeList.entrySet() ) {
			if ( t.getValue() < 0 || t.getValue() > 1 ) {
				throw new IllegalArgumentException( "_gradeList's grade is not appropriate." );
			}
		}

		m_curve = _curve;
		m_type = _type;
		m_gradeList = _gradeList;
	}

	/**
	 * 認識曲線を返します。
	 * @return ラフ曲線
	 */
	public RoughCurve getCurve() {
		return m_curve;
	}

	/**
	 * この認識結果の認識曲線の曲線種を返します。
	 * @return 曲線種
	 */
	public PrimitiveType getType() {
		return m_type;
	}

	/**
	 * この認識結果の各曲線種のグレードを返します。
	 * @return 各曲線種に対応したjava.util.Map
	 */
	public Map<PrimitiveType, Double> getGradeList() {
		return m_gradeList;
	}

	/**
	 * この認識結果の認識曲線のグレードを返します。
	 * @return グレード
	 */
	public double getGrade() {
		return m_gradeList.get( m_type );
	}

	/**
	 * 認識曲線をプリミティブ曲線化して返します。<br>
	 * 認識曲線はラフな曲線({@link RoughCurve})であるため、曲線の番号を指定します。
	 * @return プリミティブ曲線
	 */
	public PrimitiveCurve getPrimitiveCurve() {
		PrimitiveCurve primitive = getCurve().toPrimitive();

		return primitive;
	}

	/**
	 * 楕円弧幾何曲線としてのグレード値（自由曲線のグレード値の否定）を計算する。
	 *
	 * @param _recognitionResult 楕円弧幾何曲線の認識結果
	 * @return 楕円弧幾何曲線としてのグレード値
	 */
	public double calcNotFreeCurveGrade( ) {
		double foGrade = getGradeList().get( PrimitiveType.OPEN_FREE_CURVE );
		double fcGrade = getGradeList().get( PrimitiveType.CLOSED_FREE_CURVE );
		return 1.0 - Math.max( foGrade, fcGrade );
	}
	
	/**
	 * スナッピング候補リストを返します。
	 * @return スナッピング候補のリスト
	 */
	public abstract List<SnappingCandidate> getSnappingCandidateList();

	public abstract List<SnappingCandidate> getPartitionSnappingCandidateList( Point _start, Point _end );

	public abstract List<SnappingCandidate> getEdgePointsSnappingCandidateList();

	/**
	 * この RecognitionResult と指定された Object が等しいかどうかを比較します。
	 * @param obj この RecognitionResult と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ラフ曲線、曲線種、認識曲線のグレード、閉じているかがまったく同じ RecognitionResult である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final RecognitionResult other = (RecognitionResult) obj;
		if ( !m_curve.equals( other.m_curve ) ) {
			return false;
		}
		if ( !m_type.equals( other.m_type ) ) {
			return false;
		}

		return getGrade() == other.getGrade();
	}

	/**
	 * この RecognitionResult のハッシュコードを返します。
	 * @return この RecognitionResult のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 73 * hash + ( this.m_curve != null ? this.m_curve.hashCode() : 0 );
		hash = 73 * hash + ( this.m_type != null ? this.m_type.hashCode() : 0 );
		hash = 73 * hash + ( this.m_gradeList != null ? this.m_gradeList.hashCode() : 0 );
		return hash;
	}

	/**
	 * この RecognitionResult の文字列表現を返します。
	 * @return ラフ曲線、曲線種、認識曲線のグレード、閉じているかを表す String
	 */
	@Override
	public String toString() {
		return String.format(
			"curve:%s, type:%s, grade:%.3f",
			m_curve.toString(), m_type.toString(), getGrade() );
	}

	/**
	 * 指定された平面列と交差する点の探索
	 * @param _curve 二次有理ベジェ曲線
	 * @param _planes 交点計算で用いる平面
	 * @return 交点配列
	 */
	static Point[] searchIntersectionPoints( QuadraticBezierCurve _curve, Plane[] _planes ) {
		List<Point> intersectionPoints = new ArrayList<Point>();
		for ( Plane plane : _planes ) {
			Point[] intersections = _curve.intersectionWith( plane );
			List<Point> diametricalPoints = new ArrayList<Point>();
			LOOP:
			for ( Point intersection : intersections ) {
				// リストに同一の点が格納されていないか検査
				int size = diametricalPoints.size();
				for ( int i = 0; i < size; ++i ) {
					Point p = diametricalPoints.get( i );
					double x = intersection.x();
					double y = intersection.y();
					double z = intersection.z();
					double f = intersection.fuzziness();
					// 同一の点かどうか
					if ( x == p.x() && y == p.y() && z == p.z() && f == p.fuzziness() ) {
						if ( intersection.time() < p.time() ) {
							// タイムスタンプが小さい方に差し替える
							diametricalPoints.set( i, intersection );
						}
						// 同一の点が既に格納されている場合は次の交点へ
						continue LOOP;
					}
				}
				// 同一の点がリストに無い場合は交点を格納
				diametricalPoints.add( intersection );
			}
			intersectionPoints.addAll( diametricalPoints );
		}
		return intersectionPoints.toArray( new Point[intersectionPoints.size()] );
	}

	/**
	 * 曲線の範囲を[開始値-開始値+2]にします。<br>
	 * 円弧・楕円弧を円・楕円にするという操作になります。
	 * @param _curve 二次有理ベジェ曲線
	 * @return 円形・楕円形の二次有理ベジェ曲線
	 */
	static QuadraticBezierCurve toOval( QuadraticBezierCurve _curve ) {
		Point[] cp = _curve.controlPoints();
		double start = _curve.range().start();

		return QuadraticBezierCurve.create( cp[0], cp[1], cp[2], _curve.weight(), Range.create( start, start + 2.0 ) );
	}

	/**
	 * 二次有理ベジェ曲線の中心点を計算します。
	 * @param _curve 二次有理ベジェ曲線
	 * @return 中心
	 */
	static Point calcCenter( QuadraticBezierCurve _curve ) {
		// 代表点列
		Point[] cp = _curve.controlPoints();
		// cp0 - cp2 の中点
		Point m = cp[0].internalDivision( cp[2], 1, 1 );
		// 重み
		double w = _curve.weight();

		return m.internalDivision( cp[1], -w / ( 1 - w ), 1 / ( 1 - w ) );
	}

	/** ラフ曲線 */
	private final RoughCurve m_curve;
	/** 曲線種 */
	private final PrimitiveType m_type;
	/** 推論結果のグレード値 */
	private final Map<PrimitiveType, Double> m_gradeList;
}
