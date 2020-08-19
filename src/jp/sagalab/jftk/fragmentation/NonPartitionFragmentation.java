package jp.sagalab.jftk.fragmentation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.recognition.PrimitiveType;
import jp.sagalab.jftk.recognition.RecognitionResult;
import jp.sagalab.jftk.recognition.Recognizable;

/**
 * 探索区間数を設定した楕円弧幾何曲線列化による最良分割点を用いてファジィフラグメンテーション法を行います。<br>
 * このアルゴリズムでは探索区間数を設定し，その区間毎に楕円弧幾何曲線列化を探索することで，
 * 処理速度の改善を図った楕円弧幾何曲線列化手法です．
 * @author aburaya
 * @see "修士論文「手書き自由曲線の楕円弧幾何曲線列に関する研究」4章 油谷 凜"
 */
public class NonPartitionFragmentation implements BestDivideParameterSearchAlgorithm,FuzzyFragmentation{

	/** グレード値と幾何曲線数と分割点パラメータ列からなるクラス */
	private class DividedCurveInformation{

		/**
		 * 分割諸量（分割後のグレード値、分割数、分割パラメータ列）を返す。
		 *
		 * @param _grade グレード値
		 * @param _curvesNum 幾何曲線数
		 * @param _parameters パラメータ列
		 * @param _isIncludeFreeCurve 自由曲線が含まれているかどうか
		 * @param _paramNums パラメータの数
		 */
		public DividedCurveInformation( double _grade, int _curveNum, Double[] _parameters, boolean _isIncludeFreeCurve, Integer[] _paramNums ) {
			this.grade = _grade;
			this.curveNum = _curveNum;
			if ( _parameters != null ) {
				this.parameters = new Double[_parameters.length];
				for ( int i = 0; i < _parameters.length; ++i ) {
					this.parameters[i] = _parameters[i];
				}
			} else {
				this.parameters = null;
			}
			this.isIncludeFreeCurve = _isIncludeFreeCurve;
			this.paramNums = _paramNums;
		}

		/**
		 * グレード値を返す。
		 *
		 * @return グレード値
		 */
		public double grade() {
			return grade;
		}

		/**
		 * 分割数を返す。
		 *
		 * @return 分割数
		 */
		public int dividedNums() {
			return curveNum;
		}

		/**
		 * 分割パラメータ列を返す。
		 *
		 * @return 分割パラメータ列
		 */
		public Double[] parameters() {
			return parameters.clone();
		}

		/**
		 * 自由曲線が含まれているかどうか返す。
		 *
		 * @return 含まれているかどうか
		 */
		public boolean isIncludeFreeCurve() {
			return isIncludeFreeCurve;
		}

		/**
		 * 最良分割点列のindexを返す。
		 *
		 * @return 最良分割点列のindex
		 */
		public Integer[] paramNums() {
			return paramNums.clone();
		}
		
		/** グレード値 */
		private final double grade;
		/** 分割数 */
		private final int curveNum;
		/** 分割点列 */
		private final Double[] parameters;
		/** 自由曲線が含まれているか */
		private final boolean isIncludeFreeCurve;
		/** 最良分割点列のindex */
		private final Integer[] paramNums;
	}

	@Override
	public Fragment[] createFragment( SplineCurve _splineCurve ) {
		//同定フラグメントの生成
		IdentificationFragment identificationFragment = IdentificationFragment.create( _splineCurve );
		// FSCの探索点の導出
		double[] searchParameters = calcSearchParameters( _splineCurve, MOVE_PARAM );
		
		// 最良分割パラメータ列を探索（探索アルゴリズムは各自選択してください）
		Double[] separateParameters = getBestDevidedParameters(identificationFragment, _splineCurve, searchParameters );
		
		// 分割対象のfsc
		SplineCurve targetFSC = _splineCurve;
		
		//フラグメント列
		Fragment[] fragments = new Fragment[separateParameters.length + 1];
		
		// 複数分割の場合
		for ( int i = 0; i < separateParameters.length; ++i ) {
			// 分割対象のFSCを最良分割位置で二分
			double parameter = separateParameters[i];

			// フラグメント列に入れる
			fragments[i] = createFragments( parameter, targetFSC )[0];

			// targetFSCの更新
			targetFSC = createFragments( parameter, targetFSC )[1].curve();
			if(i==separateParameters.length-1){
				fragments[separateParameters.length] = createFragments( parameter, targetFSC )[1];
			}
		}
		return fragments;
	}

