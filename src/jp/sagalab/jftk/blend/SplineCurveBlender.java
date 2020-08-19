package jp.sagalab.jftk.blend;

import java.util.Arrays;
import java.util.EnumSet;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.curve.OutOfRangeException;
import jp.sagalab.jftk.curve.ParametricEvaluable;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.fuzzysplinecurve.FuzzySplineCurveCreater;

/**
 * ファジィスプライン曲線を融合するためのクラスです。
 * @author miwa
 */
public class SplineCurveBlender implements FSCBlender{

	/**
	 * 融合パターンの種類を定義する識別子です。
	 */
	private static enum BlendPattern {
		/** この曲線から融合区間を通りこの曲線 */
		THIS_TO_THIS,
		/** この曲線から融合区間を通り他の曲線 */
		THIS_TO_OTHER,
		/** 他の曲線から融合区間を通りこの曲線 */
		OTHER_TO_THIS,
		/** 他の曲線から融合区間を通り他の曲線 */
		OTHER_TO_OTHER,
		/** 融合区間からこの曲線 */
		BLENDED_TO_THIS,
		/** 融合区間から他の曲線 */
		BLENDED_TO_OTHER,
		/** この曲線から融合区間 */
		THIS_TO_BLENDED,
		/** 他の曲線から融合区間 */
		OTHER_TO_BLENDED,
		/** 融合区間のみ */
		BLENDED_TO_BLENDED,
		/** この曲線か他の曲線から融合区間を通りこの曲線 */
		BOTH_TO_THIS,
		/** この曲線か他の曲線から融合区間を通り他の曲線 */
		BOTH_TO_OTHER,
		/** この曲線から融合区間を通りこの曲線か他の曲線 */
		THIS_TO_BOTH,
		/** 他の曲線から融合区間を通りこの曲線か他の曲線 */
		OTHER_TO_BOTH,
		/** この曲線か他の曲線から融合区間を通りこの曲線か他の曲線 */
		BOTH_TO_BOTH,
		/** 融合区間からこの曲線か他の曲線 */
		BLENDED_TO_BOTH,
		/** この曲線か他の曲線から融合区間 */
		BOTH_TO_BLENDED
	}

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _figuralOverlappingTrue 形状的重複の言語的真理値「真」
	 * @param _overlappingStateTrue 重複状態の言語的真理値「真」
	 * @param _fscConvertVelocityCoeff FSC変換の速度係数
	 * @param _fscConvertAccelerationCoeff FSC変換の加速度係数
	 * @return インスタンス
	 * @throws IllegalArgumentException 形状的重複の言語的真理値がnullの場合
	 * @throws IllegalArgumentException 重複状態の言語的真理値がnullの場合
	 * @throws IllegalArgumentException 速度係数がNaNもしくは、Infの場合
	 * @throws IllegalArgumentException 加速度係数がNaNもしくは、Infの場合
	 */
	public static SplineCurveBlender create(Sigmoid _figuralOverlappingTrue, Sigmoid _overlappingStateTrue, double _fscConvertVelocityCoeff, double _fscConvertAccelerationCoeff){
		if ( _figuralOverlappingTrue == null ) {
			throw new IllegalArgumentException( "_figuralOverlappingTrue is null" );
		}
		if ( _overlappingStateTrue == null ) {
			throw new IllegalArgumentException( "_overlappingStateTrue is null" );
		}
		if ( Double.isNaN( _fscConvertAccelerationCoeff ) || Double.isInfinite( _fscConvertAccelerationCoeff ) ) {
			throw new IllegalArgumentException( "_fscConvertAccelerationCoeff is NaN or Inf" );
		}
		if ( Double.isNaN( _fscConvertVelocityCoeff ) || Double.isInfinite( _fscConvertVelocityCoeff ) ) {
			throw new IllegalArgumentException( "_fscConvertVelocityCoeff is NaN or Inf" );
		}
		return new SplineCurveBlender(_figuralOverlappingTrue, _overlappingStateTrue, _fscConvertVelocityCoeff, _fscConvertAccelerationCoeff);
	}
	
