package jp.sagalab.jftk.blend;

import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.convex.ConvexHull;

/**
 * 重複範囲を探索するためのクラスです。
 * @author kaneko
 */
public class OverlappingRangeFinder {
	
	/**
	 * 指定された2曲線の重複範囲を探索します。
	 * @param _existed 既存曲線
	 * @param _overlapped 重ね書き曲線
	 * @param _threshold 閾値
	 * @return 重複範囲
	 * @throws IllegalArgumentException 既存曲線がnullである場合
	 * @throws IllegalArgumentException 重ね書き曲線がnullである場合
	 * @throws IllegalArgumentException 閾値がNaNもしくはInfinityの場合
	 * @throws IllegalArgumentException 次数が異なる場合
	 */
	public static OverlappingRange[] find( SplineCurve _existed, SplineCurve _overlapped, double _threshold ) {
		if ( _existed == null ) {
			throw new IllegalArgumentException( " _existed is null " );
		}
		if ( _overlapped == null ) {
			throw new IllegalArgumentException( " overlapped is null " );
		}
		if ( Double.isInfinite( _threshold ) || Double.isNaN( _threshold ) ) {
			throw new IllegalArgumentException( " _threshold is NaN or Infinite " );
		}
		if ( _existed.degree() != _overlapped.degree() ) {
			throw new IllegalArgumentException( "_existed's degree not equal _overlapped's degree" );
		}
		// 凸包単位で重複区間探索
		List<List<Range>> rangePairs = searchConvexHullOverlappingRangePairs( _existed, _overlapped, 0.0 );
		List<Range> existedRange = rangePairs.get( 0 );
		List<Range> overlappedRange = rangePairs.get( 1 );
		if ( existedRange.size() > 0 && overlappedRange.size() > 0 ) {
			return searchOverlappingRanges( _existed, existedRange, _overlapped, overlappedRange, _threshold );
		} else {
			return new OverlappingRange[ 0 ];
		}
	}

	/**
	 * 指定された2曲線の重複範囲のペアを凸包単位で探索して，重複開始区間探索領域のペアを返します。
	 * @param _existed 既存曲線
	 * @param _overlapped 重ね書き曲線
	 * @param _threshold 閾値
	 * @return 重複開始区間探索領域のペアのペア
	 */
	private static List<List<Range>> searchConvexHullOverlappingRangePairs( SplineCurve _existed, SplineCurve _overlapped, double _threshold ) {
		Range existedRange = _existed.range();
		Range overlappedRange = _overlapped.range();
		_existed = insertMultipleKnotsAtStartAndEnd( _existed, existedRange );
		_overlapped = insertMultipleKnotsAtStartAndEnd( _overlapped, overlappedRange );
		Point[] existedCP = _existed.controlPoints();
		Point[] overlappedCP = _overlapped.controlPoints();
		double[] existedKnots = _existed.knots();
		double[] overlappedKnots = _overlapped.knots();
		int existedStartIndex = _existed.searchKnotNum( existedRange.start(), _existed.degree() - 1, existedKnots.length - _existed.degree() );
		int overlappedStartIndex = _overlapped.searchKnotNum( overlappedRange.start(), _overlapped.degree() - 1, overlappedKnots.length - _overlapped.degree() );

		// 凸包列を生成
		List<ConvexHull> convexHullList = new ArrayList<ConvexHull>();
		ConvexHull[] existedConvexHulls;
		ConvexHull[] overlappedConvexHulls;
		for ( int i = existedStartIndex; existedKnots[i - 1] < existedRange.end(); ++i ) {
			Point[] cp = Arrays.copyOfRange( existedCP, i - _existed.degree(), i + 1 );
			convexHullList.add( ConvexHull.create( cp, ConvexHull.Dimension.THREE_DIMENSION ) );
		}
		existedConvexHulls = convexHullList.toArray( new ConvexHull[ convexHullList.size() ] );
		convexHullList.clear();
		for ( int i = overlappedStartIndex; overlappedKnots[i - 1] < overlappedRange.end(); ++i ) {
			Point[] cp = Arrays.copyOfRange( overlappedCP, i - _overlapped.degree(), i + 1 );
			convexHullList.add( ConvexHull.create( cp, ConvexHull.Dimension.THREE_DIMENSION ) );
		}
		overlappedConvexHulls = convexHullList.toArray( new ConvexHull[ convexHullList.size() ] );

		// 重複開始インデックス取得
		List<int[]> indexPairs = searchOverlappingStartConvexHullIndexPairs( existedConvexHulls, overlappedConvexHulls, _threshold );

		List<List<Range>> rangePairs = new ArrayList<List<Range>>();
		rangePairs.add( new ArrayList<Range>() );
		rangePairs.add( new ArrayList<Range>() );
		// 凸包単位の経路探索
		PAIR_LOOP:
		for ( int[] indexPair : indexPairs ) {
			int i = indexPair[0];
			int j = indexPair[1];
			while ( i + 1 < existedConvexHulls.length && j + 1 < overlappedConvexHulls.length ) {
				double[] candidatePos = {
					existedConvexHulls[i + 1].includedIn( overlappedConvexHulls[j + 1] ).possibility(),
					existedConvexHulls[i].includedIn( overlappedConvexHulls[j + 1] ).possibility(),
					existedConvexHulls[i + 1].includedIn( overlappedConvexHulls[j] ).possibility()
				};
				if ( Math.max( candidatePos[0], Math.max( candidatePos[1], candidatePos[2] ) ) > _threshold ) {
					if ( candidatePos[0] >= Math.max( candidatePos[1], candidatePos[2] ) ) {
						++i;
						++j;
					} else if ( candidatePos[1] >= Math.max( candidatePos[2], candidatePos[0] ) ) {
						++j;
					} else {
						++i;
					}
				} else {
					continue PAIR_LOOP;
				}
			}
			// 重複経路の末端部分の探索
			if ( i + 1 >= existedConvexHulls.length ) {
				while ( j + 1 < overlappedConvexHulls.length ) {
					double pos = existedConvexHulls[i].includedIn( overlappedConvexHulls[j + 1] ).possibility();
					if ( pos > _threshold ) {
						++j;
					} else {
						break;
					}
				}
			}
			if ( j + 1 >= overlappedConvexHulls.length ) {
				while ( i + 1 < existedConvexHulls.length ) {
					double pos = existedConvexHulls[i + 1].includedIn( overlappedConvexHulls[j] ).possibility();
					if ( pos > _threshold ) {
						++i;
					} else {
						break;
					}
				}
			}

			// 探索して見つけた重複区間に対して，重複開始区間の探索領域を調べる
			boolean isStartOfExisted = ( indexPair[0] == 0 );
			boolean isStartOfOverlapped = ( indexPair[1] == 0 );
			if ( isStartOfExisted ) {
				ConvexHull existedConvexHull = existedConvexHulls[indexPair[0]];
				int k = indexPair[1] + 1;
				while ( k <= j && existedConvexHull.includedIn( overlappedConvexHulls[k] ).possibility() > _threshold ) {
					++k;
				}
				if ( !isStartOfOverlapped ) {
					rangePairs.get( 0 ).add( Range.create( existedKnots[existedStartIndex + indexPair[0] - 1], existedKnots[existedStartIndex + indexPair[0]] ) );
				}
				rangePairs.get( 1 ).add( Range.create( overlappedKnots[overlappedStartIndex + indexPair[1] - 1], overlappedKnots[overlappedStartIndex + k - 1] ) );
			}
			if ( isStartOfOverlapped ) {
				ConvexHull overlappedConvexHull = overlappedConvexHulls[indexPair[1]];
				int k = indexPair[0] + 1;
				while ( k <= i && existedConvexHulls[k].includedIn( overlappedConvexHull ).possibility() > _threshold ) {
					++k;
				}
				if ( !isStartOfExisted ) {
					rangePairs.get( 1 ).add( Range.create( overlappedKnots[overlappedStartIndex + indexPair[1] - 1], overlappedKnots[overlappedStartIndex + indexPair[1]] ) );
				}
				rangePairs.get( 0 ).add( Range.create( existedKnots[existedStartIndex + indexPair[0] - 1], existedKnots[existedStartIndex + k - 1] ) );
			}
		}

		return rangePairs;
	}

