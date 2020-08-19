package jp.sagalab.jftk.curve;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import jp.sagalab.jftk.FuzzySet;
import jp.sagalab.jftk.GeomUtil;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.Transformable;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.Vector;

/**
 * パラメトリック曲線を表すクラスです。
 * @author miwa
 */
public abstract class ParametricCurve implements ParametricEvaluable<Point>,
	Transformable<ParametricCurve>, Invertible<ParametricCurve>{

	/**
	 * パラメトリック曲線を生成。
	 * @param _range 曲線の範囲
	 */
	protected ParametricCurve( Range _range ) {
		m_range = Range.create(_range.start(), _range.end());
	}
	
	@Override
	public Point evaluateAt( double _parameter ) {
		// 範囲チェック
		if ( !m_range.isInner( _parameter ) ) {
			throw new OutOfRangeException(
				String.format( "_parameter:%.3f is out of range:%s", _parameter, m_range ) );
		}

		return evaluate( _parameter );
	}

	/**
	 * パラメータ範囲全体に対応する点列を評価します。
	 * @param _num 評価数
	 * @param _type 評価タイプ
	 * @return 評価点列
	 * @throws OutOfRangeException 評価数が2未満の場合
	 * @throws UnsupportedOperationException 評価タイプがTIMEまたはDISTANCEでない場合
	 */
	@Override
	public Point[] evaluateAll( int _num, ParametricCurve.EvaluationType _type ) {
		// 評価点数チェック
		if ( _num < 2 ) {
			throw new OutOfRangeException(
				String.format( "_num:%d must be more than 1", _num ) );
		}
		Point[] points;
		switch ( _type ) {
			case TIME:
				points = evaluateAllByTime( _num );
				break;
			case DISTANCE:
				points = evaluateAllByDistance( _num );
				break;
			default:
				throw new UnsupportedOperationException("_type is not TIME or DISTANCE");
		}

		return points;
	}

	@Override
	public Point evaluateAtStart() {
		return evaluateAt( m_range.start() );
	}

	@Override
	public Point evaluateAtEnd() {
		return evaluateAt( m_range.end() );
	}

	@Override
	public Range range() {
		return m_range;
	}

	/**
	 * 直線性を用いて曲線の評価点列を生成します。
	 * @param _divisionNum 分割数
	 * @param _threshold 直線性判定の閾値
	 * @return 評価点列
	 * @throws IllegalArgumentException _divisionNumが0未満の場合
	 */
	public Point[] evaluateAllByOptimized( int _divisionNum, double _threshold ) {
		if ( _divisionNum < 0 ) {
			throw new IllegalArgumentException("_divisionNum < 0");
		}
		// 分母の値
		int denominator = _divisionNum + 1;
		Range range = range();
		// 開始パラメータ
		double start = range.start();
		// 終了パラメータ
		double end = range.end();

		List<Point> points = new ArrayList<Point>();

		// 区間開始点
		Point pre = evaluateAtStart();
		points.add( pre );
		// 区間終了点群
		Deque<Point> posts = new ArrayDeque<Point>();
		// 区間終了点
		double t = 1.0 / denominator;
		Point post = evaluateAt( ( 1 - t ) * start + t * end );
		// 指定された分割数に達するまでループ
		int i = 2;
		while ( i <= denominator ) {
			double preTime = pre.time();
			double postTime = post.time();
			// 中間点
			double midTime = ( preTime + postTime ) / 2;
			Point mid = evaluateAt( midTime );
			// 直線性の導出
			double distance = GeomUtil.distanceWithPointAndLine( pre, Vector.createSE( pre, post ), mid );
			// 終了点群数
			int postsNum = posts.size();
			// 直線性がなければ
			if ( ( preTime < midTime && midTime < postTime )
				&& distance > _threshold * pre.distance( post )
				&& postsNum < LIMIT_OF_SEARCH_DEEPTH ) { // NG
				// 区間終了点群に現在の区間終了点を追加し、中間点を区間終了点に再セット
				posts.offerLast( post );
				post = mid;
			} else { // OK
				// 区間開始点に現在の区間終了点をセット
				points.add( post );
				pre = post;
				// 区間終了点群が存在すれば
				if ( postsNum > 0 ) {
					// 新たな区間終了点に区間終了点群の最新のものをセット
					post = posts.pollLast();
				} else { // 存在しなければ
					// 新たに評価
					t = i / (double) denominator;
					post = evaluateAt( ( 1 - t ) * start + t * end );
					++i;
				}
			}
		}
		points.add( post );

		return points.toArray( new Point[ points.size() ] );
	}

	/**
	 * 指定された曲線に含まれているかを評価します。
	 * @param _other 曲線
	 * @param _num 評価点数
	 * @return 区間真理値
	 * @throws IllegalArgumentException 他のパラメータ評価クラスがNullである場合
	 * @throws IllegalArgumentException 評価点数が0点以下の場合
	 */
	public TruthValue includedIn( ParametricEvaluable<?> _other, int _num ) {
		if(_other == null){
			throw new IllegalArgumentException("_other is null");
		}
		if(_num <= 0){
			throw new IllegalArgumentException("_num <= 0");
		}
		
		FuzzySet[] pointsA = evaluateAll( _num, EvaluationType.DISTANCE );
		FuzzySet[] pointsB = _other.evaluateAll( _num, EvaluationType.DISTANCE );

		double nec = 1;
		double pos = 1;
		for ( int i = 0; i < _num; ++i ) {
			TruthValue tv = pointsA[i].includedIn( pointsB[i] );
			nec = Math.min( nec, tv.necessity() );
			pos = Math.min( pos, tv.possibility() );
		}
		return TruthValue.create( nec, pos );
	}

	/**
	 * 曲線の長さを返します。
	 * @return 曲線の長さ
	 */
	public double length() {
		Point[] samplePoints = evaluateAllByOptimized( 100, 0.001 );
		return Point.length( samplePoints );
	}

	/**
	 * 指定した平面との交点列を返します。
	 * @param _plane 平面
	 * @return 交点列
	 */
	public abstract Point[] intersectionWith( Plane _plane );

	/**
	 * 指定した範囲で切り出した曲線を返します。
	 * @param _range 範囲
	 * @return 部分曲線
	 */
	public abstract ParametricCurve part(Range _range) throws OutOfRangeException;

	/**
	 * 等時間間隔でこの曲線の評価点列を生成します。
	 * @param _num 評価数
	 * @return 評価点列
	 */
	protected Point[] evaluateAllByTime( int _num ) {
		// 評価点列
		Point[] points = new Point[ _num ];

		// 内分で評価パラメータを決定（パラメータのズレが累積しない）
		double start = m_range.start();
		double end = m_range.end();

		if ( _num > 0 ) {
			// 始点
			points[0] = evaluateAtStart();
			double step = ( end - start ) / ( _num - 1 );
			for ( int i = 1; i < _num - 1; ++i ) {
				points[i] = evaluateAt( start + i * step );
			}
			// 終点
			points[points.length - 1] = evaluateAtEnd();
		}

		return points;
	}

	/**
	 * 等距離間隔でこの曲線の評価点列を生成します。
	 * <p>
	 * 「等距離」はあくまで近似であり、完璧な等距離間隔の評価にはなりません。
	 * 近似精度は曲線全体の長さから相対値で決定しています。
	 * </p>
	 * @param _num 評価点数
	 * @return 評価点列
	 */
	protected Point[] evaluateAllByDistance( int _num ) {
		// TODO 曲線によって、その特性を活かした精度と速度の両立が可能
		// ここで評価するパラメータのリスト化を行う
		// リスト化の処理でprotectedなメソッドを呼ぶ（そこでクラスごとの最適化？）

		// 等距離間隔のためのサンプル評価点列
		// サンプル評価点列には直線性での評価点列を使用
		Point[] samplePoints = evaluateAllByOptimized( 100, 0.001 );
		// 曲線の長さ
		double length = Point.length( samplePoints );
		Point[] points = new Point[ _num ];

		// 始点
		points[0] = evaluateAtStart();

		// 現在の評価点番号
		int n = 1;

		// 直線性で評価した曲線の更新距離
		// ループごとに直線性で評価した点列の隣接点間の距離を累積した値
		double optLengthStep = 0;
		// 各点間の距離を測りつつ、評価点を追加していく
		for ( int i = 1; i < samplePoints.length; ++i ) {
			// 隣接サンプル点の距離
			double optDelta = samplePoints[i - 1].distance( samplePoints[i] );
			if ( optDelta > 0 ) {
				// 等距離で評価する曲線の更新距離
				// ループごとに等距離評価した点列の隣接点間の距離を累積した値
				double disLengthStep = n * length / ( _num - 1 );
				while ( n < _num - 1 && disLengthStep <= optLengthStep + optDelta ) {
					double w = ( disLengthStep - optLengthStep ) / optDelta;
					double t = ( 1 - w ) * samplePoints[i - 1].time() + w * samplePoints[i].time();
					points[n++] = evaluateAt( t );
					disLengthStep = n * length / ( _num - 1 );
				}
			}
			optLengthStep += optDelta;
		}

		// 残りは全部最後の点を詰める
		while ( n < _num ) {
			points[n++] = evaluateAtEnd();
		}

		return points;
	}
	
	/**
	 * 指定されたパラメータでの点を評価します。
	 * @param _parameter パラメータ
	 * @return 評価点
	 */
	protected abstract Point evaluate( double _parameter );

	/**
	 * この ParametricCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この ParametricCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * パラメータ範囲がまったく同じ ParametricCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final ParametricCurve other = (ParametricCurve) obj;

		return this.m_range == other.m_range || ( this.m_range != null && this.m_range.equals( other.m_range ) );
	}

	/**
	 * この ParametricCurve のハッシュコードを返します。
	 * @return この ParametricCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + ( this.m_range != null ? this.m_range.hashCode() : 0 );
		return hash;
	}

	/**
	 * この ParametricCurve の文字列表現を返します。
	 * @return パラメータ範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "range:%s", m_range );
	}

	/** パラメータ範囲 */
	private final Range m_range;
	/** 線形性探索を行う深さの限界 */
	private static final int LIMIT_OF_SEARCH_DEEPTH = 3;
}