	/**
	 * この曲線と他方の曲線を制御点の融合によって融合します。
	 * @param _existedFsc 既存の曲線
	 * @param _fsc 他の曲線
	 * @param _weightA 既存の曲線の重み
	 * @param _weightB 他の曲線の重み
	 * @return 両曲線を融合した結果の曲線
	 * @throws IllegalArgumentException 既存のファジィスプライン曲線がnullの場合
	 * @throws IllegalArgumentException 他のファジィスプライン曲線がnullの場合
	 * @throws IllegalArgumentException 既存のファジィスプライン曲線と他のファジィスプライン曲線の次数が異なる場合
	 * @throws IllegalArgumentException 重みの和が０になる場合
	 * @throws IllegalArgumentException 重みがinfinityもしくはNaNの場合
	 */
	@Override
	public SplineCurve createBlendedFsc( SplineCurve _existedFsc, SplineCurve _fsc, double _weightA, double _weightB ) {
		if ( _existedFsc == null ) {
			throw new IllegalArgumentException( "_existedFsc is null." );
		}
		if ( _fsc == null ) {
			throw new IllegalArgumentException( "_fsc is null." );
		}
		if ( _fsc.degree() != _existedFsc.degree() ) {
			throw new IllegalArgumentException( "The degree is different." );
		}
		if ( Double.isNaN( _weightA ) || Double.isNaN( _weightB ) ) {
			throw new IllegalArgumentException( "internal division ratios is NaN." );
		}
		if ( Double.isInfinite( _weightA ) || Double.isInfinite( _weightB ) ) {
			throw new IllegalArgumentException( "internal division ratios is Inf." );
		}
		if ( _weightA + _weightB == 0 ) {
			throw new IllegalArgumentException( "_weightA + _weightB = 0" );
		}
		OverlappingRange[] overlappingRanges = OverlappingRangeFinder.find( _existedFsc, _fsc, 0.5 );
		Arrays.sort( overlappingRanges,
			new OverlappingRange.FiguralDOLComparator( m_figuralOverlappingTrue, m_overlappingStateTrue ) );
		if ( overlappingRanges.length <= 0
			|| overlappingRanges[overlappingRanges.length - 1].figuralPossibility() <= 0 ) {
			return null;
		}
		Range[] rangePair = overlappingRanges[overlappingRanges.length - 1].rangePair();
		double[][] knotsPair = overlappingRanges[overlappingRanges.length - 1].knotsPair();
		// 制御点に対して平均操作を行う融合
		SplineCurve fsc = blend( _existedFsc, rangePair[0], knotsPair[0], _fsc, rangePair[1], knotsPair[1], 1, 1 );
		// 融合不可能と判断された場合null返却
		if ( fsc == null ) {
			return null;
		}
		// ファジネス再生成
		fsc = FuzzySplineCurveCreater.create(
			fsc, m_fscConvertVelocityCoeff,
			m_fscConvertAccelerationCoeff );

		return fsc;
	}
	
