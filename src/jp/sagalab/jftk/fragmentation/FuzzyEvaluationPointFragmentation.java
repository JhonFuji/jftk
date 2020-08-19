package jp.sagalab.jftk.fragmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.ParametricEvaluable.EvaluationType;

/**
 * ファジィスプライン曲線の評価点列を用いてファジィフラグメンテーション法を行います。
 * <p>
 * ファジィスプライン曲線を評価点列により離散化して、
 * 評価点列のチャンク毎に状態を判別します。
 * </p>
 * @see <span>博士学位論文 「幾何曲線に基づく汎用的手書き作図インタフェースの実現」 西川 玲</span>
 * @author aburaya
 */
public class FuzzyEvaluationPointFragmentation implements FuzzyFragmentation{
	
	/**
	 * このクラスのインスタンスを生成します。
	 * @param _divideThreshold 閾値
	 * @param _divideTimeResolution 時間解像度
	 * @param _divideChunkLength チャンク時間長
	 * @return フラグメント
	 * @throws IllegalArgumentException 閾値がnullの場合
	 * @throws IllegalArgumentException 時間解像度がNaNの場合
	 * @throws IllegalArgumentException 時間解像度がInfの場合
	 * @throws IllegalArgumentException チャンク時間長がNaNの場合
	 * @throws IllegalArgumentException チャンク時間長がInfの場合
	 */	
	public static FuzzyEvaluationPointFragmentation create( TruthValue _divideThreshold, double _divideTimeResolution, double _divideChunkLength ){
		if( _divideThreshold == null ){
			throw new IllegalArgumentException("_divideThreshold  is null");
		}
		if( Double.isNaN( _divideTimeResolution ) ){
			throw new IllegalArgumentException("_divideTimeResolution is NaN");
		}
		if( Double.isInfinite(_divideTimeResolution) ){
			throw new IllegalArgumentException("_divideTimeResolution is Infinite");
		}
		if ( _divideTimeResolution <= 0 ) {
			throw new IllegalArgumentException("_divideTimeResolution is no more than 0");
		}
		if( Double.isNaN( _divideChunkLength ) ){
			throw new IllegalArgumentException("_divideChunkLength is NaN");
		}
		if( Double.isInfinite( _divideChunkLength )  ){
			throw new IllegalArgumentException("_divideChunkLength is Infinite");
		}
		return new FuzzyEvaluationPointFragmentation( _divideThreshold, _divideTimeResolution, _divideChunkLength );
	}
		
	/**
	 * 状態範囲列の抽出を行います。
	 * <p>
	 * 曲線全体をチャンクサイズ単位に部分評価点列化し、チャンクの停止性判定によって
	 * 「移動」「停止」「不明」の三つの状態に分類していきます。<br>
	 * 最終的に「移動状態」「停止状態」「不明状態」の三つの状態の部分範囲列が得られます。<br>
	 * なお、閾値は停止性判定の際の分類に用いられ、チャンクサイズは停止判定時間（停止とみなす最小の時間）の意味を持ちます。<br>
	 * また、チャンクの評価密度や連続するチャンクの時間間隔は時間分解能を元に決められます。<br>
	 * ただし、時間分解能はそのままの値ではなく、あくまで基準であることに注意してください。<br>
	 * （例）チャンクサイズが0.1で時間分解能が0.03であれば、チャンクの評価点数はMath.ceil( 0.1 / 0.03 )で４点となります。
	 * </p>
	 * @param _curve 曲線
	 * @param _threshold 閾値
	 * @param _chunkSize チャンクサイズ（停止判定時間）
	 * @param _timeResolution 時間分解能
	 * @param _target 抽出対象とする状態
	 * @return 状態範囲列
	 * @throws IllegalArgumentException 抽出対象の状態がnullの場合
	 * @throws IllegalArgumentException チャンクサイズがinfの場合
	 * @throws IllegalArgumentException 時間分解能が0以下の場合
	 */
	public static Range[] extract( ParametricCurve _curve, TruthValue _threshold, double _chunkSize, double _timeResolution, State _target ) {
		if ( _target == null ) {
			throw new IllegalArgumentException("_target is null");
		}
		if ( Double.isInfinite( _chunkSize ) ) {
			throw new IllegalArgumentException("_chunkSize is inf");
		}
		if ( _timeResolution <= 0 ) {
			throw new IllegalArgumentException("_timeResolution is no more than 0");
		}

		// 範囲列
		List<Range> ranges = new ArrayList<Range>();

		// 評価点列化
		int evaluateNum = Math.max( 2, (int) Math.ceil( _curve.range().length() / _timeResolution ) );
		Point[] points = _curve.evaluateAll( evaluateNum, EvaluationType.TIME );

		// チャンクの構成点数
		int n = (int) Math.ceil( _chunkSize / _timeResolution );

		// 初期状態は停止
		State globalState = state( Arrays.copyOfRange( points, 0, Math.min( n, points.length ) ), _threshold );
		globalState = globalState == State.UNKNOWN ? State.STAY : globalState;

		// 状態開始時刻
		double stateStart = points[0].time();

		// 不明状態か
		boolean isUnknownState = false;

		for ( int i = 0; i < points.length - n + 1; ++i ) {
			// チャンクの状態チェック
			Point[] chunk = Arrays.copyOfRange( points, i, i + n );
			State chunkState = state( chunk, _threshold );

			// 状態遷移の検出
			if ( chunkState != State.UNKNOWN ) {
				if ( _target == State.UNKNOWN ) {
					if ( isUnknownState ) {
						ranges.add( Range.create( stateStart, points[i].time() ) );
					}
					isUnknownState = false;
				} else if ( globalState != chunkState ) { // _targetがunknownでない，かつ状態が遷移した場合
					if ( globalState == _target ) { // 遷移するまでの区間が_targetだった場合，その区間を格納
						ranges.add( Range.create( stateStart, points[i].time() ) );
					} else {
						// 状態開始時刻を更新
						stateStart = points[i].time();
					}
					// 状態を更新
					globalState = chunkState;
				}
			} else if ( _target == State.UNKNOWN ) { // チャンクの状態がunknownでそれが_targetの場合
				if ( !isUnknownState ) {
					// 状態開始時刻を更新
					stateStart = points[i].time();
					isUnknownState = true;
				}
			}
		}
		// isUnknownStateは ( _target == State.UNKNOWN ) が真の時だけ true になり得る
		if ( globalState == _target || isUnknownState ) {
			ranges.add( Range.create( stateStart, points[points.length - 1].time() ) );
		}

		return ranges.toArray( new Range[ ranges.size() ] );
	}
	