	/**
	 * 指定された凸包列の重複開始インデックスのペアを探索します。
	 * @param _existed 既存曲線の凸包列
	 * @param _overlapped 重ね書き曲線の凸包列
	 * @param _threshold 閾値
	 * @return 重複開始インデックスのペア
	 */
	private static List<int[]> searchOverlappingStartConvexHullIndexPairs( ConvexHull[] _existed, ConvexHull[] _overlapped, double _threshold ) {
		List<int[]> starts = new ArrayList<int[]>();

		double prev = 0.0;
		for ( int i = 0; i < _existed.length; ++i ) {
			double now = _existed[i].includedIn( _overlapped[0] ).possibility();
			if ( prev <= _threshold && now > _threshold ) {
				starts.add( new int[]{ i, 0 } );
			}
			prev = now;
		}

		prev = _existed[0].includedIn( _overlapped[0] ).possibility();
		for ( int i = 1; i < _overlapped.length; ++i ) {
			double now = _existed[0].includedIn( _overlapped[i] ).possibility();
			if ( prev <= _threshold && now > _threshold ) {
				starts.add( new int[]{ 0, i } );
			}
			prev = now;
		}

		return starts;
	}

	/**
	 * 重複範囲を探索します。
	 * @param _existed 既存曲線
	 * @param _overlapped 重ね書き曲線
	 * @param _threshold 閾値
	 * @return 重複範囲
	 */
	private static OverlappingRange[] searchOverlappingRanges( SplineCurve _existed, List<Range> _existedSearchingRange, SplineCurve _overlapped, List<Range> _overlappedSearchingRange, double _threshold ) {
		int degree = _existed.degree();

		Range eRange = _existed.range();
		Range oRange = _overlapped.range();
		double existedEnd = eRange.end();
		double overlappedEnd = oRange.end();
		// 指定された曲線に対して定義域の終端で節点挿入する
		SplineCurve unprocessedExistedFSC = _existed.insertKnot( existedEnd );
		SplineCurve unprocessedOverlappedFSC = _overlapped.insertKnot( overlappedEnd );
		double[] unprocessedExistedKnots = unprocessedExistedFSC.knots();
		double[] unprocessedOverlappedKnots = unprocessedOverlappedFSC.knots();

		// 指定された範囲を細分化する
		SplineCurve existedSegmentalizedFSC = _existed;
		SplineCurve overlappedSegmentalizedFSC = _overlapped;
		existedSegmentalizedFSC = segmentalize( existedSegmentalizedFSC, _threshold );
		overlappedSegmentalizedFSC = segmentalize( overlappedSegmentalizedFSC, _threshold );

		List<OverlappingRange> result = new ArrayList<OverlappingRange>();
		// 重複開始時刻を取得する
		List<double[]> startPairs = searchOverlappingStartKnotPairs( existedSegmentalizedFSC, _existedSearchingRange, overlappedSegmentalizedFSC, _overlappedSearchingRange );
		// 重複区間の探索を行う
		for ( double[] startPair : startPairs ) {
			// 重複開始時刻で多重節点挿入する
			SplineCurve existedFSC = existedSegmentalizedFSC;
			SplineCurve overlappedFSC = overlappedSegmentalizedFSC;
			double[] existedKnots = existedFSC.knots();
			double[] overlappedKnots = overlappedFSC.knots();

			// 重複開始時刻を含む区間番号を取得する
			int h = unprocessedExistedFSC.searchKnotNum( startPair[0], degree - 1, unprocessedExistedKnots.length - degree );
			int i = unprocessedOverlappedFSC.searchKnotNum( startPair[1], degree - 1, unprocessedOverlappedKnots.length - degree );
			int j = existedFSC.searchKnotNum( startPair[0], degree - 1, existedKnots.length - degree );
			int k = overlappedFSC.searchKnotNum( startPair[1], degree - 1, overlappedKnots.length - degree );
			int existedStartIndex = j - 1;
			int overlappedStartIndex = k - 1;

			// 運動的重複度と形状的重複度の初期値を設定する
			TruthValue figural = existedFSC.overlappedWith( j, overlappedFSC, k );
			// XXX kineticRatioは描画運動の類似度を示す指標として扱っているが，この数値の算出方法はまだ十分な議論がなされていない。
			// 現状は，対応付けした各節点区間の時間長がどの程度異なっているかで算出している
			double kineticRatio = 0.0;
			boolean existedInsertedFlag = false;
			boolean overlappedInsertedFlag = false;
			// 重複区間の末端までの重複経路を探索する
			while ( existedKnots[j] < existedEnd && overlappedKnots[k] < overlappedEnd ) {
				// 重複区間の形状的重複度を更新する
				TruthValue tv = existedFSC.overlappedWith( j + 1, overlappedFSC, k + 1 );
				figural = TruthValue.create( Math.min( tv.necessity(), figural.necessity() ),
					Math.min( tv.possibility(), figural.possibility() ) );
				boolean expandedFlag = false;
				while ( existedKnots[j + 1] < existedEnd && overlappedKnots[k + 1] < overlappedEnd
					&& ( existedKnots[j] < unprocessedExistedKnots[h] || overlappedKnots[k] < unprocessedOverlappedKnots[i] ) ) {
					// 現状の区間を1区間広げた区間を求める
					SplineCurve[] splines = expandPeriod( existedFSC, j, overlappedFSC, k,
						expandedFlag, existedInsertedFlag, overlappedInsertedFlag,
						existedKnots[j] == unprocessedExistedKnots[h],
						overlappedKnots[k] == unprocessedOverlappedKnots[i] );
					// 広げた区間の運動的重複度と形状的重複度を求める
					TruthValue kineticTV = splines[0].overlappedWith( j, splines[1], k );
					TruthValue figuralTV = splines[0].overlappedWith( j + 1, splines[1], k + 1 );
					// 区間を広げた後の重複可能性値が閾値より高い場合，現状の区間を広げる
					if ( Math.min( kineticTV.possibility(), figuralTV.possibility() ) > _threshold ) {
						if ( splines[0] != existedFSC ) {
							existedFSC = splines[0];
							existedKnots = existedFSC.knots();
						}
						if ( splines[1] != overlappedFSC ) {
							overlappedFSC = splines[1];
							overlappedKnots = overlappedFSC.knots();
						}
						expandedFlag = true;
					} else { // 現状の区間をこれ以上広げられない場合
						double existedInterval = existedKnots[j] - existedKnots[j - 1];
						double overlappedInterval = overlappedKnots[k] - overlappedKnots[k - 1];
						kineticRatio += Math.min( existedInterval, overlappedInterval )
							/ Math.max( existedInterval, overlappedInterval );
						// 形状的重複可能性値が最も高い区間の進め方を選択
						splines = selectMostFiguralOverlapping( existedFSC, j, overlappedFSC, k );
						existedInsertedFlag = overlappedInsertedFlag = false;
						if ( splines[0] != existedFSC ) {
							existedFSC = splines[0];
							existedKnots = existedFSC.knots();
							existedInsertedFlag = true;
						} else if ( splines[1] != overlappedFSC ) {
							overlappedFSC = splines[1];
							overlappedKnots = overlappedFSC.knots();
							overlappedInsertedFlag = true;
						}
						expandedFlag = false;
						if ( existedKnots[j] == unprocessedExistedKnots[h] ) {
							++h;
						}
						if ( overlappedKnots[k] == unprocessedOverlappedKnots[i] ) {
							++i;
						}
						// 次の区間へ
						++j;
						++k;
					}
					// 重複区間の形状的重複度を更新する
					tv = existedFSC.overlappedWith( j + 1, overlappedFSC, k + 1 );
					figural = TruthValue.create( Math.min( tv.necessity(), figural.necessity() ),
						Math.min( tv.possibility(), figural.possibility() ) );
				}
				if ( ( existedKnots[j + 1] < existedEnd && overlappedKnots[k + 1] < overlappedEnd ) ) {
					double existedInterval = existedKnots[j] - existedKnots[j - 1];
					double overlappedInterval = overlappedKnots[k] - overlappedKnots[k - 1];
					kineticRatio += Math.min( existedInterval, overlappedInterval )
						/ Math.max( existedInterval, overlappedInterval );
					// 形状的重複可能性値が最も高い区間の進め方を選択					
					SplineCurve[] splines = selectMostFiguralOverlapping( existedFSC, j, overlappedFSC, k );
					existedInsertedFlag = overlappedInsertedFlag = false;
					if ( splines[0] != existedFSC ) {
						existedFSC = splines[0];
						existedKnots = existedFSC.knots();
						existedInsertedFlag = true;
					} else if ( splines[1] != overlappedFSC ) {
						overlappedFSC = splines[1];
						overlappedKnots = overlappedFSC.knots();
						overlappedInsertedFlag = true;
					}
					++h;
					++i;
				}
				// 次の区間へ
				++j;
				++k;
			}
			// 重複経路の末端部分の探索
			TruthValue tailTV = existedFSC.overlappedWith( j, overlappedFSC, k );
			while ( existedKnots[j] < existedEnd
				&& existedKnots[j - 1] < unprocessedExistedKnots[h] ) {
				SplineCurve expanded = existedFSC.deleteKnot( j - 1 );
				TruthValue kineticTV = expanded.overlappedWith( j - 1, overlappedFSC, k - 1 );
				TruthValue figuralTV = expanded.overlappedWith( j, overlappedFSC, k );
				if ( figuralTV.possibility() >= tailTV.possibility()
					&& kineticTV.possibility() > _threshold ) {
					tailTV = figuralTV;
					existedFSC = expanded;
					existedKnots = existedFSC.knots();
				} else {
					break;
				}
			}
			while ( overlappedKnots[k] < overlappedEnd
				&& overlappedKnots[k - 1] < unprocessedOverlappedKnots[i] ) {
				SplineCurve expandedFSC = overlappedFSC.deleteKnot( k - 1 );
				TruthValue kineticTV = existedFSC.overlappedWith( j - 1, expandedFSC, k - 1 );
				TruthValue figuralTV = existedFSC.overlappedWith( j, expandedFSC, k );
				if ( figuralTV.possibility() >= tailTV.possibility()
					&& kineticTV.possibility() > _threshold ) {
					tailTV = figuralTV;
					overlappedFSC = expandedFSC;
					overlappedKnots = overlappedFSC.knots();
				} else {
					break;
				}
			}
			// 探索における最後の区間を広げることができるか検討する
			boolean isExpandedEnd = false;
			if ( existedKnots[j] == existedEnd && j - 1 > existedStartIndex
				&& existedKnots[j - 1] < unprocessedExistedKnots[h] ) {
				SplineCurve[] existedCandidate;
				SplineCurve[] overlappedCandidate;
				SplineCurve expandedExisted = existedFSC.deleteKnot( j - 1 );
				if ( k - 1 > overlappedStartIndex
					&& overlappedKnots[k - 1] < unprocessedOverlappedKnots[i] ) {
					SplineCurve expandedOverlapped = overlappedFSC.deleteKnot( k - 1 );
					existedCandidate = new SplineCurve[]{ expandedExisted, expandedExisted };
					overlappedCandidate = new SplineCurve[]{ expandedOverlapped, overlappedFSC };
				} else {
					existedCandidate = new SplineCurve[]{ expandedExisted };
					overlappedCandidate = new SplineCurve[]{ overlappedFSC };
				}
				SplineCurve[] splines = selectMostKineticOverlapping( existedCandidate, j - 1, overlappedCandidate, k - 1 );
				TruthValue kineticTV = splines[0].overlappedWith( j - 1, splines[1], k - 1 );
				if ( kineticTV.possibility() > _threshold ) {
					existedFSC = splines[0];
					existedKnots = existedFSC.knots();
					if ( splines[1] != overlappedFSC ) {
						overlappedFSC = splines[1];
						overlappedKnots = overlappedFSC.knots();
					}
					isExpandedEnd = true;
				}
			}
			if ( overlappedKnots[i] == overlappedEnd && k - 1 > overlappedStartIndex
				&& overlappedKnots[k - 1] < unprocessedOverlappedKnots[i] ) {
				SplineCurve[] existedCandidate;
				SplineCurve[] overlappedCandidate;
				SplineCurve expandedOverlapped = overlappedFSC.deleteKnot( k - 1 );
				if ( j - 1 > existedStartIndex
					&& existedKnots[j - 1] < unprocessedExistedKnots[h] ) {
					SplineCurve expandedExisted = existedFSC.deleteKnot( j - 1 );
					existedCandidate = new SplineCurve[]{ expandedExisted, existedFSC };
					overlappedCandidate = new SplineCurve[]{ expandedOverlapped, expandedOverlapped };
				} else {
					existedCandidate = new SplineCurve[]{ existedFSC };
					overlappedCandidate = new SplineCurve[]{ expandedOverlapped };
				}
				SplineCurve[] splines = selectMostKineticOverlapping( existedCandidate, j - 1, overlappedCandidate, k - 1 );
				TruthValue kineticTV = splines[0].overlappedWith( j - 1, splines[1], k - 1 );
				if ( kineticTV.possibility() > _threshold ) {
					if ( splines[0] != existedFSC ) {
						existedFSC = splines[0];
						existedKnots = existedFSC.knots();
					}
					overlappedFSC = splines[1];
					overlappedKnots = overlappedFSC.knots();
					isExpandedEnd = true;
				}
			}

			if ( j - 1 > existedStartIndex && k - 1 > overlappedStartIndex ) {
				double existedInterval = existedKnots[j - 1] - existedKnots[j - 2];
				double overlappedInterval = overlappedKnots[k - 1] - overlappedKnots[k - 2];
				kineticRatio += Math.min( existedInterval, overlappedInterval )
					/ Math.max( existedInterval, overlappedInterval );
			}
			if ( !isExpandedEnd ) {
				double existedInterval = existedKnots[j] - existedKnots[j - 1];
				double overlappedInterval = overlappedKnots[k] - overlappedKnots[k - 1];
				kineticRatio += Math.min( existedInterval, overlappedInterval )
					/ Math.max( existedInterval, overlappedInterval );
			}
			// 重複区間の範囲を生成
			int existedEndIndex = j - ( isExpandedEnd ? 1 : 0 );
			int overlappedEndIndex = k - ( isExpandedEnd ? 1 : 0 );
			Range existedRange = Range.create( existedKnots[existedStartIndex], existedKnots[existedEndIndex] );
			Range overlappedRange = Range.create( overlappedKnots[overlappedStartIndex], overlappedKnots[overlappedEndIndex] );
			// 挿入すべき節点列を生成
			existedKnots = searchDifferenceKnots( _existed, existedFSC, existedStartIndex, existedEndIndex );
			overlappedKnots = searchDifferenceKnots( _overlapped, overlappedFSC, overlappedStartIndex, overlappedEndIndex );
			// 運動的重複率を算出
			kineticRatio /= existedEndIndex - existedStartIndex;
			// 長さ重複率を算出
			// XXX 長さ重複率は曲線の長さに関して？時間長に関して？
//			double lengthRatio = _overlapped.part( overlappedRange ).length() / overlappedAllLength;
			double lengthRatio = overlappedRange.length() / oRange.length();
			result.add( OverlappingRange.create( existedRange, existedKnots, overlappedRange, overlappedKnots,
				figural.possibility(), kineticRatio, lengthRatio ) );
		}

		return result.toArray( new OverlappingRange[ result.size() ] );
	}