	/**
	 * 指定された二つのファジィスプライン曲線を指定された区間で融合します。
	 * @param _thisFSC ファジィスプライン曲線A
	 * @param _thisRange 曲線Aの重複区間
	 * @param _thisKnots 曲線Aの節点
	 * @param _otherFSC 曲線Aと融合する曲線
	 * @param _otherRange 曲線Aと融合する曲線の重複区間
	 * @param _otherKnots 曲線Aと融合する曲線の節点
	 * @param _weightA 内分比A
	 * @param _weightB 内分比B
	 * @return 融合した曲線
	 */
	private SplineCurve blend( SplineCurve _thisFSC, Range _thisRange, double[] _thisKnots,
		SplineCurve _otherFSC, Range _otherRange, double[] _otherKnots,
		double _weightA, double _weightB ) {

		BlendPattern pattern = blendPattern(_thisFSC, _thisRange, _otherFSC, _otherRange );
		if ( !BLEND＿ENABLE＿SET.contains( pattern ) ) {
			return null;
		}

		final double[] thisKnots = _thisFSC.knots();
		final int thisDegree = _thisFSC.degree();
		final double[] otherKnots = _otherFSC.knots();
		final int otherDegree = _otherFSC.degree();
		double knotsInterval = Math.min(
			( thisKnots[thisKnots.length - thisDegree] - thisKnots[thisDegree - 1] ) / ( thisKnots.length - 2 * thisDegree + 1 ),
			( otherKnots[otherKnots.length - thisDegree] - otherKnots[thisDegree - 1] ) / ( otherKnots.length - 2 * thisDegree + 1 ) );

		SplineCurve thisFSC = _thisFSC;
		SplineCurve otherFSC = _otherFSC;
		for ( double knot : _thisKnots ) {
			SplineCurve inserted = thisFSC.insertKnot( knot );
			if ( inserted.knots().length == thisFSC.knots().length ) {
				int index = thisFSC.searchKnotNum( knot, thisDegree - 1, thisFSC.knots().length - thisFSC.degree() );
				index -= ( knot == thisFSC.knots()[thisFSC.knots().length - thisFSC.degree()] ) ? 0 : 1;
				thisFSC = thisFSC.insertMultipleKnots( index, 1 );
			} else {
				thisFSC = inserted;
			}
		}
		for ( double knot : _otherKnots ) {
			SplineCurve inserted = otherFSC.insertKnot( knot );
			if ( inserted.knots().length == otherFSC.knots().length ) {
				int index = otherFSC.searchKnotNum( knot, thisDegree - 1, otherFSC.knots().length - otherFSC.degree() );
				index -= ( knot == otherFSC.knots()[otherFSC.knots().length - otherFSC.degree()] ) ? 0 : 1;
				otherFSC = otherFSC.insertMultipleKnots( index, 1 );
			} else {
				otherFSC = inserted;
			}
		}

		// 重複開始時刻の節点番号を取得する
		int multiplicity = thisFSC.calcKnotMultiplicity( _thisRange.start(), thisFSC.degree() - 1, thisFSC.knots().length - 1 );
		int thisStart = thisFSC.searchKnotNum( _thisRange.start(), thisFSC.degree() - 1, thisFSC.knots().length - 1 );
		thisStart -= ( _thisRange.start() == thisFSC.knots()[thisFSC.knots().length - 1] ? 0 : 1 ) + multiplicity - 1;
		multiplicity = otherFSC.calcKnotMultiplicity( _otherRange.start(), otherFSC.degree() - 1, otherFSC.knots().length - 1 );
		int otherStart = otherFSC.searchKnotNum( _otherRange.start(), otherFSC.degree() - 1, otherFSC.knots().length - 1 );
		otherStart -= ( _otherRange.start() == otherFSC.knots()[otherFSC.knots().length - 1] ? 0 : 1 ) + multiplicity - 1;
		// 重複終了時刻の節点番号を取得する
		int thisEnd = thisFSC.searchKnotNum( _thisRange.end(), thisFSC.degree() - 1, thisFSC.knots().length - 1 );
		thisEnd -= ( _thisRange.end() == thisFSC.knots()[thisFSC.knots().length - 1] ) ? 0 : 1;
		int otherEnd = otherFSC.searchKnotNum( _otherRange.end(), otherFSC.degree() - 1, otherFSC.knots().length - 1 );
		otherEnd -= ( _otherRange.end() == otherFSC.knots()[otherFSC.knots().length - 1] ) ? 0 : 1;

		// 時系列の平均操作を行う
		SplineCurve[] result = blendTime(thisFSC, _thisRange, otherFSC, _otherRange, _weightA, _weightB );
		thisFSC = result[0];
		otherFSC = result[1];

		// 融合制御点列と融合節点列を生成する
		Point[] blendedCtrlPoints = null;
		double[] blendedKnots;
		Range blendedDomainRange = null;
		int blendedStart = 0;
		int blendedEnd = 0;
		switch ( pattern ) {
			case THIS_TO_THIS:
			case THIS_TO_BLENDED:
			case BLENDED_TO_THIS:
			case BLENDED_TO_BLENDED:
				int postStartControlPointsIndex = Math.max( thisEnd - thisDegree + 2, thisStart );
				blendedKnots = thisFSC.knots();
				blendedCtrlPoints = new Point[ blendedKnots.length - thisDegree + 1 ];
				System.arraycopy( thisFSC.controlPoints(), 0, blendedCtrlPoints, 0, thisStart );
				System.arraycopy( thisFSC.controlPoints(), postStartControlPointsIndex,
					blendedCtrlPoints, postStartControlPointsIndex, thisFSC.controlPoints().length - postStartControlPointsIndex );
				blendedDomainRange = thisFSC.range();
				blendedStart = thisStart;
				blendedEnd = thisEnd;
				break;
			case THIS_TO_OTHER:
				postStartControlPointsIndex = Math.max( otherEnd - thisDegree + 2, otherStart );
				blendedKnots = new double[ thisEnd + otherFSC.knots().length - otherEnd ];
				System.arraycopy( thisFSC.knots(), 0, blendedKnots, 0, thisEnd + 1 );
				System.arraycopy( otherFSC.knots(), otherEnd + 1, blendedKnots, thisEnd + 1, otherFSC.knots().length - otherEnd - 1 );
				blendedCtrlPoints = new Point[ blendedKnots.length - thisDegree + 1 ];
				System.arraycopy( thisFSC.controlPoints(), 0, blendedCtrlPoints, 0, thisStart );
				System.arraycopy( otherFSC.controlPoints(), postStartControlPointsIndex,
					blendedCtrlPoints, Math.max( thisEnd - thisDegree + 2, thisStart ), otherFSC.controlPoints().length - postStartControlPointsIndex );
				blendedDomainRange = Range.create( thisFSC.range().start(), otherFSC.range().end() );
				blendedStart = thisStart;
				blendedEnd = thisEnd;
				break;
			case OTHER_TO_THIS:
				postStartControlPointsIndex = Math.max( thisEnd - thisDegree + 2, thisStart );
				blendedKnots = new double[ otherEnd + thisFSC.knots().length - thisEnd ];
				System.arraycopy( otherFSC.knots(), 0, blendedKnots, 0, otherEnd + 1 );
				System.arraycopy( thisFSC.knots(), thisEnd + 1, blendedKnots, otherEnd + 1, thisFSC.knots().length - thisEnd - 1 );
				blendedCtrlPoints = new Point[ blendedKnots.length - thisDegree + 1 ];
				System.arraycopy( otherFSC.controlPoints(), 0, blendedCtrlPoints, 0, otherStart );
				System.arraycopy( thisFSC.controlPoints(), postStartControlPointsIndex,
					blendedCtrlPoints, Math.max( otherEnd - thisDegree + 2, otherStart ), thisFSC.controlPoints().length - postStartControlPointsIndex );
				blendedDomainRange = Range.create( otherFSC.range().start(), thisFSC.range().end() );
				blendedStart = otherStart;
				blendedEnd = otherEnd;
				break;
			case OTHER_TO_OTHER:
			case OTHER_TO_BLENDED:
			case BLENDED_TO_OTHER:
				postStartControlPointsIndex = Math.max( otherEnd - thisDegree + 2, otherStart );
				blendedKnots = otherFSC.knots();
				blendedCtrlPoints = new Point[ blendedKnots.length - thisDegree + 1 ];
				System.arraycopy( otherFSC.controlPoints(), 0, blendedCtrlPoints, 0, otherStart );
				System.arraycopy( otherFSC.controlPoints(), postStartControlPointsIndex,
					blendedCtrlPoints, postStartControlPointsIndex, otherFSC.controlPoints().length - postStartControlPointsIndex );
				blendedDomainRange = otherFSC.range();
				blendedStart = otherStart;
				blendedEnd = otherEnd;
				break;
			default:
				throw new UnsupportedOperationException( pattern.toString() );
		}

		// 重複区間の制御点列に対して平均処理
		for ( int i = thisStart, j = otherStart, k = blendedStart;
			i <= thisEnd - thisDegree + 1 && j <= otherEnd - otherDegree + 1; ++i, ++j, ++k ) {
			blendedCtrlPoints[k] = thisFSC.controlPoints()[i].internalDivision( otherFSC.controlPoints()[j], _weightA, _weightB );
		}

		Range fullBlendedDomainRange = Range.create( blendedKnots[thisDegree - 1], blendedKnots[blendedKnots.length - thisDegree] );
		SplineCurve fullBlendedFSC = SplineCurve.create(thisDegree, blendedCtrlPoints, blendedKnots, fullBlendedDomainRange );
		SplineCurve blendedFSC = fullBlendedFSC.part( blendedDomainRange );

		// 節点間隔を等間隔にするために必要な時間長にする
		multiplicity = blendedFSC.calcKnotMultiplicity( blendedFSC.knots()[blendedFSC.degree() - 1], 0, blendedFSC.knots().length - 1 );
		int minBlendedStartPrev = blendedFSC.searchKnotNum( blendedFSC.knots()[blendedFSC.degree() - 1], 0, blendedFSC.knots().length - 1 );
		minBlendedStartPrev -= ( ( blendedFSC.knots()[blendedFSC.degree() - 1] == blendedFSC.knots()[blendedFSC.knots().length - 1] ) ? 0 : 1 ) + multiplicity - 1;
		int blendedStartPrev = Math.max( blendedStart - 1, minBlendedStartPrev );

		int maxBlendedEndPost = blendedFSC.searchKnotNum( blendedFSC.knots()[blendedFSC.knots().length - blendedFSC.degree()], 0, blendedFSC.knots().length - 1 );
		maxBlendedEndPost -= ( blendedFSC.knots()[blendedFSC.knots().length - blendedFSC.degree()]== blendedFSC.knots()[blendedFSC.knots().length - 1] ) ? 0 : 1;
		int blendedEndPost = Math.min( blendedEnd + 1, maxBlendedEndPost );

		double timeLength = knotsInterval * Math.round( ( blendedKnots[blendedEndPost] - blendedKnots[blendedStartPrev] ) / knotsInterval );
		blendedFSC = blendedFSC.matchTimeLength( blendedStartPrev, blendedEndPost, timeLength );
		fullBlendedFSC = fullBlendedFSC.matchTimeLength( blendedStartPrev, blendedEndPost, timeLength );

		// ファジネスの近似補間のために融合曲線の評価点列を取得
		Point[] evaluatedPoints = fullBlendedFSC.evaluateAll( Math.max( (int) Math.ceil( fullBlendedDomainRange.length() / 0.01 ), 2 ), ParametricEvaluable.EvaluationType.TIME );

		// 節点間隔を等間隔にするために必要な節点を挿入
		double start = blendedFSC.knots()[blendedStartPrev];
		double end = blendedFSC.knots()[blendedEndPost];

		long num = Math.round( ( end - start ) / knotsInterval );
		for ( long i = 1; i < num; ++i ) {
			double w = i / (double) num;
			blendedFSC = blendedFSC.insertKnot( ( 1 - w ) * start + w * end );
		}

		// 融合区間の等間隔の節点以外を除去する
		for ( int i = blendedStartPrev, j = 0;
			i < blendedFSC.knots().length && blendedFSC.knots()[i] <= end; ) {
			if ( i > thisDegree - 1 ) {
				while ( i < blendedFSC.knots().length - thisDegree
					&& blendedFSC.knots()[i + 1] == blendedFSC.knots()[i] ) { // 多重節点の場合、1重節点の状態にする
					blendedFSC = blendedFSC.deleteKnot( i );
				}
			}
			if ( i >= thisDegree - 1
				&& i <= blendedFSC.knots().length - thisDegree ) {
				double w = j / (double) num;
				if ( blendedFSC.knots()[i] != ( 1 - w ) * start + w * end ) {
					blendedFSC = blendedFSC.deleteKnot( i );
					continue;
				} else {
					++j;
				}
			}
			++i;
		}

		blendedFSC = FuzzySplineCurveCreater.create( blendedFSC, evaluatedPoints );

		return blendedFSC;
	}

