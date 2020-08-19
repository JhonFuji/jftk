package jp.sagalab.jftk.curve.rough;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.primitive.ClosedFreeCurve;
import jp.sagalab.jftk.curve.primitive.OpenFreeCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;

/**
 * ラフな自由曲線を表すクラスです。
 * @author miwa
 */
public class FreeRoughCurve extends RoughCurve{

	/**
	 * ラフな自由曲線を生成します。
	 * @param _fsc この曲線を構成するスプライン曲線
	 * @param _isClosed この曲線が閉じているか(閉じている場合はtrueとなります)
	 * @return ラフな自由曲線
	 */
	public static FreeRoughCurve create( SplineCurve _fsc, boolean _isClosed ){
		// 例外処理
		if(_fsc == null){
			throw new IllegalArgumentException( "_fsc is null" );
		}				
		
		return new FreeRoughCurve( _fsc, _isClosed );
	}
	
	@Override
	public PrimitiveCurve toPrimitive() {
		BezierCurve[] beziers = ((SplineCurve) getCurve()).convert();
		if ( isClosed() ) {
			// close処理
			Point[] firstCP = beziers[0].controlPoints();
			Point[] lastCP = beziers[beziers.length - 1].controlPoints();
			lastCP[lastCP.length - 1] = firstCP[0];
			beziers[beziers.length - 1] = BezierCurve.create( lastCP, Range.zeroToOne() );
			return ClosedFreeCurve.create(beziers );
		} else {
			return OpenFreeCurve.create(beziers );
		}
	}

	@Override
	public QuadraticBezierCurve toSnappedModel( Point[] _snapping, Point[] _snapped, Vector snappedNormal ) {
	  // 自由曲線のスナッピングはサポートされていません。
		throw new UnsupportedOperationException( "Not supported yet." );
	}
	
	@Override
	public PrimitiveCurve toSnappedPrimitive( Point[] _snapping, Point[] _snapped, Vector _snappedNormal ) {
		// 自由曲線のスナッピングはサポートされていません。
		throw new UnsupportedOperationException( "Not supported yet." );
	}
		
	private FreeRoughCurve(SplineCurve _fsc, boolean _isClosed ) {
		super( _fsc, _isClosed );
	}

}