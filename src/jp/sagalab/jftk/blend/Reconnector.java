package jp.sagalab.jftk.blend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.fragmentation.Fragment;
import jp.sagalab.jftk.fragmentation.IdentificationFragment;
import jp.sagalab.jftk.fragmentation.PartitionFragment;
import jp.sagalab.jftk.shaper.snapper.GridSpace;

/**
 * 再接続を行うクラスです。
 * @author yamaguchi
 */
public class Reconnector {
	
	/**
	 * インスタンス生成。
	 * @param _lowThreshold 再接続の閾値
	 * @param _highThreshold 再接続の閾値
	 * @return 再接続を行うクラス
	 * @throws IllegalArgumentException 閾値がNaNもしくは、Infの場合
	 */
	public static Reconnector create( double _lowThreshold, double _highThreshold ) {
		if ( Double.isNaN( _lowThreshold ) || Double.isInfinite( _lowThreshold ) ) {
			throw new IllegalArgumentException( "_lowThreshold is NaN or Inf" );
		}
		if ( Double.isNaN( _highThreshold ) || Double.isInfinite( _highThreshold ) ) {
			throw new IllegalArgumentException( "_highThreshold is NaN or Inf" );
		}
		return new Reconnector( _lowThreshold, _highThreshold );
	}

	/**
	 * 再接続
	 * @param _reconnectedList 再接続リスト
	 * @param _added 加わるフラグメント列
	 * @param _gridSpace グリッド空間
	 * @return 再接続後のフラグメント列
	 */
	public List<Fragment> reconnect( List<Fragment[]> _reconnectedList, Fragment[] _added, GridSpace _gridSpace ) {
		// 接続先を決定
		Fragment[][] targets = selectReconnectingTargets( _reconnectedList, _added );
		List<Fragment> addedFragments = Arrays.asList( _added );
		Fragment addedHead = _added[0];
		Fragment addedTail = _added[_added.length - 1];
		// 接続済みリスト
		List<Fragment> connectedFragments = new ArrayList<Fragment>();
		// 追加図形の始点側を接続
		if ( targets[0] != null ) {
			// 接続先フラグメント列を取得
			List<Fragment> targetFragments = Arrays.asList( targets[0] );
			// 接続点となる区切り点を取得
			PartitionFragment partitionFragment = (PartitionFragment) targetFragments.get( targetFragments.size() - 1 );
			if ( addedHead.getClass() == IdentificationFragment.class ) {
				// 始点側の再接続先のフラグメント列をリストに追加
				connectedFragments.addAll( targetFragments );

				// 追加図形の始点を取得
				Point p = ( (IdentificationFragment) addedHead ).curve().evaluateAtStart();
				// 接続点となる区切り点の足を更新
				connectedFragments.set( connectedFragments.size() - 1,
					PartitionFragment.create( partitionFragment, partitionFragment.start(), p ) );
			} else if ( addedHead.getClass() == PartitionFragment.class ) {
				int num = targetFragments.size() - 1;
				// 始点側の接続先のフラグメント列をリストに追加
				connectedFragments.addAll( targetFragments.subList( 0, num ) );
				// 区切りフラグメント同士の融合
				PartitionFragment blended = partitionFragment.blend( (PartitionFragment) addedHead, 1, 1 );
				// 接続点となる区切り点の注目点と足を更新
				addedFragments = new ArrayList<Fragment>( addedFragments );
				addedFragments.set( 0, blended );
			}
		}
		// 追加図形のフラグメント列をリストに追加
		connectedFragments.addAll( addedFragments );
		// 追加図形の終点側を接続
		if ( targets[1] != null ) {
			// 接続先フラグメント列を取得
			List<Fragment> targetFragments = Arrays.asList( targets[1] );
			// 接続点となる区切り点を取得
			PartitionFragment partitionFragment = (PartitionFragment) targetFragments.get( 0 );
			if ( addedTail.getClass() == IdentificationFragment.class ) {
				// 追加図形の終点を取得
				Point p = ( (IdentificationFragment) addedTail ).curve().evaluateAtEnd();
				// 接続点となる区切り点の足を更新
				targetFragments = new ArrayList<Fragment>( targetFragments );
				targetFragments.set( 0, PartitionFragment.create( partitionFragment, p, partitionFragment.end() ) );
				// 終点側の接続先のフラグメント列をリストに追加
				connectedFragments.addAll( targetFragments );
			} else if ( addedTail.getClass() == PartitionFragment.class ) {
				// 区切りフラグメント同士の融合
				PartitionFragment blended = ( (PartitionFragment) addedTail ).blend( partitionFragment, 1, 1 );
				// 接続点となる区切り点の曲線と足を更新
				connectedFragments.set( connectedFragments.size() - 1, blended );
				int num = targetFragments.size();
				// 終点側の接続先のフラグメント列をリストに追加
				connectedFragments.addAll( targetFragments.subList( 1, num ) );
				// 終点側の接続先のプリミティブ列とスキルデータをリストに追加
			}
		}
		// 再接続後したフラグメント列の先頭の接続の足を更新
		Fragment headFragment = connectedFragments.get( 0 );
		if ( headFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) headFragment;
			if ( partition.start() != null ) {
				connectedFragments.set( 0, PartitionFragment.create( partition, null, partition.end() ) );
			}
		}
		// 再接続したフラグメント列の末尾の接続の足を更新
		Fragment tailFragment = connectedFragments.get( connectedFragments.size() - 1 );
		if ( tailFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) tailFragment;
			if ( partition.end() != null ) {
				connectedFragments.set( connectedFragments.size() - 1,
					PartitionFragment.create( partition, partition.start(), null ) );
			}
		}
		
		return connectedFragments;
	}

	/**
	 * 再接続候補を選出します。
	 * @param _reconnectedList 再接続された
	 * @param _added 新たに入力されたフラグメント列
	 * @return 再接続候補フラグメント列候補群
	 */
	public Fragment[][] selectReconnectingTargets( List<Fragment[]> _reconnectedList, Fragment[] _added ) {
		Fragment addedHead = _added[0];
		Fragment addedTail = _added[_added.length - 1];
		Fragment[][] targets = new Fragment[ 2 ][];
		double headMaxPos = Double.NEGATIVE_INFINITY;
		double tailMaxPos = Double.NEGATIVE_INFINITY;
		// 始点側の接続先を探索
		if ( addedHead.getClass() == IdentificationFragment.class ) {
			// 追加図形の先頭が同定フラグメントの場合，再接続リスト中の区切りフラグメントがもつ足との重なりを見る
			Point start = ( (IdentificationFragment) addedHead ).curve().evaluateAtStart();
			for ( Fragment[] fragments : _reconnectedList ) {
				int fragmentsSize = fragments.length;
				Fragment fragment = fragments[ fragmentsSize - 1 ];
				if ( fragment.getClass() == PartitionFragment.class ) {
					PartitionFragment partitionFragment = (PartitionFragment) fragment;
					Point end = partitionFragment.end();
					if ( end != null ) { // 区切りフラグメントが足を持っている場合
						TruthValue tv = start.includedIn( end );
						double threshold = m_lowThreshold;
						if ( fragmentsSize < 2 ) {
							threshold = m_highThreshold;
						}
						if ( tv.possibility() > threshold && tv.possibility() > headMaxPos ) {
							headMaxPos = tv.possibility();
							targets[0] = fragments;
						}
					}
				}
			}
		} else if ( addedHead.getClass() == PartitionFragment.class ) {
			// 追加図形の先頭が区切りフラグメントの場合，再接続リスト中の区切りフラグメントの注目点との重なりを見る
			Point body = ( (PartitionFragment) addedHead ).body();
			for ( Fragment[] fragments : _reconnectedList ) {
				int fragmentsSize = fragments.length;
				Fragment fragment = fragments[ fragmentsSize - 1 ];
				if ( fragment.getClass() == PartitionFragment.class ) {
					PartitionFragment partitionFragment = (PartitionFragment) fragment;
					TruthValue tv = body.includedIn( partitionFragment.body() );
					double threshold = m_lowThreshold;
					if ( fragmentsSize < 2 ) {
						threshold = m_highThreshold;
					}
					if ( tv.possibility() > threshold && tv.possibility() > headMaxPos ) {
						headMaxPos = tv.possibility();
						targets[0] = fragments;
					}
				}
			}
		}
		// 終点側の接続先を探索
		if ( addedTail.getClass() == IdentificationFragment.class ) {
			// 追加図形の末尾が同定フラグメントの場合，再接続リスト中の区切りフラグメントがもつ足との重なりを見る
			Point end = ( (IdentificationFragment) addedTail ).curve().evaluateAtEnd();
			for ( Fragment[] fragments : _reconnectedList ) {
				Fragment fragment = fragments[ 0 ];
				if ( fragment.getClass() == PartitionFragment.class ) {
					PartitionFragment partitionFragment = (PartitionFragment) fragment;
					Point start = partitionFragment.start();
					if ( start != null ) { // 区切りフラグメントが足を持っている場合
						TruthValue tv = end.includedIn( start );
						double threshold = m_lowThreshold;
						if ( fragments.length < 2 ) {
							threshold = m_highThreshold;
						}
						if ( tv.possibility() > threshold && tv.possibility() > tailMaxPos ) {
							tailMaxPos = tv.possibility();
							targets[1] = fragments;
						}
					}
				}
			}
		} else if ( addedTail.getClass() == PartitionFragment.class ) {
			// 追加図形の末尾が区切りフラグメントの場合，再接続リスト中の区切りフラグメントの注目点との重なりを見る
			Point body = ( (PartitionFragment) addedTail ).body();
			for ( Fragment[] fragments : _reconnectedList ) {
				Fragment fragment = fragments[ 0 ];
				if ( fragment.getClass() == PartitionFragment.class ) {
					PartitionFragment partitionFragment = (PartitionFragment) fragment;
					TruthValue tv = body.includedIn( partitionFragment.body() );
					double threshold = m_lowThreshold;
					if ( fragments.length < 2 ) {
						threshold = m_highThreshold;
					}
					if ( tv.possibility() > threshold && tv.possibility() > tailMaxPos ) {
						tailMaxPos = tv.possibility();
						targets[1] = fragments;
					}
				}
			}
		}

		// 接続先が同一の場合
		if ( targets[0] != null && targets[0] == targets[1] ) {
			// 可能性値の低い方を接続対象からはずす
			if ( headMaxPos < tailMaxPos ) {
				targets[0] = null;
			} else {
				targets[1] = null;
			}
		}

		return targets;
	}

	/**
	 * 残留図形群リストを生成します。
	 * @param _reconnectedList 再接続されたフラグメント列群
	 * @param _startSideTarget 探索開始フラグメント列
	 * @param _endSideTarget 探索終了フラグメント列
	 * @return 残留図形群リスト
	 */
	public List<Fragment[]> createRemainedFragmentsList( List<Fragment[]> _reconnectedList, Fragment[] _startSideTarget, Fragment[] _endSideTarget ) {
		List<Fragment[]> remained = new ArrayList<Fragment[]>();

		for ( Fragment[] fragments : _reconnectedList ) {
			if ( fragments != _startSideTarget && fragments != _endSideTarget ) {
				// 区切りフラグメントのみの図形は捨てる
				if ( fragments.length > 1
					|| fragments[ 0].getClass() == IdentificationFragment.class ) {
					Fragment headFragment = fragments[ 0];
					Fragment tailFragment = fragments[ fragments.length - 1];
					// 先頭または末尾のフラグメントが区切りフラグメントの場合，接続の足を更新
					if ( headFragment.getClass() == PartitionFragment.class
						|| tailFragment.getClass() == PartitionFragment.class ) {
//						fragments = new ArrayList<Fragment>( fragments );
						if ( headFragment.getClass() == PartitionFragment.class ) {
							PartitionFragment partitionFragment = (PartitionFragment) headFragment;
							fragments[ 0] = PartitionFragment.create( partitionFragment, null, partitionFragment.end() );
						}
						if ( tailFragment.getClass() == PartitionFragment.class ) {
							PartitionFragment partitionFragment = (PartitionFragment) tailFragment;
							fragments[ fragments.length - 1]
								= PartitionFragment.create( partitionFragment, partitionFragment.start(), null );
						}
					}
					// 残留図形群リストに追加
					remained.add( fragments );
				}
			}
		}

		return remained;
	}
		
	private Reconnector( double _lowThreshold, double _highThreshold ) {
		m_lowThreshold = _lowThreshold;
		m_highThreshold = _highThreshold;
	}
	
	/** 再接続候補探索に用いるしきい値 */
	private final double m_lowThreshold;
	/** 再接続候補探索に用いるしきい値 */
	private final double m_highThreshold;
}