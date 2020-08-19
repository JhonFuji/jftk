package jp.sagalab.jftk;

/**
 * Brent法の計算を行うクラスです。
 * <p>
 * Brent法とは、数値解析における求根アルゴリズムの1つです。
 * ここでは、放物線補間と黄金分割探索を組み合わせたアルゴリズムを用いています。<br>
 * (詳しくは 「ニューメリカルレシピ・イン・シー : C言語による数値計算のレシピ : 日本語版」
 * William H. Press[ほか]著 を参考にしてください。)
 * </p>
 * @author ishiguro
 */
public abstract class BrentMethod {

	/**
	 * コンストラクタ。
	 * @param _tol Brent法による探索の相対誤差
	 * @throws IllegalArgumentException 相対誤差が負かNaNの場合
	 */
	protected BrentMethod(double _tol) {
		if(_tol < 0 || Double.isNaN(_tol)){
			throw new IllegalArgumentException( "_tol is negative sign or NaN." );
		}
		RELATIVE_ERROR = _tol;
	}
	/**
	 * Brent法による関数の最小値探索を行います。
	 * @param _floor 探索範囲の最小値
	 * @param _ceiling 探索範囲の最大値
	 * @return 関数値(f(x))が最小となるときの x の値
	 * @throws IllegalArgumentException 探索範囲の最小値がNaNか無限大の場合
	 * @throws IllegalArgumentException 探索範囲の最大値がNaNか無限大の場合
	 */
	public double search( double _floor, double _ceiling) {
		if(Double.isNaN( _floor)){
			throw new IllegalArgumentException( "_floor is NaN." );
		}
		if(Double.isInfinite(_floor)){
			throw new IllegalArgumentException( "_floor is Infinite." );
		}
		if(Double.isNaN( _ceiling)){
			throw new IllegalArgumentException( "_ceiling is NaN." );
		}
		if(Double.isInfinite(_ceiling)){
			throw new IllegalArgumentException( "_ceiling is Infinite." );
		}
		
		// このときの関数値が、目的関数の最小値となる
		double result = 0.0;
		// 前回の探索結果の更新量
		double renewUpdateResult = 0.0;
		// 前々回の探索結果の更新量
		double lastButOneRenewUpdateResult = 0.0;
		// 探索結果の更新量
		double updateResult;
		// 探索時の探索範囲の更新量
		double updateSearchRangeA = 0.0;
		double updateSearchRangeB = 0.0;
		// 探索範囲の上限
		double floor;
		double ceiling;
		if ( _floor < _ceiling ) {
			floor = _floor;
			ceiling = _ceiling;
		} else {
			floor = _ceiling;
			ceiling = _floor;
		}

		// Brent法での最小値探索
		for ( int i = 0; i < ITMAX; ++i ) {
			// 前回の探索での定義域の中点
			double previousMedian = 0.5 * ( floor + ceiling );
			// 探索での許容誤差
			double tolerance = RELATIVE_ERROR * Math.abs( result ) + ABSOLUTE_ERROR;
			double twiceTol = 2.0 * tolerance;
			// 収束判定
			if ( Math.abs( result - previousMedian ) <= ( twiceTol - 0.5 * ( ceiling - floor ) ) ) {
				// 最良の値を返す
				return result;
			}

			// 放物線補間を行う
			// 放物線補間は以下の手順で最小値を求める
			// 1. 評価する関数(目的関数)上にある3点から放物線を求める
			// 2. 1で求めた放物線から、新たな3点を求める
			// 3. 最小値の見極めを行う
			if ( Math.abs( lastButOneRenewUpdateResult ) > tolerance ) {
				// 放物線補間を行うための3点
				// 3点の中でx軸方向に最小のもの
				double a = updateSearchRangeA;
				//  aの評価値
				double evalA = function( a );
				// 3点の中でx軸方向に最大のもの
				double c = result;
				//  cの評価値
				double evalC = function( c );
				// a < c となる点
				double b = updateSearchRangeB;
				//  bの評価値
				double evalB = function( b );

				// 放物線補間で最小値を探索するための更新量の分子 p
				double numerator = ( c - a ) * ( c - a ) * ( evalC - evalB ) - ( c - b ) * ( c - b ) * ( evalC - evalA );
				// 放物線補間で最小値を探索するための更新量の分母 q
				double denominator = 2 * ( ( c - a ) * ( evalC - evalB ) - ( c - b ) * ( evalC - evalA ) );
				// 探索結果を更新するために、符号を定義に合わせる
				numerator = ( denominator > 0.0 ) ? -numerator : numerator;
				denominator = Math.abs( denominator );

				double tmpLastButOneUpdateResult = lastButOneRenewUpdateResult;
				lastButOneRenewUpdateResult = renewUpdateResult;

				// 放物線補間の適否の検査
				// 以下の条件を満たしていれば放物線補間を採択
				// 1. 最小値を求める区間に、前々回の更新量(lastButOneUpdateResult)が存在すること
				// 2. 前回の探索結果の更新量が、前々回の更新量の半分より小さいこと
				if ( Math.abs( numerator ) >= Math.abs( 0.5 * denominator * tmpLastButOneUpdateResult )
					|| numerator <= denominator * ( floor - result )
					|| numerator >= denominator * ( ceiling - result ) ) {
					// 放物線補間は不適なので、黄金分割比を採択する
					// 大きい区間の方を黄金分割比で求める
					lastButOneRenewUpdateResult = ( result >= previousMedian ) ? floor - result : ceiling - result;
					renewUpdateResult = GOLDEN_RATIO * lastButOneRenewUpdateResult;
				} else {
					// 条件を満たすため、放物線補間を採択する
					renewUpdateResult = numerator / denominator;
					// 探索結果を更新
					updateResult = result + renewUpdateResult;
					if ( updateResult - floor < twiceTol || ceiling - updateResult < twiceTol ) {
						renewUpdateResult = sign( tolerance, previousMedian - result );
					}
				}
			} else {
				// 黄金分割比を採択する
				lastButOneRenewUpdateResult = ( result >= previousMedian ) ? floor - result : ceiling - result;
				renewUpdateResult = GOLDEN_RATIO * lastButOneRenewUpdateResult;
			}
			updateResult = result + ( ( Math.abs( renewUpdateResult ) >= tolerance ) ? renewUpdateResult : sign( tolerance, renewUpdateResult ) );
			// ここでこの探索の関数評価を行う
			double evaluate = function( updateResult );
			double evaResult = function( result );
			if ( evaluate <= evaResult ) {
				if ( updateResult >= result ) {
					floor = result;
				} else {
					ceiling = result;
				}
				// 探索範囲の更新
				updateSearchRangeA = updateSearchRangeB;
				updateSearchRangeB = result;
				result = updateResult;
			} else {
				if ( updateResult < result ) {
					floor = updateResult;
				} else {
					ceiling = updateResult;
				}
				//  updateSearchRangeAの評価値
				double evalA = function( updateSearchRangeA );
				//  updateSearchRangeBの評価値
				double evalB = function( updateSearchRangeB );
				if ( evaluate <= evalB || isConvergence( updateSearchRangeB, result, ABSOLUTE_ERROR ) ) {
					updateSearchRangeA = updateSearchRangeB;
					updateSearchRangeB = updateResult;
				} else if ( evaluate <= evalA
					|| isConvergence( updateSearchRangeA, result, ABSOLUTE_ERROR )
					|| isConvergence( updateSearchRangeA, updateSearchRangeB, ABSOLUTE_ERROR ) ) {
					updateSearchRangeA = updateResult;
				}
			}
		}

		return result;
	}