	@Override
	public Fragment[] createFragment( SplineCurve _splineCurve ) {
		// フラグメント列
		List<Fragment> fragments = new ArrayList<Fragment>();

		// 評価点列化
		int evaluateNum = Math.max( 2, (int) Math.ceil( _splineCurve.range().length() / m_divideTimeResolution ) );
		Point[] points = _splineCurve.evaluateAll( evaluateNum, EvaluationType.TIME );

		// チャンクの構成点数
		int n = (int) Math.ceil( m_divideChunkLength / m_divideTimeResolution );

		// 初期状態は停止
		State globalState = state( Arrays.copyOfRange( points, 0, Math.min( n, points.length ) ), m_divideThreshold );
		// 初期状態が不明の場合、停止とする。
		globalState = globalState == State.UNKNOWN ? State.STAY : globalState;

		// 停止点の始点側の接続点
		Point stayStart = null;

		// 状態開始時刻
		double stateStart = points[0].time();

		for ( int i = 0; i < points.length - n + 1; ++i ) {
			// チャンクの状態チェック
			Point[] chunk = Arrays.copyOfRange( points, i, i + n );
			State chunkState = state( chunk, m_divideThreshold );

			// 状態遷移の検出
			if ( chunkState != State.UNKNOWN ) {
				if ( globalState != chunkState ) { // 状態が遷移した場合
					SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[i].time() ) );
					if ( globalState == State.MOVE ) {
						// 移動状態を同定フラグメントとする
						fragments.add( IdentificationFragment.create( curve ) );
						stayStart = points[i];
					} else if ( globalState == State.STAY ) {
						// 停止状態を区切りフラグメントとする
						fragments.add( PartitionFragment.create( curve, stayStart, points[i] ) );
					}
					// 状態開始時刻を更新
					stateStart = points[i].time();
					// 状態を更新
					globalState = chunkState;
				}
			}
		}
		SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[points.length - 1].time() ) );
		if ( globalState == State.MOVE ) {
			// 移動状態を同定フラグメントとする
			fragments.add( IdentificationFragment.create( curve ) );
		} else if ( globalState == State.STAY ) {
			// 停止状態を区切りフラグメントとする
			fragments.add( PartitionFragment.create( curve, stayStart, null ) );
		}
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
		Range[] ranges = extract( _splineCurve, m_divideThreshold, m_divideChunkLength, m_divideTimeResolution, State.MOVE );
		SplineCurve[] fragments = new SplineCurve[ ranges.length ];

		for ( int i = 0; i < ranges.length; ++i ) {
			fragments[i] = _splineCurve.part( ranges[i] );
		}

		return fragments;
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
	
	private FuzzyEvaluationPointFragmentation( TruthValue _divideThreshold, double _divideTimeResolution, double _divideChunkLength ){		
		m_divideThreshold = _divideThreshold;
		m_divideTimeResolution = _divideTimeResolution;
		m_divideChunkLength = _divideChunkLength;
	}
	
	/** ファジィフラグメンテーションの閾値。 */
	private final TruthValue m_divideThreshold;
	/** ファジィフラグメンテーションの時間解像度。 */
	private final double m_divideTimeResolution;
	/** ファジィフラグメンテーションのチャンク時間長。 */
	private final double m_divideChunkLength;
}