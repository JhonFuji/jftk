package jp.sagalab.jftk.reference.elliptic;

import java.util.Arrays;
import jp.sagalab.jftk.BrentMethod;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import static jp.sagalab.jftk.reference.elliptic.EllipticReferenceModelGenerator.calculateRange;

/**
 * 楕円弧におけるBrent法の計算を行うクラスです。
 * @author ishiguro
 */
class BrentMethodForElliptic extends BrentMethod{

	/**
	 * 楕円弧におけるBreant法の計算を行うためのインスタンスを生成します。
	 * @param _rp 代表点列
	 * @param _curve パラメトリック曲線
	 * @param _tol Brent法による探索の相対誤差
	 * @return インスタンス
	 * @throws IllegalArgumentException 代表点列の要素にNullが存在する場合
	 * @throws IllegalArgumentException パラメトリック曲線がNullである場合
	 */
	static BrentMethodForElliptic create( Point[] _rp, ParametricCurve _curve, double _tol ) {
		if ( Arrays.asList( _rp ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_rp include null." );
		}
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null." );
		}
		return new BrentMethodForElliptic( _rp, _curve, _tol );
	}

	@Override
	public double function( double _x ) {
		// 2次有理Bezier曲線から楕円弧を生成
		QuadraticBezierCurve bezier = QuadraticBezierCurve.create( m_rp[0], m_rp[1], m_rp[2], _x, calculateRange( m_curve, m_rp, _x ) );
		// 2次有理Bezier曲線を楕円形リファレンスモデルに変換
		EllipticReferenceModel model = EllipticReferenceModel.create( bezier );
		// 負の値を含む可能性値を用いてFMPSを導出
		Point[] points = model.evaluateAll( NUM_OF_EVALUATION, ParametricCurve.EvaluationType.DISTANCE );
		Point[] otherPoints = m_curve.evaluateAll( NUM_OF_EVALUATION, ParametricCurve.EvaluationType.DISTANCE );
		double pos = 1;
		for ( int i = 0; i < NUM_OF_EVALUATION; ++i ) {
			double distance = points[i].distance( otherPoints[i] );
			double fuzzinessSum = points[i].fuzziness() + otherPoints[i].fuzziness();
			double result;
			if ( Double.isInfinite( fuzzinessSum ) ) {
				result = 1;
			} else {
				// 負の可能性値を取るよう拡張
				result = ( fuzzinessSum - distance ) / fuzzinessSum;
			}
			if ( Double.isNaN( pos ) ) {
				result = 1;
			}
			pos = Math.min( pos, result );
		}
		// 導出されたFMPSを評価関数の値として返す
		return 1 - pos;
	}

	/**
	 * コンストラクタ。
	 * @param _rp 代表点列
	 * @param _curve パラメトリック曲線
	 * @param _tol Brent法による探索の相対誤差
	 */
	private BrentMethodForElliptic( Point[] _rp, ParametricCurve _curve, double _tol ) {
		super( _tol );
		m_rp = _rp;
		m_curve = _curve;
	}

	/** 代表点列 */
	private final Point[] m_rp;
	/** パラメトリック曲線 */
	private final ParametricCurve m_curve;
	/** 評価点数 */
	private static final int NUM_OF_EVALUATION = 10;
}
