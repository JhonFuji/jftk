package jp.sagalab.jftk.curve.rough;

import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.primitive.Ellipse;
import jp.sagalab.jftk.curve.primitive.EllipticArc;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.transform.AffineMatrix;
import jp.sagalab.jftk.transform.SimMatrix;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * ラフな楕円形曲線を表すクラスです。
 * @author miwa
 */
public class EllipticRoughCurve extends RoughCurve implements NQuarterable{

	/**
	 * ラフな楕円形曲線を生成します。
	 * @param _curve この曲線を構成する曲線群
	 * @param _isClosed この曲線が閉じているか(閉じている場合はtrueとなります)
	 * @param _reductionType リダクションモデルの種類
	 * @return ラフな楕円弧曲線
	 */
	public static EllipticRoughCurve create( QuadraticBezierCurve _curve, boolean _isClosed, NQuartersType _reductionType ) {
		// 例外処理
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}

		if ( _reductionType == null ) {
			throw new IllegalArgumentException( "_reductionType is null" );
		}

		if ( _reductionType != NQuartersType.GENERAL ) {
			if ( _isClosed == true ) {
				throw new IllegalArgumentException( "_reductionType is " + _reductionType + ", _isClosed is false" );
			}
		}

		return new EllipticRoughCurve( _curve, _isClosed, _reductionType );
	}

	@Override
	public PrimitiveCurve toPrimitive() {
		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		// 代表点列
		Point[] cp = curve.controlPoints();
		// cp0 - cp2 の中点
		Point m = cp[0].internalDivision( cp[2], 1, 1 );
		// 重み
		double w = curve.weight();
		// 中心
		Point center = m.internalDivision( cp[1], -w / ( 1 - w ), 1 / ( 1 - w ) );
		// 始点
		Point start = curve.evaluateAtStart();
		// 終点
		Point end = curve.evaluateAtEnd();

		Point major = majorPoint( center );
		Point minor = minorPoint( center, major );
		// 長軸（xAxis）と短軸（yAxis）の導出
		Vector xAxis = Vector.createSE( center, major );
		Vector yAxis = Vector.createNormal( cp[0], cp[1], cp[2] ).cross( xAxis );

		// 開始・終了角度の導出
		Vector centerToStart = Vector.createSE( center, start );
		Vector centerToEnd = Vector.createSE( center, end );

		// 長軸との偏角を求める
		double startAngle = xAxis.angle( centerToStart );
		double endAngle = xAxis.angle( centerToEnd );

		//長軸側に始点があればそのまま、逆側であれば変更
		if ( centerToStart.dot( yAxis ) < 0 ) {
			startAngle *= -1;
		}
		// 短軸側に終点があればそのまま、逆側であれば変更
		if ( centerToEnd.dot( yAxis ) < 0 ) {
			endAngle *= -1;
		}

		// ｎ周対応
		double roundAngle = 2 * Math.PI;
		endAngle += roundAngle * Math.floor( curve.range().length() * 0.5 );
		// １周未満で終了角が開始角未満の状態に対処
		if ( endAngle <= startAngle ) {
			if ( curve.range().length() > 0 ) {
				endAngle += roundAngle;
			}
		}
		// 姿勢の導出
		//XXX たまにここでIllegalArgumentExceptionが発生する。
		// 原因は楕円弧の代表点が一直線に並んでたり、2点が同じ位置だったりすること。
		// EllipticReferenceModelGeneratorクラスのsearchRepresentationPointsを見直すべきか。
		SimMatrix posture = SimMatrix.createByAxis( xAxis, yAxis );

		//一点に縮退していたら
		if ( Double.isNaN( startAngle ) || Double.isNaN( endAngle ) ) {
			startAngle = 0;
			endAngle = 0;
		}

		double majorR = center.distance( major );
		double minorR = center.distance( minor );
		if ( isClosed() ) {
			return Ellipse.create( center, majorR, minorR, startAngle, posture );
		} else {
			return EllipticArc.create( center, majorR, minorR, Range.create( startAngle, endAngle ), posture );
		}
	}

	@Override
	public QuadraticBezierCurve toSnappedModel( Point[] _snapping, Point[] _snapped, Vector _snappedNormal ) {
		// 例外処理
		if ( _snapping == null ) {
			throw new IllegalArgumentException( "_snapping is null" );
		}

		if ( _snapped == null ) {
			throw new IllegalArgumentException( "_snapped is null" );
		}

		for ( int i = 0; i < _snapping.length; ++i ) {
			if ( _snapping[i] == null ) {
				throw new IllegalArgumentException( "_snapping[" + i + "] is null" );
			}
		}

		for ( int i = 0; i < _snapped.length; ++i ) {
			if ( _snapped[i] == null ) {
				throw new IllegalArgumentException( "_snapped[" + i + "] is null" );
			}
		}

		if ( _snappedNormal == null ) {
			throw new IllegalArgumentException( "_snappedNormal is null" );
		}

		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		Point[] cp = curve.controlPoints();

		TransformMatrix mat = null;
		if ( _snapping.length == 2 && _snapped.length == 2 ) {
			Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
			mat = SimMatrix.createByBeforeAfterPoints( _snapping[0], _snapping[1],
				normal, _snapped[0], _snapped[1], _snappedNormal );
		}
		if ( _snapping.length == 3 && _snapped.length == 3 ) {
			// アフィン変換
			mat = AffineMatrix.createBy3Points( _snapping, _snapped );
		}
		if ( mat == null ) {
			mat = SimMatrix.identity();
		}
		Point[] newCP = new Point[cp.length];
		for ( int i = 0; i < cp.length; ++i ) {
			newCP[i] = cp[i].transform( mat );
		}
		QuadraticBezierCurve snapped = QuadraticBezierCurve.create(
			newCP[0], newCP[1], newCP[2], curve.weight(), curve.range() );

		return snapped;
	}

	@Override
	public PrimitiveCurve toSnappedPrimitive( Point[] _snapping, Point[] _snapped, Vector _snappedNormal ) {
		// 例外処理
		if ( _snapping == null ) {
			throw new IllegalArgumentException( "_snapping is null" );
		}

		if ( _snapped == null ) {
			throw new IllegalArgumentException( "_snapped is null" );
		}

		for ( int i = 0; i < _snapping.length; ++i ) {
			if ( _snapping[i] == null ) {
				throw new IllegalArgumentException( "_snapping[" + i + "] is null" );
			}
		}

		for ( int i = 0; i < _snapped.length; ++i ) {
			if ( _snapped[i] == null ) {
				throw new IllegalArgumentException( "_snapped[" + i + "] is null" );
			}
		}

		if ( _snappedNormal == null ) {
			throw new IllegalArgumentException( "_snappedNormal is null" );
		}

		TransformMatrix mat = null;
		if ( _snapping.length == 2 && _snapped.length == 2 ) {

			Point[] cp = ( (QuadraticBezierCurve) getCurve() ).controlPoints();
			Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
			mat = SimMatrix.createByBeforeAfterPoints( _snapping[0], _snapping[1],
				normal, _snapped[0], _snapped[1], _snappedNormal );
		}
		if ( _snapping.length == 3 && _snapped.length == 3 ) {
			// アフィン変換
			mat = AffineMatrix.createBy3Points( _snapping, _snapped );
		}
		if ( mat == null ) {
			return null;
		}
		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		QuadraticBezierCurve transed = curve.transform( mat );
		return new EllipticRoughCurve( transed, isClosed(), getNQuartersType() ).toPrimitive();
	}

	@Override
	public NQuartersType getNQuartersType() {
		return m_reductionType;
	}

	private Point majorPoint( Point _center ) {
		// モデルの全長に対する相対値で許容誤差を設定
		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		double threshold = curve.length() * 1e-8;
		// 最遠点
		Point farPoint = curve.evaluateAtStart();
		double preDistance = _center.distance( farPoint );
		double step = curve.range().length() / 20;
		// 誤差
		double delta = Double.POSITIVE_INFINITY;

		// TODO 計算が粗すぎる？
		int i = 0;
		// 最初の最大値を目指して収束計算開始
		double distance;
		while ( farPoint.time() < 2.0 && delta > threshold ) {
			farPoint = curve.evaluateAt( farPoint.time() + step );
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

	private Point minorPoint( Point _center, Point _major ) {
		Vector normal = Vector.createSE( _center, _major );
		//長径点と中心点が同一座標の場合、短径点として中心点を返す
		if ( Double.isInfinite( 1 / normal.length() ) ) {
			return _center;
		}
		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		Plane plane = Plane.create( _center, normal );
		Point[] cp = curve.controlPoints();
		Range range = Range.create( curve.range().start(), curve.range().start() + 2 );
		QuadraticBezierCurve oval = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], curve.weight(), range );
		Point[] intersections = oval.intersectionWith( plane );
		return intersections[0];
	}

	private EllipticRoughCurve( QuadraticBezierCurve _curve, boolean _isClosed, NQuartersType _reductionType ) {
		super( _curve, _isClosed );
		m_reductionType = _reductionType;
	}

	/** 簡約型を表す */
	private final NQuartersType m_reductionType;
}