	/**
	 * 評価関数。
	 * @param _x パラメータ
	 * @return 評価値
	 */
	public abstract double function( double _x );

	/**
	 * 符号の付け替えを行います。
	 * (参考 : Fortranの関数)
	 * _b ＞= 0 なら | _a |を返し、 _b ＜ 0 なら -| _a |を返します。
	 * @param _a 符号を付け替える値
	 * @param _b 基準となる値
	 * @return 符号の付け替えを行った値
	 */
	private static double sign( double _a, double _b ) {
		if ( _b >= 0.0 ) {
			return Math.abs( _a );
		} else {
			return -Math.abs( _a );
		}
	}

	/**
	 * 与えられた2変数が近似しているかを判定します。
	 * @param _a 基準a
	 * @param _b 基準b
	 * @param _eps 許容値
	 * @return 2変数が近似しているか
	 */
	private static boolean isConvergence( double _a, double _b, double _eps ) {
		return ( Math.abs( _a - _b ) <= _eps );
	}

	/** Brent法による探索の相対誤差 */
	private final double RELATIVE_ERROR;
	/** 黄金分割比 */
	private static final double GOLDEN_RATIO = 0.3819660;
	/** 反復回数の上限 */
	private static final int ITMAX = 20;
	/** 絶対精度 */
	private static final double ABSOLUTE_ERROR = 1.0E-5;
}
