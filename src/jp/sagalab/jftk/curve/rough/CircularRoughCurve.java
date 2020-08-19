package jp.sagalab.jftk.curve.rough;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.SimMatrix;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.primitive.Circle;
import jp.sagalab.jftk.curve.primitive.CircularArc;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * ラフな円形曲線を表すクラスです。
 * @author miwa
 */
public class CircularRoughCurve extends RoughCurve implements NQuarterable{

	/**
	 * ラフな円形曲線を生成します。
	 * @param _curve この曲線を構成する曲線
	 * @param _isClosed この曲線が閉じているか(閉じている場合はtrueとなります)
	 * @param _reductionType リダクションモデルの種類
	 * @return ラフな円形曲線
	 */
	public static CircularRoughCurve create( QuadraticBezierCurve _curve, boolean _isClosed, NQuartersType _reductionType ) {
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

		return new CircularRoughCurve( _curve, _isClosed, _reductionType );
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
		Point center;
		double angle;
		SimMatrix posture;
		double radius = 0;

		switch ( m_reductionType ) {
			case QUARTER:
				center = m.internalDivision( cp[1], -w / ( 1 - w ), 1 / ( 1 - w ) );
				radius = center.distance( cp[0] );
				angle = Math.PI * 0.5;
				posture = SimMatrix.createByAxis( Vector.createSE( center, cp[0] ), Vector.createSE( center, cp[2] ) );
				if ( isClosed() ) {
					return Circle.create( center, radius, posture );
				} else {
					return CircularArc.create( center, radius, angle, posture );
				}
			case HALF:
				center = m;
				radius = center.distance( cp[0] );
				angle = Math.PI;
				posture = SimMatrix.createByAxis( Vector.createSE( center, cp[0] ), Vector.createSE( center, cp[1] ) );
				if ( isClosed() ) {
					return Circle.create( center, radius, posture );
				} else {
					return CircularArc.create( center, radius, angle, posture );
				}
			case THREE_QUARTERS:
				center = m.internalDivision( cp[1], -w / ( 1 - w ), 1 / ( 1 - w ) );
				radius = center.distance( cp[0] );
				angle = Math.PI * 1.5;
				posture = SimMatrix.createByAxis( Vector.createSE( center, cp[0] ), Vector.createSE( center, cp[2] ).reverse() );
				if ( isClosed() ) {
					return Circle.create( center, radius, posture );
				} else {
					return CircularArc.create( center, radius, angle, posture );
				}
			case FOUR_QUARTERS:
				center = m;
				angle = Math.PI * 2.0;
				posture = SimMatrix.createByAxis( Vector.createSE( center, cp[0] ), Vector.createSE( center, cp[1] ) );
				if ( isClosed() ) {
					return Circle.create( center, radius, posture );
				} else {
					return CircularArc.create( center, radius, angle, posture );
				}
			default:
				// 始点
				Point start = curve.evaluateAtStart();
				// 終点
				Point end = curve.evaluateAtEnd();
				center = m.internalDivision( cp[1], -w / ( 1 - w ), 1 / ( 1 - w ) );
				radius = center.distance( start );

				Vector xAxis;
				if ( isClosed() ) {
					Point midPoint = start.internalDivision( end, 1.0, 1.0 );
					Vector centerToMid = Vector.createSE( center, midPoint );
					xAxis = centerToMid.normalize().magnify( radius );
				} else {
					xAxis = Vector.createSE( center, start );
				}

				Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
				Vector yAxis = normal.cross( xAxis );
				int n = (int) Math.floor( curve.range().length() * 0.5 );
				double roundAngle = 2 * Math.PI;
				angle = roundAngle * n;
				Vector centerToEnd = Vector.createSE( center, end );
				if ( centerToEnd.dot( yAxis ) > 0 ) {
					angle += xAxis.angle( centerToEnd );
				} else {
					angle += roundAngle - xAxis.angle( centerToEnd );
				}
				posture = SimMatrix.createByAxis( xAxis, yAxis );
				if ( isClosed() ) {
					return Circle.create( center, radius, posture );
				} else {
					return CircularArc.create( center, radius, angle, posture );
				}
		}
	}

	@Override
	public QuadraticBezierCurve toSnappedModel( Point[] _snapping, Point[] _snapped, Vector _snappedNormal ) {
		// 例外処理
		if ( _snapping == null ) {
			throw new IllegalArgumentException( "_snapping is nul" );
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
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );

		TransformMatrix mat = SimMatrix.createByBeforeAfterPoints( _snapping[0], _snapping[1],
			normal, _snapped[0], _snapped[1], _snappedNormal );
		// スナッピング前の点とスナッピング後の点から変換行列が生成できない場合
		if ( mat == null ) {
			mat = SimMatrix.identity();
		}
		Point[] newCP = new Point[cp.length];
		for ( int i = 0; i < cp.length; ++i ) {
			newCP[i] = cp[i].transform( mat );
		}
		QuadraticBezierCurve snappedCurve = QuadraticBezierCurve.create(
			newCP[0], newCP[1], newCP[2], curve.weight(), curve.range() );

		return snappedCurve;
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

		Point[] cp = ( (QuadraticBezierCurve) getCurve() ).controlPoints();
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
		SimMatrix mat = SimMatrix.createByBeforeAfterPoints( _snapping[0], _snapping[1],
			normal, _snapped[0], _snapped[1], _snappedNormal );
		// スナッピング前の点とスナッピング後の点から変換行列が生成できない場合
		if ( mat == null ) {
			return null;
		}
		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		QuadraticBezierCurve transed = curve.transform( mat );
		return new CircularRoughCurve( transed, isClosed(), getNQuartersType() ).toPrimitive();
	}

	@Override
	public NQuartersType getNQuartersType() {
		return m_reductionType;
	}

	private CircularRoughCurve( QuadraticBezierCurve _curve, boolean _isClosed, NQuartersType _reductionType ) {
		super( _curve, _isClosed );
		m_reductionType = _reductionType;
	}

	/** 簡約型を表す */
	private final NQuartersType m_reductionType;

}