	@Override
	public SplineCurve[] divide( SplineCurve _splineCurve ) {
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * このクラスのインスタンスを生成します。
	 *
	 * @param _searchInterval 探索区間数
	 * @param _rule 推論ルール
	 * @param _recognizer 幾何曲線認識法のストラテジー
	 */
	public NonPartitionFragmentation( int _searchInterval, Map<String, Sigmoid> _rule, Recognizable _recognizer) {
		m_searchInterval = _searchInterval;
		m_rule = _rule;
		m_recognizer = _recognizer;
	}

	/**
	 * 最良分割点を返します．
	 * @param _identificationFragment 同定フラグメント
	 * @param _fsc FSC
	 * @param _searchEvaluationParameters 探索候補点
	 * @return 最良分割点列
	 */
	@Override
	public Double[] getBestDevidedParameters( IdentificationFragment _identificationFragment, SplineCurve _fsc, double[] _searchEvaluationParameters ) {
		// 最良分割時のパラメータ列の取り出し
		List<Double> dividedParameters = new ArrayList<Double>();
		// 探索対象のFSC
		SplineCurve targetFSC = _fsc;
		// 探索対象の同定フラグメント
		IdentificationFragment identificationFragment = _identificationFragment;
		
		// 探索開始番号
		int searchStartIndex = 0;
		// 分割情報
		DividedCurveInformation dividedCurveInformation;

		// 最良分割点の探索
		do {
			// 最良分割結果の取得(FSCR')
			dividedCurveInformation = searchBestDividedParameters( identificationFragment,
				targetFSC,_searchEvaluationParameters, searchStartIndex );
			Double[] dividedCurveParameters = dividedCurveInformation.parameters();

			// 自由曲線が含まれている場合
			if ( dividedCurveInformation.isIncludeFreeCurve() ) {
				// 12・13.一つ目のパラメータを最良分割点として採用
				dividedParameters.add( dividedCurveParameters[0] );

				// 始点側fragmentの更新
				double firstDividedParameter = dividedCurveParameters[0];

				//パラメータ番号の更新
				searchStartIndex = dividedCurveInformation.paramNums()[0];
				
				// 探索対象のFSCの更新
				Range range = Range.create( firstDividedParameter, targetFSC.evaluateAtEnd().time() );
				targetFSC = targetFSC.part( range );
				// 探索対象の同定フラグメントの更新
				identificationFragment = IdentificationFragment.create( targetFSC );
			}
		} while ( dividedCurveInformation.isIncludeFreeCurve() );
		dividedParameters.addAll( Arrays.asList( dividedCurveInformation.parameters() ) );

		// 分割諸量の出力
		return dividedParameters.toArray( new Double[dividedParameters.size()] );
	}

	/**
	 * 指定されたパラメータでFSCをフラグメント列化する（指定パラメータでばっつり分割）。
	 * XXX 楕円弧幾何曲線列化に用います。
	 * 
	 * @param _splineCurve ファジィスプライン曲線
	 * @param _divisionParameter 分割位置のパラメータ
	 * @return フラグメント列（同定フラグメント）
	 */
	public Fragment[] createFragments( double _divisionParameter, SplineCurve _splineCurve) {
		Fragment[] fragments = new Fragment[ 2 ];
		// 同定フラグメント(Left)を作る-------------
		Range range = Range.create( _splineCurve.range().start(), _divisionParameter );
		fragments[0] = IdentificationFragment.create(_splineCurve.part( range ) );

		// 同定フラグメント(Right)を作る-------------
		range = Range.create(_divisionParameter, _splineCurve.range().end() );
		fragments[1] = IdentificationFragment.create(_splineCurve.part( range ) );

		return fragments;
	}
	
	/**
	 * 指定された分割数で最良分割位置を探索する。
	 *
	 * @param _identificationFragment 探索対象となる同定フラグメント
	 * @param _targetFSC 探索対象となるFSC
	 * @param _startPartitionFragment 始点側の区切りフラグメント
	 * @param _endPartitionFragment 終点側の区切りフラグメント
	 * @param _requestDivisionNum 探索分割単位
	 * @param _dividedParameters これまで分割に使用してきたパラメータ
	 * @param _searchStartIndex 探索開始位置のパラメータ番号
	 * @return 最良分割パラメータ列
	 */
	private DividedCurveInformation searchBestDividedParameters( IdentificationFragment _identificationFragment,
		SplineCurve _targetFSC,  double[] _dividedParameters, int _searchStartIndex ) {

		// 延長FSCを認識
		RecognitionResult recognition = m_recognizer.recognize( _identificationFragment, _targetFSC, m_rule );
		PrimitiveType firstPrimitiveType = recognition.getType();

		// 11.最良分割点列の決定判断(自由曲線じゃなかったら探索終了)
		if ( !firstPrimitiveType.equals( PrimitiveType.OPEN_FREE_CURVE )
			&& !firstPrimitiveType.equals( PrimitiveType.CLOSED_FREE_CURVE ) ) {
			Double[] bestDividedPoints = new Double[0];
			Integer[] numResult = new Integer[0];
			return new DividedCurveInformation( 0.0, 0, bestDividedPoints, false, numResult );
		}
		
		// 探索開始位置
		int searchStartIndex = _searchStartIndex + 1;
		// 探索forを抜けたかどうか判定
		boolean isPassSearch = false;

		// 分割対象となるFSC
		SplineCurve targetFSC = _targetFSC;
		// 分割対象となるFSCを保持
		Deque<SplineCurve> targetFSCStack = new ArrayDeque<SplineCurve>();
		// 最良グレード値を保持
		Deque<Double> maxGradeStack = new ArrayDeque<Double>();
		// 最良分割時のパラメータ位置列を保持
		Deque<List<Double>> bestParamListStack = new ArrayDeque<List<Double>>();
		// 最良分割時のパラメータ位置列の番号を保持
		Deque<List<Integer>> bestParamIndexListStack = new ArrayDeque<List<Integer>>();
		// 最良分割数を保持
		Deque<Integer> minDivisionNumStack = new ArrayDeque<Integer>();
		// 現在のパラメータ位置
		int currentParameterIndex = 0;
		// パラメータ位置の保存
		Stack<Integer> dividedParamIndex = new Stack<Integer>();
		double rightGrade = 0.0;
		// 階層
		int currentDivisionRank = 0;
		// 探索単位
		int requestedDivisionNum = m_searchInterval;

		// 探索
		while ( true ) {
			double targetRangeStart = targetFSC.range().start();
			RecognitionResult dividedPointResult = null;

			// 分割位置の探索（(現在のパラメータ位置+1)から見ていく）
			for ( int index = searchStartIndex; index < _dividedParameters.length; ++index ) {
				double dividedParameter = _dividedParameters[index];
				// 認識させる曲線の生成
				SplineCurve searchedCurve = targetFSC.part( Range.create( targetRangeStart, dividedParameter ) );
				// 同定フラグメント
				IdentificationFragment identificationFragment = IdentificationFragment.create( searchedCurve );
				// 認識
				dividedPointResult = m_recognizer.recognize( identificationFragment, searchedCurve, m_rule );

				// 現在のパラメータ位置の更新
				currentParameterIndex = index;
				// 認識結果の幾何曲線種の取得
				PrimitiveType primitiveType = dividedPointResult.getType();

				// 2.分割判断(自由曲線となってしまった場合break)
				if ( primitiveType.equals( PrimitiveType.OPEN_FREE_CURVE )
					|| primitiveType.equals( PrimitiveType.CLOSED_FREE_CURVE ) ) {

					// 3.次ループの探索開始位置
					searchStartIndex = index;
					// 現在のパラメータ位置保存
					currentParameterIndex = index - 1;
					dividedParameter = _dividedParameters[currentParameterIndex];
					// 認識させる曲線の生成
					searchedCurve = targetFSC.part( Range.create( targetRangeStart, dividedParameter ) );
					// 同定フラグメント
					identificationFragment = IdentificationFragment.create( searchedCurve );
					// 認識
					dividedPointResult = m_recognizer.recognize( identificationFragment, searchedCurve, m_rule );

					// 2・4.探索限界位置の決定
					if ( currentDivisionRank == requestedDivisionNum - 1 ) {
						isPassSearch = false;
						break;
					}

					isPassSearch = true;
					break;
				}

			}

			// 末端の場合
			if ( currentDivisionRank <= requestedDivisionNum - 1 && isPassSearch == false ) {
				// XXX （ここどうするか（右側のFO））認識結果のグレード値取得
//				rightGrade = 1.0;
				rightGrade = dividedPointResult.calcNotFreeCurveGrade( );
				// 最良グレード値にrightGradeをpush
				maxGradeStack.push( rightGrade );
				// 最良分割位置パラメータ列にpush
				bestParamListStack.push( new ArrayList<Double>() );
				bestParamIndexListStack.push( new ArrayList<Integer>() );

				// 現在のパラメータが前のパラメータと一致するかどうか確認用（階層上がる用）
				boolean rankUp;
				do {
					// 階層移動変数の初期化
					rankUp = false;

					// 探索終了
					// XXX【要検討】スタックが空だったら脱出
					if ( dividedParamIndex.empty() ) {
						List<Double> resultParameters = bestParamListStack.pop();
						Double[] bestDividedPoints = new Double[resultParameters.size()];
						for ( int i = 0; i < bestDividedPoints.length; ++i ) {
							bestDividedPoints[i] = resultParameters.get( bestDividedPoints.length - i - 1 );
						}
						List<Integer> resultParameterIndexList = bestParamIndexListStack.pop();
						Integer[] numResult = new Integer[resultParameterIndexList.size()];
						for ( int i = 0; i < numResult.length; ++i ) {
							numResult[i] = resultParameterIndexList.get( numResult.length - i - 1 );
						}

						// 10.分割諸量の出力(番号返す)
						return new DividedCurveInformation( 0.0, 0, bestDividedPoints, true, numResult );
					}

					// パラメータ位置をpop，現在のパラメータ位置にする
					currentParameterIndex = dividedParamIndex.pop();
					// targetFSCをpop，targetFSCにする
					targetFSC = targetFSCStack.pop();
					// 前のパラメータリストに現在のパラメータ位置を追加する
					List<Integer> preParamIndexList = bestParamIndexListStack.pop();
					List<Integer> preParamIndexListOld = new ArrayList<Integer>();
					preParamIndexListOld.addAll( bestParamIndexListStack.peek() );
					preParamIndexList.add( currentParameterIndex );
					// 前のパラメータリストに現在のパラメータ位置を追加する
					List<Double> preParamList = bestParamListStack.pop();
					List<Double> preParamListOld = new ArrayList<Double>();
					preParamListOld.addAll( bestParamListStack.peek() );
					double currentParameter = _dividedParameters[currentParameterIndex];
					preParamList.add( currentParameter );
					// Leftのグレード値算出（フラグメントのstartRange〜現在のパラメータ位置）
					SplineCurve leftCurve = targetFSC.part( Range.create( targetFSC.range().start(), currentParameter ) );
					// 同定フラグメント
					IdentificationFragment identificationFragment = IdentificationFragment.create( leftCurve );
					// 認識
					RecognitionResult leftRecognitionResult = m_recognizer.recognize( identificationFragment, leftCurve, m_rule );
					double leftGrade = leftRecognitionResult.calcNotFreeCurveGrade( );
					// Rightのグレード値を取得（スタックからpop(取得して削除)）
					rightGrade = maxGradeStack.pop();
					// 7.グレード値を計算
					double currentTotalGrade = Math.min( leftGrade, rightGrade );
					// 最良グレード値を参照（peekなので削除はされない）
					double maxGrade = maxGradeStack.peek();
					// 最良分割数を取得（popなので削除される）
					int minDivisionNum = minDivisionNumStack.pop();

					// 8.探索終了判断	
					// 【最良分割点の更新】現在の分割数が前の分割数より小さい場合
					// または、分割数が同じだが現在のグレード値が大きい場合
					if ( ( minDivisionNum > currentDivisionRank )
						|| ( minDivisionNum == currentDivisionRank && maxGrade < currentTotalGrade ) ) {
						minDivisionNumStack.push( currentDivisionRank );
						// 最良分割位置の番号の格納（すり替え）
						bestParamIndexListStack.pop();
						bestParamIndexListStack.push( preParamIndexList );
						// 最良分割位置の格納（すり替え）
						bestParamListStack.pop();
						bestParamListStack.push( preParamList );
						// 最良グレード値の格納（すり替え）
						maxGradeStack.pop();
						maxGradeStack.push( currentTotalGrade );
					} else if ( ( maxGrade >= currentTotalGrade ) ) { // 【この階層で探索打ち切り】現在のグレード値が前のグレード値以下の場合
						// 階層上げ
						rankUp = true;
						// 自由曲線含まれてる
						// --現在の分割数（階層）
						--currentDivisionRank;
						// do Whileの後ろまで飛ばす
						continue;
					} else {
						// この回の値は最良ではないため、前回までの最良結果を入れておく
						bestParamIndexListStack.pop();
						bestParamIndexListStack.push( preParamIndexListOld );
						bestParamListStack.pop();
						bestParamListStack.push( preParamListOld );
						minDivisionNumStack.push( minDivisionNum );
					}
					// --現在の分割数（階層）
					--currentDivisionRank;
					// 9.パラメータの更新
					currentParameterIndex = currentParameterIndex - 1;

					// 前のパラメータ位置
					int preParameterPosition;
					// パラメータ位置スタックが空
					if ( dividedParamIndex.empty() ) {
						// 始点にいる
						preParameterPosition = _searchStartIndex;
					} else {
						// 前のパラメータ位置を取得（削除しない）
						preParameterPosition = dividedParamIndex.peek();
					}
					// 前のパラメータ位置==現在のパラメータ位置だったら階層上がる
					if ( preParameterPosition == currentParameterIndex ) {
						rankUp = true;
					}

				} while ( rankUp );

				// 探索開始位置の更新
				searchStartIndex = currentParameterIndex + 1;

			} else {
				// 最良グレード値を初期化
				maxGradeStack.push( 0.0 );
				// 最良分割数を初期化（現在の分割数+1かな？）
				minDivisionNumStack.push( currentDivisionRank + 1 );
				// 最良分割位置パラメータ列を初期化（空のリストを入れることで，newによる生成コストが少なくなる）
				bestParamListStack.push( Collections.<Double>emptyList() );
				// 最良分割位置パラメータ'番号'を初期化
				bestParamIndexListStack.push( Collections.<Integer>emptyList() );
			}
			// 探索ループ判定をリセット
			isPassSearch = false;
			// 5.現在のパラメータ位置でFSC分割
			Fragment[] fragments = createFragments( _dividedParameters[currentParameterIndex], targetFSC );	
			IdentificationFragment rightFragment = (IdentificationFragment) fragments[1];
			SplineCurve fscRight = (SplineCurve) rightFragment.curve();

			// 現在のパラメータ位置の保存
			dividedParamIndex.push( currentParameterIndex );
			// targetFSCを保存
			targetFSCStack.push( targetFSC );
			// FSCRightをtargetFSCとして設定
			targetFSC = fscRight;
			// 現在の分割数（階層）の更新
			++currentDivisionRank;
		}
	}
	
	/**
	 * FSCの等時間間隔の探索パラメータ列の生成。
	 * （探索時の誤差をなくすため、内分により求める）
	 * @param _targetFSC 探索対象のFSC
	 * @param _movementAmount 探索点の時間間隔(秒)
	 * @return 探索パラメータ列
	 */
	private static double[] calcSearchParameters( SplineCurve _targetFSC, double _movementAmount ) {
		Range targetRange = _targetFSC.range();
		int num = Math.max( (int) Math.round( targetRange.length() / _movementAmount ), 1 );
		double[] dividedParameters = new double[num + 1];
		double targetRangeStart = targetRange.start();
		double targetRangeEnd = targetRange.end();

		for ( int i = 0; i < dividedParameters.length; ++i ) {
			double t = i / (double) num;
			// 探索点格納
			dividedParameters[i] = ( 1 - t ) * targetRangeStart + t * targetRangeEnd;
		}

		return dividedParameters;
	}

	/** 認識ルール */
	private final Map<String, Sigmoid> m_rule;
	/** ファジィオブジェクトスナッピング法のストラテジー */
	private final Recognizable m_recognizer;
	/** 探索単位 */
	private final int m_searchInterval;
	/** 探索移動量Δt(秒) */
	private static final double MOVE_PARAM = 0.1;

}

