package jp.sagalab.jftk.fragmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.TruthValue;

/**
 * ファジィスプライン曲線の制御点列を用いてファジィフラグメンテーションを行います。
 * <p>
 * 制御点を用いてファジィスプライン曲線の連続的な動きを見て
 * 評価する。動きの範囲は1節点区間あたりの時間解像度によって変化する。
 * 細かくしすぎると重なりが強くなり、停止が出やすくなる。
 * TODO 16期 金子氏が作成したが、論文未作成のため詳細不明、評価実験も行っていない。
 * </p>
 * @author aburaya
 */
public class FuzzyControlPointFragmentation implements FuzzyFragmentation{
	
	/**
	 * このクラスのインスタンスを生成します。
	 * @param _divideThreshold ファジィフラグメンテーションの閾値
	 * @param _divideKnotSpanResolution ファジィフラグメンテーションの1節点区間あたりの時間解像度
	 * @param _gestureThreshold 書描弁別におけるファジィフラグメンテーションの閾値
	 * @return インスタンス
	 * @throws IllegalArgumentException フラグメンテーションの閾値がnullの場合
	 * @throws IllegalArgumentException 書描弁別の閾値がnullの場合
	 */
	public static FuzzyControlPointFragmentation create(TruthValue _divideThreshold, int _divideKnotSpanResolution, TruthValue _gestureThreshold){
		if( _divideThreshold == null ){
			throw new IllegalArgumentException("_dividedThreshold is null");
		}
		if( _gestureThreshold == null ){
			throw new IllegalArgumentException("_gestureThreshold is null");
		}
		return new FuzzyControlPointFragmentation( _divideThreshold, _divideKnotSpanResolution, _gestureThreshold);
	}
	
