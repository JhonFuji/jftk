package jp.sagalab.jftk.curve.rough;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.primitive.Line;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.transform.SimMatrix;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * ラフな線形曲線を表すクラスです。
 * @author miwa
 */
public class LinearRoughCurve extends RoughCurve{

	/**
	 * ラフな線形曲線を生成します。
	 * @param _curve この曲線を構成する曲線
	 * @return LinearRoughCurve
	 */
	public static LinearRoughCurve create( QuadraticBezierCurve _curve ) {
		// 例外処理
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}

		return new LinearRoughCurve( _curve );
	}

	@Override
	public PrimitiveCurve toPrimitive() {
		return Line.create( getCurve().evaluateAtStart(), getCurve().evaluateAtEnd() );
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

		if ( _snapping.length != 2 || _snapped.length != 2 ) {
			throw new IllegalArgumentException( "Size of snapping points or snapped points is wrong." );
		}

		QuadraticBezierCurve curve = (QuadraticBezierCurve) getCurve();
		Point[] cp = curve.controlPoints();
		Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
		TransformMatrix mat = SimMatrix.createByBeforeAfterPoints( _snapping[0], _snapping[1],
			normal, _snapped[0], _snapped[1], _snappedNormal );
		// スナッピング前の点とスナッピング後の点に変換する行列が生成できない場合
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

		if ( _snapping.length != 2 || _snapped.length != 2 ) {
			throw new IllegalArgumentException( "Size of snapping points or snapped points is wrong." );
		}
		Point mid = _snapped[0].internalDivision( _snapped[1], 1, 1 );
		QuadraticBezierCurve snapped = QuadraticBezierCurve.create(
			_snapped[0], mid, _snapped[1], 0, getCurve().range() );

		return new LinearRoughCurve( snapped ).toPrimitive();
	}

	private LinearRoughCurve( QuadraticBezierCurve _curve ) {
		super( _curve, false );
	}

}