	/**
	 * 指定された2曲線の重複開始時刻のペアを探索します。
	 * @param _existed 既存曲線
	 * @param _existedSearchingRanges 既存曲線の重複開始区間探索領域
	 * @param _overlapped 重ね書き曲線
	 * @param _overlappedSearchingRanges 重ね書き曲線の重複開始区間探索領域
	 * @return 重複開始時刻ペア
	 * @throws IllegalArgumentException 既存曲線の重複開始区間探索領域の長さと重ね書き曲線の重複開始区間探索領域が異なる場合
	 */
	private static List<double[]> searchOverlappingStartKnotPairs( SplineCurve _existed, List<Range> _existedSearchingRanges,
		SplineCurve _overlapped, List<Range> _overlappedSearchingRanges ) {
		if ( _existedSearchingRanges.size() != _overlappedSearchingRanges.size() ) {
			throw new IllegalArgumentException( "_existedSearchingRanges's size not equal _overlappedSearchingRanges's size" );
		}
		int degree = _existed.degree();
		Range existedRange = _existed.range();
		Range overlappedRange = _overlapped.range();
		_existed = insertMultipleKnotsAtStartAndEnd( _existed, existedRange );
		_overlapped = insertMultipleKnotsAtStartAndEnd( _overlapped, overlappedRange );
		double[] existedKnots = _existed.knots();
		double[] overlappedKnots = _overlapped.knots();

		boolean isAdded = false;
		List<double[]> starts = new ArrayList<double[]>();
		int size = _existedSearchingRanges.size();
		for ( int i = 0; i < size; ++i ) {
			Range existedSearchingRange = _existedSearchingRanges.get( i );
			Range overlappedSearchingRange = _overlappedSearchingRanges.get( i );
			boolean isStartOfExisted = existedSearchingRange.start() == existedRange.start();
			boolean isStartOfOverlapped = overlappedSearchingRange.start() == overlappedRange.start();
			if ( !isStartOfExisted && !isStartOfOverlapped ) {
				throw new IllegalArgumentException();
			}

			int existedIndex = _existed.searchKnotNum( existedSearchingRange.start(), degree - 1, existedKnots.length - degree );
			int overlappedIndex = _overlapped.searchKnotNum( overlappedSearchingRange.start(), degree - 1, overlappedKnots.length - degree );

			double startPos = _existed.overlappedWith( existedIndex, _overlapped, overlappedIndex ).possibility();
			double existedPost = 0.0;
			double overlappedPost = 0.0;
			// 既存曲線の中で重ね書き曲線の先頭区間と重なる区間を探索
			if ( isStartOfOverlapped && existedIndex + 1 <= existedKnots.length - degree ) {
				double prev = startPos;
				existedPost = ( existedKnots[existedIndex + 1] <= existedRange.end() ) ? _existed.overlappedWith( existedIndex + 1, _overlapped, overlappedIndex ).possibility() : 0.0;
				for ( int j = existedIndex + 1; existedKnots[j] <= existedRange.end(); ++j ) {
					double now = _existed.overlappedWith( j, _overlapped, overlappedIndex ).possibility();
					double post = ( j + 1 <= existedKnots.length - degree )
						? _existed.overlappedWith( j + 1, _overlapped, overlappedIndex ).possibility() : 0.0;
					if ( prev < now && now >= post ) {
						starts.add( new double[]{ existedKnots[j - 1], overlappedKnots[overlappedIndex - 1] } );
					}
					if ( existedKnots[j] >= existedSearchingRange.end() ) {
						break;
					}
					prev = now;
				}
			}
			// 重ね書き曲線の中で既存曲線の先頭区間と重なる区間を探索
			if ( isStartOfExisted && overlappedIndex + 1 <= overlappedKnots.length - degree ) {
				double prev = startPos;
				overlappedPost = ( overlappedKnots[overlappedIndex + 1] <= overlappedRange.end() ) ? _existed.overlappedWith( existedIndex, _overlapped, overlappedIndex + 1 ).possibility() : 0.0;
				for ( int j = overlappedIndex + 1; overlappedKnots[j] <= overlappedRange.end(); ++j ) {
					double now = _existed.overlappedWith( existedIndex, _overlapped, j ).possibility();
					double post = ( j + 1 <= overlappedKnots.length - degree )
						? _existed.overlappedWith( existedIndex, _overlapped, j + 1 ).possibility() : 0.0;
					if ( prev < now && now >= post ) {
						starts.add( new double[]{ existedKnots[existedIndex - 1], overlappedKnots[j - 1] } );
					}
					if ( overlappedKnots[j] >= overlappedSearchingRange.end() ) {
						break;
					}
					prev = now;
				}
			}
			double now = startPos;
			if ( !isAdded && ( isStartOfExisted && isStartOfOverlapped )
				&& 0.0 < now && ( now >= existedPost || now >= overlappedPost ) ) {
				starts.add( new double[]{ existedKnots[existedIndex - 1], overlappedKnots[overlappedIndex - 1] } );
				isAdded = true;
			}
		}
		return starts;
	}
	
