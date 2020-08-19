package jp.sagalab.jftk.fuzzysplinecurve;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.ParametricEvaluable;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.interporation.PointExtrapolator2th;
import jp.sagalab.jftk.curve.interporation.PointInterpolator0th;
import jp.sagalab.jftk.curve.interporation.SplineCurveInterpolator;

/**
 * 時間伸縮を行うためのクラスです。
 * 与えられた曲線の時間を伸縮します．
 * <p>
 * 時間伸縮モデルは，インスタンス生成時に設定された「基準ファジネス」よりも
 * ファジネスが大きい箇所は，よりファジネスが大きくなるよう時間を短縮し，
 * 逆にファジネスが小さい箇所は時間を伸長します．
 * </p><p>
 * 伸縮の程度は「補正強度」によって調整することが出来ます．
 * 0なら補正なし．大きくなるほど極端に伸縮します．
 * また，マイナス値を与えれば，上述とは逆の特性で補正します．
 * </p>
 * @see "修士論文 「手書き図形認識法FSCIにおける心理的描画時間と実描画時間の対応に関する研究」 永瀬 孝之"
 * @author T.Nagase
 */
public final class TimeStretchingModel {

	/**
	 * インスタンスを生成します．
	 *
	 * 「補正強度」，「基準ファジネス」，「デバイス単位の長さ」以外の
	 * 引数はFSCの生成に関わるものです．
	 * FSCCreaterに指定したものと同様の値を指定してください．
	 * @param _timeStretchCoeff 補正強度
	 * @param _timeStretchBaseFuzziness 基準ファジネス
	 * @param _lengthPerDeviceUnit 1デバイス単位あたりの長さ[cm]
	 * @param _pointExtrapolateLength 点列内挿の内挿時間間隔
	 * @param _pointExtrapolateSpan 点列外挿の外挿時間長
	 * @param _pointInterpolateSpan 点列外挿の外挿時間間隔
	 * @param _splineInterpolateKnotSpan スプライン曲線補間の節点間隔
	 * @param _fscConvertVelocityCoeff FSC変換の速度係数
	 * @param _fscConvertAccelerationCoeff FSC変換の加速度係数
	 * @return インスタンス
	 */
	public static TimeStretchingModel create( double _timeStretchCoeff, double _timeStretchBaseFuzziness,
		double _lengthPerDeviceUnit,
		double _pointExtrapolateLength, double _pointExtrapolateSpan,
		double _pointInterpolateSpan, double _splineInterpolateKnotSpan,
		double _fscConvertVelocityCoeff, double _fscConvertAccelerationCoeff ){
				if ( Double.isNaN( _timeStretchCoeff ) ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _timeStretchBaseFuzziness )
			|| _timeStretchBaseFuzziness <= 0.0 ) {
			throw new IllegalArgumentException();
		}
		if ( Double.isNaN( _lengthPerDeviceUnit )
			|| _lengthPerDeviceUnit < 0.0 ) {
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

		return new TimeStretchingModel(_timeStretchCoeff, _timeStretchBaseFuzziness, _lengthPerDeviceUnit,
																	 _pointExtrapolateLength, _pointExtrapolateSpan,
																	 _pointInterpolateSpan, _splineInterpolateKnotSpan,
																	 _fscConvertVelocityCoeff,  _fscConvertAccelerationCoeff );
	}
	
