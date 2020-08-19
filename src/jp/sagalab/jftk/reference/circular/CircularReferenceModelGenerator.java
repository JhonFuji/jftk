package jp.sagalab.jftk.reference.circular;

import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.ParametricCurve;
import jp.sagalab.jftk.curve.ParametricEvaluable.EvaluationType;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.reference.ReferenceModelGenerator;

/**
 * 円形リファレンスモデルを生成するためのクラスです。
 * @author Akira Nishikawa
 */
public class CircularReferenceModelGenerator implements ReferenceModelGenerator {

	/**
	 * このクラスのインスタンスを生成します。
	 * @return 円弧リファレンスモデルを生成するためのインスタンス
	 */
	public static CircularReferenceModelGenerator create(){
		return new CircularReferenceModelGenerator();
	}
	
	/**
	 * 円形リファレンスモデルを生成します。
	 * <p>
	 * ParametricCurveを構成するファジィ点の中から、
	 * 三角形の面積が最大となるような3点の代表点を探索してリファレンスモデルを生成する。<br>
	 * 円形において、面積が最大となる三角形を得る3点を選出することと
	 * 最短の辺が最大となる三角形を得る3点を選出することは同義である。
	 * </p>
	 * @param _curve パラメトリック曲線
	 * @return 円形リファレンスモデル
	 * @throws IllegalArgumentException パラメトリック曲線がnullの場合
	 */
	@Override
	public CircularReferenceModel generateGeneralModel( ParametricCurve _curve ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( " _curve is null" );
		}
		// 代表点列の決定
		Point[] cp = searchRepresentationPoints( _curve );

		// 重みの決定
		double weight = calculateWeight( cp );

		// パラメータ範囲の決定
		Range range = calculateRange( _curve, cp, weight );