	/**
	 * 指定したスプライン曲線の時系列融合を行います。
	 * @param _thisFSC ファジィスプライン曲線A
	 * @param _thisRange ファジィスプライン曲線Aの範囲
	 * @param _otherFSC ファジィスプライン曲線B
	 * @param _otherRange ファジィスプライン曲線Bの範囲
	 * @param _weightA 内分比A
	 * @param _weightB 内分比B
	 * @return 時系列融合後のスプライン曲線列
	 */
	private SplineCurve[] blendTime(SplineCurve _thisFSC, Range _thisRange,
		SplineCurve _otherFSC, Range _otherRange, double _weightA, double _weightB ) {
		// 重複の開始時刻を揃える
		double shift = _thisRange.start() - _otherRange.start();
		SplineCurve thisFSC = _thisFSC;
		SplineCurve otherFSC = _otherFSC.shiftTimeSeries( shift, 0 );
		Range otherRange = Range.create( _otherRange.start() + shift, _otherRange.end() + shift );

		// 開始時刻の節点番号を取得する
		int thisKnotNum = thisFSC.searchKnotNum( _thisRange.start(), thisFSC.degree() - 1, thisFSC.knots().length - thisFSC.degree() );
		thisKnotNum -= ( _thisRange.start() == thisFSC.knots()[thisFSC.knots().length - thisFSC.degree()] ) ? 0 : 1;
		int otherKnotNum = otherFSC.searchKnotNum( otherRange.start(), otherFSC.degree() - 1, otherFSC.knots().length - otherFSC.degree() );
		otherKnotNum -= ( otherRange.start() == otherFSC.knots()[otherFSC.knots().length - otherFSC.degree()] ) ? 0 : 1;
		// 開始時刻から終了時刻未満までの新しい節点列を生成する
		double sumOfRatio = _weightA + _weightB;
		double[] thisKnots = thisFSC.knots();
		double[] otherKnots = otherFSC.knots();
		int i = thisKnotNum;
		int j = otherKnotNum;
		while ( i < thisKnots.length
			&& j < otherKnots.length
			&& ( thisKnots[i] < _thisRange.end() || otherKnots[j] < otherRange.end() ) ) {
			double knot = ( _weightB * thisKnots[i] + _weightA * otherKnots[j] ) / sumOfRatio;
			thisKnots[i] = knot;
			otherKnots[j] = knot;
			++i;
			++j;
		}
		if ( i >= thisKnots.length
			|| j >= otherKnots.length
			|| thisKnots[i] != _thisRange.end()
			|| otherKnots[j] != otherRange.end() ) {
			throw new IllegalArgumentException();
		}

		// 終了時刻以降の新しい節点列を生成する
		double knot = ( _weightB * thisKnots[i] + _weightA * otherKnots[j] ) / sumOfRatio;
		shift = knot - thisKnots[i];
		while ( i < thisKnots.length ) {
			thisKnots[i] += shift;
			++i;
		}
		Range thisDomainRange = Range.create( thisFSC.range().start(), thisFSC.range().end() + shift );
		shift = knot - otherKnots[j];
		while ( j < otherKnots.length ) {
			otherKnots[j] += shift;
			++j;
		}
		Range otherDomainRange = Range.create( otherFSC.range().start(), otherFSC.range().end() + shift );

		return new SplineCurve[] {
				SplineCurve.create( thisFSC.degree(), thisFSC.controlPoints(), thisKnots, thisDomainRange ),
				SplineCurve.create( otherFSC.degree(), otherFSC.controlPoints(), otherKnots, otherDomainRange )
			};
	}