	/**
	 * 指定された2曲線の節点の差分を探索します。
	 * @param _spline 曲線
	 * @param _insertedSpline 節点挿入済み曲線
	 * @param _start 開始インデックス
	 * @param _end 終了インデックス
	 * @throws 曲線の節点数が節点挿入済み曲線より多い場合
	 * @return 差分節点列
	 */
	private static double[] searchDifferenceKnots( SplineCurve _spline, SplineCurve _insertedSpline, int _start, int _end ) {
		int degree = _spline.degree();
		double[] knots = _spline.knots();
		double[] insertedKnots = _insertedSpline.knots();
		if ( knots.length > insertedKnots.length ) {
			throw new IllegalArgumentException(" spline's knots is more than inserted spline's knots ");
		}

		int i = _spline.searchKnotNum( insertedKnots[_start], degree - 1, knots.length - degree );
		i -= ( insertedKnots[_start] == knots[knots.length - degree] ) ? 0 : 1;

		int endIndex = _spline.searchKnotNum( insertedKnots[_end], degree - 1, knots.length - degree );
		while ( endIndex > i
			&& ( knots[endIndex - 1] == knots[endIndex]
			|| knots[endIndex - 1] >= insertedKnots[_end] ) ) {
			--endIndex;
		}

		double[] result = new double[ ( _end - _start ) - ( endIndex - i )
			+ ( knots[i] < insertedKnots[_start] ? 1 : 0 )
			+ ( knots[endIndex] > insertedKnots[_end] ? 1 : 0 ) ];
		for ( int j = _start, k = 0; j <= _end; ++j ) {
			if ( insertedKnots[j] != knots[i] ) {
				result[k] = insertedKnots[j];
				++k;
				if ( insertedKnots[j] > knots[i] ) {
					++i;
				}
			} else {
				++i;
			}
		}

		return result;
	}