	@Override
	public Fragment[] createFragment( SplineCurve _splineCurve ) {
		// フラグメント列
		List<Fragment> fragments = new ArrayList<Fragment>();

		Range range = _splineCurve.range();
		SplineCurve spline = segmentalize( _splineCurve, m_divideKnotSpanResolution );
		int degree = spline.degree();
		double[] knots = spline.knots();
		Point[] cp = spline.controlPoints();
		// 状態開始時刻
		int i = spline.searchKnotNum( range.start(), degree - 1, knots.length - degree );
		double stateStart = Math.max( range.start(), knots[i - 1] );

		State globalState = state( Arrays.copyOfRange( cp, i - degree, i + 1 ), m_divideThreshold );
		globalState = globalState == State.UNKNOWN ? State.STAY : globalState;
		while ( knots[i - 1] < range.end() ) {
			// チャンクの状態チェック
			Point[] chunk = Arrays.copyOfRange( cp, i - degree, i + 1 );
			State chunkState = state( chunk, m_divideThreshold );

			// 状態遷移の検出
			if ( chunkState != State.UNKNOWN && globalState != chunkState ) { // 状態が遷移した場合
				SplineCurve splinePart = _splineCurve.part( Range.create( stateStart, knots[i - 1] ) );
				Fragment fragment;
				if ( globalState == State.MOVE ) {
					fragment = IdentificationFragment.create( splinePart );
				} else {
					fragment = PartitionFragment.create( splinePart, splinePart.evaluateAtStart(), splinePart.evaluateAtEnd() );
				}
				fragments.add( fragment );
				// 状態開始時刻を更新
				stateStart = knots[i - 1];
				// 状態を更新
				globalState = chunkState;
			}
			++i;
		}
		SplineCurve splinePart = _splineCurve.part( Range.create( stateStart, Math.min( range.end(), knots[i - 1] ) ) );
		Fragment fragment;
		if ( globalState == State.MOVE ) {
			fragment = IdentificationFragment.create( splinePart );
		} else {
			fragment = PartitionFragment.create( splinePart, splinePart.evaluateAtStart(), splinePart.evaluateAtEnd() );
		}
		fragments.add( fragment );
		// フラグメント列の先頭の接続の足を更新
		Fragment headFragment = fragments.get( 0 );
		if ( headFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) headFragment;
			if ( !partition.isHead() ) {
				fragments.set( 0, PartitionFragment.create( partition, null, partition.end() ) );
			}
		}
		// フラグメント列の末尾の接続の足を更新
		Fragment tailFragment = fragments.get( fragments.size() - 1 );
		if ( tailFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) tailFragment;
			if ( !partition.isTail() ) {
				fragments.set( fragments.size() - 1,
					PartitionFragment.create( partition, partition.start(), null ) );
			}
		}
		return fragments.toArray( new Fragment[ fragments.size() ] );
	}
	
	@Override
	public SplineCurve[] divide( SplineCurve _splineCurve ) {
		Range[] ranges = extract( _splineCurve, m_gestureThreshold, m_divideKnotSpanResolution, State.MOVE );
		SplineCurve[] fragments = new SplineCurve[ ranges.length ];

		for ( int i = 0; i < ranges.length; ++i ) {
			fragments[i] = _splineCurve.part( ranges[i] );
		}
		return fragments;
	}
	
	/**
	 * 状態範囲列の抽出を行います。
	 * <p>
	 * 曲線の節点区間を節点挿入によってチャンクサイズ単位に細分化し、チャンクの停止性判定によって
	 * 「移動」「停止」「不明」の三つの状態に分類していきます。<br>
	 * 最終的に「移動状態」「停止状態」「不明状態」の三つの状態の部分範囲列が得られます。<br>
	 * なお、閾値は停止性判定の際の分類に用いられます。
	 * </p>
	 * @param _spline 曲線
	 * @param _threshold 閾値
	 * @param _resolution 1区間における時間解像度
	 * @param _target 抽出対象とする状態
	 * @return 状態範囲列
	 */
	private static Range[] extract( SplineCurve _spline, TruthValue _threshold, int _resolution, State _target ) {
		// 範囲列
		List<Range> ranges = new ArrayList<Range>();

		SplineCurve spline = segmentalize( _spline, _resolution );
		int degree = spline.degree();
		Point[] cp = spline.controlPoints();
		double[] knots = spline.knots();
		Range range = spline.range();

		// 状態開始時刻
		int i = spline.searchKnotNum( range.start(), degree - 1, knots.length - degree );
		double stateStart = Math.max( range.start(), knots[i - 1] );
		// 初期状態
		State globalState = state( Arrays.copyOfRange( cp, i - degree, i + 1 ), _threshold );
		globalState = ( globalState == State.UNKNOWN ) ? State.STAY : globalState;

		// 不明状態か
		boolean isUnknownState = false;

		while ( knots[i - 1] < range.end() ) {
			// チャンクの状態チェック
			Point[] chunk = Arrays.copyOfRange( cp, i - degree, i + 1 );
			State chunkState = state( chunk, _threshold );

			// 状態遷移の検出
			if ( chunkState != State.UNKNOWN ) {
				if ( _target == State.UNKNOWN ) {
					if ( isUnknownState ) {
						ranges.add( Range.create( stateStart, knots[i - 1] ) );
					}
					isUnknownState = false;
				} else if ( globalState != chunkState ) { // _targetがunknownでない，かつ状態が遷移した場合
					if ( globalState == _target ) { // 遷移するまでの区間が_targetだった場合，その区間を格納
						ranges.add( Range.create( stateStart, knots[i - 1] ) );
					} else {
						// 状態開始時刻を更新
						stateStart = knots[i - 1];
					}
					// 状態を更新
					globalState = chunkState;
				}
			} else if ( _target == State.UNKNOWN ) { // チャンクの状態がunknownでそれが_targetの場合
				if ( !isUnknownState ) {
					// 状態開始時刻を更新
					stateStart = knots[i - 1];
					isUnknownState = true;
				}
			}
			++i;
		}
		// isUnknownStateは ( _target == State.UNKNOWN ) が真の時だけ true になり得る
		if ( globalState == _target || isUnknownState ) {
			ranges.add( Range.create( stateStart, Math.min( range.end(), knots[i - 1] ) ) );
		}

		return ranges.toArray( new Range[ ranges.size() ] );
	}

	/**
	 * 曲線を細分化します。
	 * @param _spline 曲線
	 * @param _resolution 1区間における時間解像度
	 * @return 細分化した曲線
	 * @throws IllegalArgumentException 時間解像度が自然数じゃない場合
	 */
	private static SplineCurve segmentalize( SplineCurve _spline, int _resolution ) {
		if ( _resolution < 1 ) {
			throw new IllegalArgumentException( "_resolution < 1" );
		}
		int degree = _spline.degree();
		SplineCurve spline = _spline;
		// 曲線の節点区間を細分化
		double[] knots = _spline.knots();
		for ( int i = degree + 1; i < knots.length - degree + 1; ++i ) {
			for ( int j = 1; j < _resolution; ++j ) {
				double t = j / (double) _resolution;
				double knot = ( 1.0 - t ) * knots[i - 1] + t * knots[i];
				spline = spline.insertKnot( knot );
			}
		}

		return spline;
	}
	
	/**
	 * チャンクの状態を返します。
	 * @param _chunk チャンク
	 * @param _threshold 閾値
	 * @return チャンクの状態
	 */
	private static State state( Point[] _chunk, TruthValue _threshold ) {
		double nec = 1;
		double pos = 1;
		Point last = _chunk[ _chunk.length - 1];
			for ( int i = 0; i < _chunk.length - 1; ++i ) {
			TruthValue tv = last.includedIn( _chunk[ i] );
					nec = Math.min( nec, tv.necessity() );
					pos = Math.min( pos, tv.possibility() );
				}
		State state = State.UNKNOWN;
		if ( nec < _threshold.necessity() && pos < _threshold.possibility() ) {
			state = State.MOVE;
		} else if ( _threshold.necessity() < nec && _threshold.possibility() < pos ) {
			state = State.STAY;
		}
		return state;
	}
	
	private FuzzyControlPointFragmentation(
		TruthValue _divideThreshold, int _divideKnotSpanResolution, TruthValue _gestureThreshold){
			
		m_divideThreshold = _divideThreshold;
		m_divideKnotSpanResolution = _divideKnotSpanResolution;
		m_gestureThreshold = _gestureThreshold;
	}
	
	/** ファジィフラグメンテーションの閾値。 */
	private final TruthValue m_divideThreshold;
	/** ファジィフラグメンテーションの1節点区間あたりの時間解像度 */
	private final int m_divideKnotSpanResolution;
	/** 書描弁別におけるファジィフラグメンテーションの閾値 */
	private final TruthValue m_gestureThreshold;
}