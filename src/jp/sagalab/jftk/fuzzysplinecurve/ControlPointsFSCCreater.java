package jp.sagalab.jftk.fuzzysplinecurve;

import jp.sagalab.jftk.curve.interporation.SplineCurveInterpolator;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.interporation.PointExtrapolator2th;
import jp.sagalab.jftk.curve.interporation.PointInterpolator0th;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;

/**
 * スプライン曲線の制御点を用いてファジィスプライン曲線を生成します。
 * @author nagase
 */
public class ControlPointsFSCCreater implements FSCCreater{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _timeStretcher 時間伸縮モデル
	 * @param _pointExtrapolateLength 点列内挿の内挿時間間隔
	 * @param _pointExtrapolateSpan 点列外挿の外挿時間長
	 * @param _pointInterpolateSpan 点列外挿の外挿時間間隔
	 * @param _splineInterpolateKnotSpan スプライン曲線補間の節点間隔
	 * @param _fscConvertVelocityCoeff FSC変換の速度係数
	 * @param _fscConvertAccelerationCoeff FSC変換の加速度係数
	 * @return インスタンス
	 */
	public static ControlPointsFSCCreater create( TimeStretchingModel _timeStretcher, double _pointExtrapolateLength,
		double _pointExtrapolateSpan, double _pointInterpolateSpan,
		double _splineInterpolateKnotSpan, double _fscConvertVelocityCoeff,
		double _fscConvertAccelerationCoeff ) {
		if ( _timeStretcher == null ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _pointInterpolateSpan ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _pointExtrapolateLength ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _pointExtrapolateSpan ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _splineInterpolateKnotSpan ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _fscConvertVelocityCoeff ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _fscConvertAccelerationCoeff ) ) {
			throw new IllegalArgumentException();
		}
		return new ControlPointsFSCCreater( _timeStretcher, _pointExtrapolateLength,
			_pointExtrapolateSpan, _pointInterpolateSpan,
			_splineInterpolateKnotSpan, _fscConvertVelocityCoeff, _fscConvertAccelerationCoeff );
	}

	@Override
	public SplineCurve createFSC( Point[] _points ) {
		// 入力点列にノイズをつける
		Point[] noisedPoints = new Point[_points.length];
		for ( int i = 0; i < _points.length; ++i ) {
			noisedPoints[i] = _points[i].move( Math.random() * 0.002 - 0.001, Math.random() * 0.002 - 0.001, 0 );
		}
		// 外挿
		Point[] fixedPoints = PointExtrapolator2th.extrapolate(
			noisedPoints, m_pointExtrapolateLength, m_pointExtrapolateSpan );
		// 内挿
		fixedPoints = PointInterpolator0th.interpolate( fixedPoints, m_pointInterpolateSpan );
		// 外装内装処理後の点列が3次スプライン曲線で評価可能かチェック
		if ( fixedPoints.length >= 4 ) {
			// 3次スプライン近似補間
			SplineCurve spline = SplineCurveInterpolator.interpolate( fixedPoints, 3, m_splineInterpolateKnotSpan );
			// ファジネスを付加してFSC化
			SplineCurve fsc = FuzzySplineCurveCreater.create( spline, m_fscConvertVelocityCoeff, m_fscConvertAccelerationCoeff );
			// 外挿した部分を存在範囲から外す
			Range range = Range.create( noisedPoints[0].time(), noisedPoints[noisedPoints.length - 1].time() );
			fsc = fsc.part( range );
			// 時間伸縮
//			fsc = m_timeStretcher.stretch( fsc );
			return fsc;
		} else {
			return null;
		}
	}

	private ControlPointsFSCCreater( TimeStretchingModel _timeStretcher, double _pointExtrapolateLength,
		double _pointExtrapolateSpan, double _pointInterpolateSpan,
		double _splineInterpolateKnotSpan, double _fscConvertVelocityCoeff,
		double _fscConvertAccelerationCoeff ) {
		m_pointExtrapolateLength = _pointExtrapolateLength;
		m_pointExtrapolateSpan = _pointExtrapolateSpan;
		m_pointInterpolateSpan = _pointInterpolateSpan;
		m_splineInterpolateKnotSpan = _splineInterpolateKnotSpan;
		m_fscConvertVelocityCoeff = _fscConvertVelocityCoeff;
		m_fscConvertAccelerationCoeff = _fscConvertAccelerationCoeff;
		m_timeStretcher = _timeStretcher;
	}

	/** 点列内挿の内挿時間間隔 */
	private final double m_pointInterpolateSpan;
	/** 点列外挿の外挿時間長 */
	private final double m_pointExtrapolateLength;
	/** 点列外挿の外挿時間間隔 */
	private final double m_pointExtrapolateSpan;
	/** スプライン曲線補間の節点間隔 */
	private final double m_splineInterpolateKnotSpan;
	/** FSC変換の速度係数 */
	private final double m_fscConvertVelocityCoeff;
	/** FSC変換の加速度係数 */
	private final double m_fscConvertAccelerationCoeff;
	/** FSC時間伸縮機 */
	private final TimeStretchingModel m_timeStretcher;
}