	/**
	 * 節点区間を拡大します。
	 * @param _existed 既存曲線
	 * @param _i 既存曲線の区間番号
	 * @param _overlapped 重ね書き曲線の区間番号
	 * @param _j 重ね書き曲線の区間番号
	 * @param _expandedFlag 拡大済みフラグ
	 * @param _existedInsertedFlag 既存曲線の挿入済みフラグ
	 * @param _overlappedInsertedFlag 重ね書き曲線の挿入済みフラグ
	 * @param _existedFixedFlag 既存の節点列に変更フラグ
	 * @param _overlappedFixedFlag 重ね書き曲線の変更フラグ
	 * @throws パターンが不正になる場合
	 * @return 区間拡大済み曲線ペア
	 */
	private static SplineCurve[] expandPeriod(
		SplineCurve _existed, int _i, SplineCurve _overlapped, int _j,
		boolean _expandedFlag, boolean _existedInsertedFlag, boolean _overlappedInsertedFlag,
		boolean _existedFixedFlag, boolean _overlappedFixedFlag ) {
		
		SplineCurve[] existedCandidate;
		SplineCurve[] overlappedCandidate;
		if ( ( _existedFixedFlag && _overlappedFixedFlag )
			|| ( !_expandedFlag && _existedInsertedFlag && _overlappedInsertedFlag )
			|| ( !_expandedFlag && _existedInsertedFlag && _existedFixedFlag )
			|| ( !_expandedFlag && _overlappedInsertedFlag && _overlappedFixedFlag ) ) {
			throw new IllegalArgumentException("pattern is incorrect");
		} else if ( _existedFixedFlag ) {
			existedCandidate = new SplineCurve[]{ _existed };
			overlappedCandidate = new SplineCurve[]{ _overlapped.deleteKnot( _j ) };
		} else if ( _overlappedFixedFlag ) {
			existedCandidate = new SplineCurve[]{ _existed.deleteKnot( _i ) };
			overlappedCandidate = new SplineCurve[]{ _overlapped };
		} else if ( _expandedFlag || ( !_existedInsertedFlag && !_overlappedInsertedFlag ) ) {
			SplineCurve expandedExisted = _existed.deleteKnot( _i );
			SplineCurve expandedOverlapped = _overlapped.deleteKnot( _j );
			existedCandidate = new SplineCurve[]{ expandedExisted, _existed, expandedExisted };
			overlappedCandidate = new SplineCurve[]{ expandedOverlapped, expandedOverlapped, _overlapped };
		} else if ( !_expandedFlag && _existedInsertedFlag ) {
			SplineCurve expandedExisted = _existed.deleteKnot( _i );
			existedCandidate = new SplineCurve[]{ expandedExisted, expandedExisted };
			overlappedCandidate = new SplineCurve[]{ _overlapped.deleteKnot( _j ), _overlapped };
		} else /*( !_expandedFlag && _overlappedFlag )*/ {
			SplineCurve expandedOverlapped = _overlapped.deleteKnot( _j );
			existedCandidate = new SplineCurve[]{ _existed.deleteKnot( _i ), _existed };
			overlappedCandidate = new SplineCurve[]{ expandedOverlapped, expandedOverlapped };
		}

		return selectMostOverlapping( existedCandidate, _i, overlappedCandidate, _j );
	}