	/**
	 * FSCの時間伸縮を行います．
	 * @param _fsc ファジィスプライン曲線
	 * @return 時間伸縮されたファジィスプライン曲線
	 */
	public SplineCurve stretch( SplineCurve _fsc ) {
		// 評価点数　評価点間の時間が最低時間解像度より小さくなるように求める　区間数+1
		int num = (int) Math.ceil( _fsc.range().length() / ( m_resolution ) ) + 1;
		Point[] points = _fsc.evaluateAll( num, ParametricEvaluable.EvaluationType.TIME );
		points = polate( points );
		Range orgRange = _fsc.range();

		// 普通のFSC
		SplineCurve fuzSpline = pointsToSpline( points, m_fscConvertVelocityCoeff, m_fscConvertAccelerationCoeff );
		// 速度成分からのみファジネスを生成したFSC
		SplineCurve velSpline = pointsToSpline( points, 1, 0 );
		// 加速度成分からのみファジネスを生成したFSC
		SplineCurve accSpline = pointsToSpline( points, 0, 1 );

		// 積分
		SplineCurve iFuzSpline = fuzSpline.integration( Point.createXYZ( 0, 0, 0 ) );
		SplineCurve iVelSpline = velSpline.integration( Point.createXYZ( 0, 0, 0 ) );
		SplineCurve iAccSpline = accSpline.integration( Point.createXYZ( 0, 0, 0 ) );

		Point[] samples = fuzSpline.evaluateAll( num, ParametricEvaluable.EvaluationType.TIME );
		double[] times = new double[ samples.length ];
		for ( int i = 0; i < samples.length; ++i ) {
			times[i] = samples[i].time();
		}
		double delta = 0;
		double start = 0;
		double end = 0;
		for ( int i = 1; i < times.length; ++i ) {
			int pre = i - 1;
			int post = i;
			// 点列間の平均ファジネスを求める
			double f0 = iFuzSpline.evaluate( times[pre] ).fuzziness();
			double f1 = iFuzSpline.evaluate( times[post] ).fuzziness();
			double fuz = ( f1 - f0 ) / ( times[post] - times[pre] );

			// 点列間の平均速度を求める
			f0 = iVelSpline.evaluate( times[pre] ).fuzziness();
			f1 = iVelSpline.evaluate( times[post] ).fuzziness();
			double vel = ( f1 - f0 ) / ( times[post] - times[pre] );

			// 点列間の平均加速度を求める
			f0 = iAccSpline.evaluate( times[pre] ).fuzziness();
			f1 = iAccSpline.evaluate( times[post] ).fuzziness();
			double acc = ( f1 - f0 ) / ( times[post] - times[pre] );


			// 二次方程式 a*ratio^2 + b*ratio = c を解く
			double a = m_fscConvertAccelerationCoeff * acc;
			double b = m_fscConvertVelocityCoeff * vel;
			double c = fuz * Math.pow( fuz / m_baseFuzziness, m_alpha );
			// 上式の代わりに使用すると，ほぼ一定のファジネスを持ったFSCが生成されます．
			//double c = (0.5/m_lpd);

			// 解の公式使って時間伸縮率を求める
			double ratio = ( 2 * a ) / ( -b + Math.sqrt( b * b - 4 * a * -c ) );

			// 伸縮後のレンジ開始値を求める
			if ( times[i - 1] < orgRange.start() && times[i] >= orgRange.start() ) {
				start = samples[0].time() + delta + ( orgRange.start() - times[i - 1] ) * ratio;
			}
			// 伸縮後のレンジ終了値を求める
			if ( times[i] >= orgRange.end() && times[i - 1] < orgRange.end() ) {
				end = samples[0].time() + delta + ( orgRange.end() - times[i - 1] ) * ratio;
			}

			// 点列間の時間を伸縮する．
			double dt = ( times[i] - times[i - 1] ) * ratio;
			delta += dt;
			samples[i] = Point.createXYZT( samples[i].x(), samples[i].y(), samples[i].z(), samples[0].time() + delta );
		}
		return pointsToSpline( samples, m_fscConvertVelocityCoeff, m_fscConvertAccelerationCoeff ).part( Range.create( start, end ) );
	}

	private SplineCurve pointsToSpline( final Point[] _points, double _fscConvertVelocityCoeff, double _fscConvertAccelerationCoeff ) {
		Point[] points = _points.clone();
		// 3次スプライン近似補間
		SplineCurve spline = SplineCurveInterpolator.interpolate( points, 3, m_splineInterpolateKnotSpan );
		// ファジネスを付加してFSC化
		SplineCurve fsc = FuzzySplineCurveCreater.create( spline, _fscConvertVelocityCoeff, _fscConvertAccelerationCoeff );
		return fsc;
	}

	/**
	 * 内挿・外挿
	 */
	private Point[] polate( Point[] _points ) {
		// 外挿
		Point[] fixedPoints = PointExtrapolator2th.extrapolate( _points, m_pointExtrapolateLength, m_pointExtrapolateSpan);
		// 内挿
		fixedPoints = PointInterpolator0th.interpolate( fixedPoints, m_pointInterpolateSpan );
		return fixedPoints;
	}

	private TimeStretchingModel( double _timeStretchCoeff, double _timeStretchBaseFuzziness,
		double _lengthPerDeviceUnit,
		double _pointExtrapolateLength, double _pointExtrapolateSpan,
		double _pointInterpolateSpan, double _splineInterpolateKnotSpan,
		double _fscConvertVelocityCoeff, double _fscConvertAccelerationCoeff ) {
		
		m_lpd = _lengthPerDeviceUnit;
		m_alpha = _timeStretchCoeff;
		m_baseFuzziness = _timeStretchBaseFuzziness / _lengthPerDeviceUnit;
		// 最低時間解像度は小さいほど精度がいいけど，とりあえず内挿時間の自乗ぐらいあれば十分
		m_resolution = _pointInterpolateSpan*_pointInterpolateSpan;

		m_pointExtrapolateLength = _pointExtrapolateLength;
		m_pointExtrapolateSpan = _pointExtrapolateSpan;
		m_pointInterpolateSpan = _pointInterpolateSpan;
		m_splineInterpolateKnotSpan = _splineInterpolateKnotSpan;
		m_fscConvertVelocityCoeff = _fscConvertVelocityCoeff;
		m_fscConvertAccelerationCoeff = _fscConvertAccelerationCoeff;
	}

	
	/** 1デバイス単位あたりの長さ[cm] */
	private final double m_lpd;
	/** 時間伸縮の基準となるファジネスの大きさ[デバイス単位] */
	private final double m_baseFuzziness;
	/** 時間伸縮モデルの補正係数 */
	private final double m_alpha;
	/** 最低時間解像度 */
	private final double m_resolution;

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
}