	/**
	 * 融合パターンを判別します。
	 * @param _thisRange この曲線の重複区間
	 * @param _otherFSC この曲線と融合する曲線
	 * @param _otherRange この曲線と融合する曲線の重複区間
	 * @return 融合パターン
	 * @throws OutOfRangeException 指定されたレンジが曲線のレンジ外である場合
	 */
	private BlendPattern blendPattern( SplineCurve _thisFSC, Range _thisRange,
		SplineCurve _otherFSC, Range _otherRange ) {
		if ( !_thisFSC.range().isInner( _thisRange ) || !_otherFSC.range().isInner( _otherRange ) ) {
			throw new OutOfRangeException( "range is out of domain." );
		}
		Range thisDomain = _thisFSC.range();
		Range otherDomain = _otherFSC.range();

		boolean tA = _thisRange.start() == thisDomain.start();
		boolean tB = _thisRange.end() == thisDomain.end();
		boolean oA = _otherRange.start() == otherDomain.start();
		boolean oB = _otherRange.end() == otherDomain.end();

		BlendPattern pattern;
		if ( tA && tB ) {
			if ( oA && oB ) {
				pattern = BlendPattern.BLENDED_TO_BLENDED;
			} else if ( oA && !oB ) {
				pattern = BlendPattern.BLENDED_TO_OTHER;
			} else if ( !oA && !oB ) {
				pattern = BlendPattern.OTHER_TO_OTHER;
			} else /* if( !oA && oB ) */ {
				pattern = BlendPattern.OTHER_TO_BLENDED;
			}
		} else if ( tA && !tB ) {
			if ( oA && oB ) {
				pattern = BlendPattern.BLENDED_TO_THIS;
			} else if ( oA && !oB ) {
				pattern = BlendPattern.BLENDED_TO_BOTH;
			} else if ( !oA && !oB ) {
				pattern = BlendPattern.OTHER_TO_BOTH;
			} else /* if( !oA && oB ) */ {
				pattern = BlendPattern.OTHER_TO_THIS;
			}
		} else if ( !tA && !tB ) {
			if ( oA && oB ) {
				pattern = BlendPattern.THIS_TO_THIS;
			} else if ( oA && !oB ) {
				pattern = BlendPattern.THIS_TO_BOTH;
			} else if ( !oA && !oB ) {
				pattern = BlendPattern.BOTH_TO_BOTH;
			} else /* if( !oA && oB ) */ {
				pattern = BlendPattern.BOTH_TO_THIS;
			}
		} else /* if( !eA && eB ) */ {
			if ( oA && oB ) {
				pattern = BlendPattern.THIS_TO_BLENDED;
			} else if ( oA && !oB ) {
				pattern = BlendPattern.THIS_TO_OTHER;
			} else if ( !oA && !oB ) {
				pattern = BlendPattern.BOTH_TO_OTHER;
			} else /* if( !oA && oB ) */ {
				pattern = BlendPattern.BOTH_TO_BLENDED;
			}
		}

		return pattern;
	}
	