	/**
	 * 指定された候補の中から重複可能性値の最も高い候補を選択します。
	 * @param _existedCandidate 既存曲線の候補群
	 * @param _thisIndex 既存曲線の区間インデックス
	 * @param _overlappedCandidate 重ね書き曲線の候補群
	 * @param _otherIndex 重ね書き曲線の区間インデックス
	 * @return 重複度の高い候補
	 */
	private static SplineCurve[] selectMostOverlapping( SplineCurve[] _existedCandidate, int _thisIndex, SplineCurve[] _overlappedCandidate, int _otherIndex ) {
		double[] temporalPossibilities = new double[ _existedCandidate.length ];
		double[] figuralPossibilities = new double[ _existedCandidate.length ];
		// 各候補の重複可能性値を求める
		for ( int i = 0; i < _existedCandidate.length; ++i ) {
			temporalPossibilities[i] = _existedCandidate[i].overlappedWith( _thisIndex, _overlappedCandidate[i], _otherIndex ).possibility();
			figuralPossibilities[i] = _existedCandidate[i].overlappedWith( _thisIndex + 1, _overlappedCandidate[i], _otherIndex + 1 ).possibility();
		}

		int index = 0;
		double maxPos = Math.min( temporalPossibilities[index], figuralPossibilities[index] );
		for ( int i = 1; i < temporalPossibilities.length; ++i ) {
			double pos = Math.min( temporalPossibilities[i], figuralPossibilities[i] );
			if ( pos > maxPos ) {
				maxPos = pos;
				index = i;
			}
		}

		return new SplineCurve[]{ _existedCandidate[index], _overlappedCandidate[index] };
	}