		QuadraticBezierCurve curve = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], weight, range );

		return CircularReferenceModel.create(curve, NQuartersType.GENERAL );
	}

	@Override
	public CircularReferenceModel generateQuarterModel( ParametricCurve _curve ) {
		return generateNQuartersModel( _curve, NQuartersType.QUARTER );
	}

	@Override
	public CircularReferenceModel generateHalfModel( ParametricCurve _curve ) {
		return generateNQuartersModel( _curve, NQuartersType.HALF );
	}

	@Override
	public CircularReferenceModel generateThreeQuartersModel( ParametricCurve _curve ) {
		return generateNQuartersModel( _curve, NQuartersType.THREE_QUARTERS );
	}

	private CircularReferenceModel generateNQuartersModel( ParametricCurve _curve, NQuartersType _type ) {
		// 重みの決定
		double weight = decideWeight( _type );
		// 代表点列の決定
		Point[] cp = searchRepresentationPoints( _curve, weight );

		QuadraticBezierCurve curve = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], weight, Range.zeroToOne() );

		return CircularReferenceModel.create( curve, _type );
	}

	/**
	 * 代表点列の探索を行います。
	 * <p>
	 * rp0は前半1/3、rp2は後半1/3から抽出されます。<br>
	 * rp1はrp0とrp2の間です。
	 * </p>
	 * @param _curve パラメトリック曲線
	 * @return 代表点列
	 */
	static Point[] searchRepresentationPoints( ParametricCurve _curve ) {
		// 評価点列化
		// TODO 99点のサンプルで大丈夫か？
		Point[] points = _curve.evaluateAll( 99, EvaluationType.DISTANCE );

		// 代表点列
		Point[] rp = null;

		// 探索は全体の1/3（rp0は最初1/3、rp2は最後1/3から決定し、rp1はその間で探索）
		int searchNum = points.length / 3;

		// 三角形の最短の辺の長さの最大値
		double maxDistance = Double.NEGATIVE_INFINITY;

		// ベストな代表点列のセットを探索
		for ( int i = 0; i < searchNum; ++i ) {
			Point rp0 = points[i];
			Point rp2 = points[points.length - 1 - i];
			// rp1の取得
			Point rp1 = getMidPoint( rp0, rp2, _curve );
			// 代表点によって構成される三角形の頂点
			Point[] tmp = new Point[]{ rp0, rp1, rp2 };

			// 代表点列で形成される三角形の最短の辺の長さ
			double distance = Double.POSITIVE_INFINITY;
			// tmpの要素数
			int length = tmp.length;
			// 三角形の各辺の長さを計算し、最短のものを求める
			for ( int j = 0; j < length; ++j ) {
				// 2点間の距離を計算
				distance = Math.min( tmp[j].distance( tmp[( j + 1 ) % length] ), distance );
			}
			// 最短の辺の中でも最長であるものに更新
			if ( distance > maxDistance ) {
				rp = tmp;
				maxDistance = distance;
			}
		}

		return rp;
	}

	/**
	 * 曲線からリダクションモデルの代表点を生成します
	 *
	 * @param _curve FSC
	 * @param _weight 生成したい形状の重み係数
	 * @return リダクションモデル
	 */
	private Point[] searchRepresentationPoints( ParametricCurve _curve, double _weight ) {
		//代表点三点を選出する
		//cp0は始点、cp2は終点
		double weight = _weight;
		// 代表点列
		Point[] rp;

		Point cp0 = _curve.evaluateAtStart();
		Point cp2 = _curve.evaluateAtEnd();
		Point m = cp0.internalDivision( cp2, 1, 1 );

		Point cp1 = getMidPoint( cp0, cp2, _curve );
		// cp1 の位置を調整
		double L = m.distance( cp0 );
		double H = Math.sqrt( ( 1 - weight ) / ( 1 + weight ) ) * L;
		double tmpH = m.distance( cp1 );
		double ratio = ( H - tmpH ) / tmpH;
		cp1 = cp1.move( Vector.createSE( m, cp1 ).magnify( ratio ) );

		// 代表点によって構成される三角形の頂点
		Point[] cp = new Point[]{ cp0, cp1, cp2 };
		rp = cp;

		return rp;
	}

	/**
	 * 代表点1を求めます。
	 * @param _rp0 代表点0
	 * @param _rp2 代表点2
	 * @param _curve スプライン曲線
	 * @return 代表点１
	 */
	static Point getMidPoint( Point _rp0, Point _rp2, ParametricCurve _curve ) {
		// _rp0と_rp2の中点
		Point mid = _rp0.internalDivision( _rp2, 1, 1 );
		// _rp0 -> _rp2のベクトル
		Vector normal = Vector.createSE( _rp0, _rp2 ).normalize();
		// 中間点の初期値はrp0とrp2のパラメータ的に中間の点をセットしておく
		Point intersection = _curve.evaluateAt( ( _rp0.time() + _rp2.time() ) * 0.5 );
		if ( !Double.isInfinite( 1 / normal.length() ) ) {
			// _rp0と_rp2の垂直二等分面
			Plane plane = Plane.create( mid, normal );

			// _rp0から_rp2までの部分区間を抽出
			ParametricCurve part = _curve.part( Range.create( _rp0.time(), _rp2.time() ) );
			// 部分区間内での交点群を導出
			Point[] intersections = part.intersectionWith( plane );

			// 交点は見つからなかったときは曲線が点に縮退しているときのはずなので、なんでも良いはず
			// rp0とrp2のパラメータ的に中間の点を入れておく
			if ( intersections.length > 0 ) {
				intersection = intersections[0];
			}
		}

		return intersection;
	}

	/**
	 * 重みの導出します。
	 * @param _rp 代表点列
	 * @return 重み
	 * @throws RuntimeException ほぼ直線状の代表点列の場合
	 */
	static double calculateWeight( Point[] _rp ) {
		Point mid = _rp[0].internalDivision( _rp[2], 1, 1 );

		double L = mid.distance( _rp[2] );
		double H = mid.distance( _rp[1] );

		double L2 = L * L;
		double H2 = H * H;

		double weight = ( L2 - H2 ) / ( L2 + H2 );
		if ( Double.isNaN( weight ) ) {
			weight = 0;
		}
		weight = Math.min( weight, 0.999 );
		weight = Math.max( weight, -0.999 );

		// 重みが非数になる＝代表点が１点に収束しているので、重みを０にセット
		return weight;
	}

	/**
	 * 存在範囲を導出します。
	 * @param _curve スプライン曲線
	 * @param _rp 代表点列
	 * @param _weight 重み
	 * @return 存在範囲
	 */
	static Range calculateRange( ParametricCurve _curve, Point[] _rp, double _weight ) {
		// 代表点0 〜 代表点2の間に対応する元曲線の部分長
		double centerLength = _curve.part( Range.create( _rp[0].time(), _rp[2].time() ) ).length();

		// 始点から代表点0の時刻に対応する元曲線の部分長
		double preLength = _curve.part( Range.create( _curve.range().start(), _rp[0].time() ) ).length();
		// 代表点2から終点の時刻に対応する元曲線の部分長
		double postLength = _curve.part( Range.create( _rp[2].time(), _curve.range().end() ) ).length();

		// リファレンスモデルの生成
		QuadraticBezierCurve model = QuadraticBezierCurve.create( _rp[0], _rp[1], _rp[2], _weight, Range.zeroToOne() );
		// リファレンスモデルと元曲線での中央部分での長さ比
		double ratio = model.length() / centerLength;

		// リファレンスモデルにおける始点側・終点側の部分長
		preLength *= ratio;
		postLength *= ratio;

		if ( Double.isInfinite( preLength ) || Double.isInfinite( postLength ) ) {
			return Range.zeroToOne();
		}

		// 許容誤差を設定
		double timeTolerance = 1.0E-14;

		double step = -0.1;
		double tS = 0;
		double length = 0;
		double tmpLength = 0;
		// ステップの更新が反映されているかどうか
		boolean isUpdateStep = false;

		// 許容誤差内になるまでループ
		while ( isConvergence( length, preLength ) ) {
			// 部分曲線を生成
			double evalTime = tS + step;
			// 部分曲線の長さ
			length = QuadraticBezierCurve.create( _rp[0], _rp[1], _rp[2], _weight, Range.create( evalTime, 0 ) ).length();
			double remainLength = preLength - tmpLength;
			double extensionLength = length - tmpLength;
			if ( remainLength < extensionLength ) { // 残りを上回ってしまったら、ステップ幅を短くして続ける
				step *= remainLength / extensionLength;
				isUpdateStep = true;
				if ( Math.abs( step ) < timeTolerance ) {
					break;
				}
			} else { //ステップ更新
				tS = evalTime;
				isUpdateStep = false;
				tmpLength = length;
			}
		}
		if ( isUpdateStep ) {
			tS += step;
		}

		step = 0.1;
		double tE = 1;
		length = 0;
		tmpLength = 0;
		isUpdateStep = false;
		while ( isConvergence( length, postLength ) ) {
			double evalTime = tE + step;
			// 部分曲線を生成
			length = QuadraticBezierCurve.create( _rp[0], _rp[1], _rp[2], _weight, Range.create( 1, evalTime ) ).length();
			double remainLength = postLength - tmpLength;
			double extensionLength = length - tmpLength;
			if ( remainLength < extensionLength ) { // 残りを上回ってしまったら、ステップ幅を短くして続ける
				step *= remainLength / extensionLength;
				isUpdateStep = true;
				if ( step < timeTolerance ) {
					break;
				}
			} else { //ステップ更新
				tE = evalTime;
				isUpdateStep = false;
				tmpLength = length;
			}
		}
		if ( isUpdateStep ) {
			tE += step;
		}

		return Range.create( tS, tE );
	}

	/**
	 * 収束の判定を行います。
	 * @param _length 長さ
	 * @param _targetLength 対象の長さ
	 * @return 設定した許容誤差を上回ればtrue
	 */
	private static boolean isConvergence( double _length, double _targetLength ) {
		// 許容誤差を設定
		double lengthRatioTolerance = 1.0E-6;
		double lengthRatio = 1 - ( _length / _targetLength );
		return ( lengthRatio * lengthRatio > lengthRatioTolerance );
	}

	/**
	 * 指定されたタイプの重み係数を返します
	 * @param _type　リダクションモデルのタイプ
	 * @return 重み係数
	 */
	private double decideWeight( NQuartersType _type ) {
		// TODO staticな変数である必要はない?
		double w = -Math.sqrt( 2 ) * 0.5;
		if ( _type == NQuartersType.QUARTER ) {
			w = Math.sqrt( 2 ) * 0.5;
		} else if ( _type == NQuartersType.HALF ) {
			w = 0;
		}
		return w;
	}
	
	private CircularReferenceModelGenerator() {
	}

}