	private SplineCurveBlender( Sigmoid _figuralOverlappingTrue, Sigmoid _overlappingStateTrue, double _fscConvertVelocityCoeff, double _fscConvertAccelerationCoeff ) {
		m_figuralOverlappingTrue = _figuralOverlappingTrue;
		m_overlappingStateTrue = _overlappingStateTrue;
		m_fscConvertAccelerationCoeff = _fscConvertAccelerationCoeff;
		m_fscConvertVelocityCoeff = _fscConvertVelocityCoeff;
	}
	
	/** 形状的重複の言語的真理値「真」 */
	private final Sigmoid m_figuralOverlappingTrue;
	/** 重複状態の言語的真理値「真」 */
	private final Sigmoid m_overlappingStateTrue;
	/** FSC変換の速度係数 */
	private final double m_fscConvertVelocityCoeff;
	/** FSC変換の加速度係数 */
	private final double m_fscConvertAccelerationCoeff;

	/** 融合可能パターンセット */
	// TODO ここまで融合パターン分けをしなくてはならない理由を調査
	private static final EnumSet<BlendPattern> BLEND＿ENABLE＿SET = EnumSet.of(
		BlendPattern.THIS_TO_THIS,
		BlendPattern.THIS_TO_OTHER,
		BlendPattern.OTHER_TO_THIS,
		BlendPattern.OTHER_TO_OTHER,
		BlendPattern.BLENDED_TO_THIS,
		BlendPattern.BLENDED_TO_OTHER,
		BlendPattern.THIS_TO_BLENDED,
		BlendPattern.OTHER_TO_BLENDED,
		BlendPattern.BLENDED_TO_BLENDED );
}