	/**
	 * 区間の運動的重複度の可能性値の最も高い候補を選択します。
	 * @param _existedCandidate 既存曲線の候補群
	 * @param _thisIndex 既存曲線の区間インデックス
	 * @param _overlappedCandidate 重ね書き曲線の候補群
	 * @param _otherIndex 重ね書き曲線の区間インデックス
	 * @return 運動的重複度の高い候補
	 * @throws IllegalArgumentException 既存曲線の候補群が重ね書き曲線の候補群の数が異なる場合
	 */
	private static SplineCurve[] selectMostKineticOverlapping( SplineCurve[] _existedCandidate, int _thisIndex, SplineCurve[] _overlappedCandidate, int _otherIndex ) {
		if ( _existedCandidate.length != _overlappedCandidate.length ) {
			throw new IllegalArgumentException(" _existedCandidate's length not equal _overlappedCandidate's length ");
		}
		double[] kineticPossibilities = new double[ _existedCandidate.length ];
		// 各候補の重複可能性値を求める
		for ( int i = 0; i < _existedCandidate.length; ++i ) {
			kineticPossibilities[i] = _existedCandidate[i].overlappedWith( _thisIndex, _overlappedCandidate[i], _otherIndex ).possibility();
		}

		int index = 0;
		double maxPos = kineticPossibilities[index];
		for ( int i = 1; i < kineticPossibilities.length; ++i ) {
			double pos = kineticPossibilities[i];
			if ( pos > maxPos ) {
				maxPos = pos;
				index = i;
			}
		}

		return new SplineCurve[]{ _existedCandidate[index], _overlappedCandidate[index] };
	}

	/**
	 * 区間の形状的重複度の高い曲線ペアを選択します。
	 * @param _existed 既存曲線
	 * @param _existedIndex 既存曲線の区間インデックス
	 * @param _overlapped 重ね書き曲線
	 * @param _overlappedIndex 重ね書き曲線の区間インデックス
	 * @return 形状的重複度の高い曲線ペア
	 */
	private static SplineCurve[] selectMostFiguralOverlapping( SplineCurve _existed, int _existedIndex, SplineCurve _overlapped, int _overlappedIndex ) {
		TruthValue[] figuralTV = new TruthValue[]{
			_existed.overlappedWith( _existedIndex + 2, _overlapped, _overlappedIndex + 2 ),
			_existed.overlappedWith( _existedIndex + 1, _overlapped, _overlappedIndex + 2 ),
			_existed.overlappedWith( _existedIndex + 2, _overlapped, _overlappedIndex + 1 )
		};

		int index = 0;
		double maxPos = figuralTV[index].possibility();
		for ( int i = 1; i < figuralTV.length; ++i ) {
			double pos = figuralTV[i].possibility();
			if ( pos > maxPos ) {
				maxPos = pos;
				index = i;
			}
		}

		SplineCurve[] result = null;
		switch ( index ) {
			case 0:
				result = new SplineCurve[]{ _existed, _overlapped };
				break;
			case 1:
				result = new SplineCurve[]{ _existed.insertKnot( _existedIndex ), _overlapped };
				break;
			case 2:
				result = new SplineCurve[]{ _existed, _overlapped.insertKnot( _overlappedIndex ) };
				break;
		}

		return result;
	}

	/**
	 * 曲線を細分化します。
	 * @param _spline 曲線
	 * @param _threshold 閾値
	 * @return 細分化済み曲線
	 */
	private static SplineCurve segmentalize( SplineCurve _spline, double _threshold ) {
		Range range = _spline.range();
		SplineCurve spline = insertMultipleKnotsAtStartAndEnd( _spline, range );
		int degree = spline.degree();
		double[] knots = spline.knots();
		int i = spline.searchKnotNum( range.start(), degree - 1, knots.length - degree );
		Point[] cp = spline.controlPoints();
		List<Range> knotRanges = new ArrayList<Range>();
		// 定義域中の各節点区間における制御点列の重複を見て，点に縮退していない区間を探索する
		while ( knots[i - 1] < range.end() ) {
			double possibility = 1;
			for ( int j = 0; j <= degree; ++j ) {
				for ( int k = j + 1; k <= degree; ++k ) {
					TruthValue tv = cp[i - degree + j].includedIn( cp[i - degree + k] );
					possibility = Math.min( tv.possibility(), possibility );
				}
			}
			if ( possibility <= _threshold ) {
				knotRanges.add( Range.create( knots[i - 1], knots[i] ) );
			}
			++i;
		}

		while ( !knotRanges.isEmpty() ) {
			// 点に縮退していない節点区間に対して，節点挿入を行って区間を細分化する
			List<Range> ranges = new ArrayList<Range>();
			for ( Range knotRange : knotRanges ) {
				double knot = ( knotRange.start() + knotRange.end() ) / 2.0;
				// 計算機精度の限界で区間の中間地点を計算できない場合は細分化が完了したものとする
				if ( knotRange.start() < knot && knot < knotRange.end() ) {
					spline = spline.insertKnot( knot );
					ranges.add( Range.create( knotRange.start(), knot ) );
					ranges.add( Range.create( knot, knotRange.end() ) );
				}
			}
			// 再度，点に縮退していない区間を探索する
			knots = spline.knots();
			cp = spline.controlPoints();
			knotRanges.clear();
			for ( Range knotRange : ranges ) {
				i = spline.searchKnotNum( knotRange.start(), degree - 1, knots.length - degree );
				double possibility = 1;
				for ( int j = 0; j <= degree; ++j ) {
					for ( int k = j + 1; k <= degree; ++k ) {
						TruthValue tv = cp[i - degree + j].includedIn( cp[i - degree + k] );
						possibility = Math.min( tv.possibility(), possibility );
					}
				}
				if ( possibility <= _threshold ) {
					knotRanges.add( Range.create(knots[i - 1], knots[i] ) );
				}
			}
		}

		return spline;
	}

	/**
	 * 指定された範囲の両端で多重節点挿入します。
	 * @param _spline スプライン曲線
	 * @param _range 指定する範囲
	 * @return 節点挿入済み曲線
	 */
	private static SplineCurve insertMultipleKnotsAtStartAndEnd( SplineCurve _spline, Range _range ) {
		SplineCurve result = _spline;
		int degree = _spline.degree();
		double start = _range.start();
		double end = _range.end();
		for ( int i = 0; i < degree; ++i ) {
			result = result.insertKnot( start );
			result = result.insertKnot( end );
		}
		return result;
	}
	
	private OverlappingRangeFinder(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}