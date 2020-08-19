package jp.sagalab.jftk.reference.elliptic;

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
 * 楕円形リファレンスモデルを生成するためのクラスです。
 * @author Akira Nishikawa
 */
public class EllipticReferenceModelGenerator implements ReferenceModelGenerator{

	/**
	 * インスタンス生成
	 * @return 楕円弧リファレンスモデルを生成するためのインスタンス
	 */
	public static EllipticReferenceModelGenerator create() {
		return new EllipticReferenceModelGenerator();
	}

	/**
	 * 楕円形リファレンスモデルを生成します。
	 * <p>
	 * ParametricCurveを構成するファジィ点の中から、
	 * 三角形の面積が最大となるような3点の代表点を探索してリファレンスモデルを生成する。
	 * </p>
	 * @param _curve パラメトリック曲線
	 * @return 楕円形リファレンスモデル
	 * @throws IllegalArgumentException パラメトリック曲線がnullである場合
	 */
	@Override
	public EllipticReferenceModel generateGeneralModel( ParametricCurve _curve ) {
		if ( _curve == null ) {
			throw new IllegalArgumentException( "_curve is null" );
		}
		// 補助点を含む代表点列を見つける
		Point[] rp = searchRepresentationPoints( _curve );
		if ( rp == null ) {
			return null;
		}

		// 重みを導出
		double weight = calculateWeight( rp, _curve );
		// 存在範囲を決定
		Range range = calculateRange( _curve, rp, weight );
		// 楕円形仮設モデルを生成
		QuadraticBezierCurve elliptic = QuadraticBezierCurve.create( rp[0], rp[1], rp[2], weight, range );

		return EllipticReferenceModel.create( elliptic );
	}

	@Override
	public EllipticReferenceModel generateQuarterModel( ParametricCurve _curve ) {
		return generateNQurtersModel( _curve, NQuartersType.QUARTER );
	}

	@Override
	public EllipticReferenceModel generateHalfModel( ParametricCurve _curve ) {
		return generateNQurtersModel( _curve, NQuartersType.HALF );
	}

	@Override
	public EllipticReferenceModel generateThreeQuartersModel( ParametricCurve _curve ) {
		return generateNQurtersModel( _curve, NQuartersType.THREE_QUARTERS );
	}

	private EllipticReferenceModel generateNQurtersModel( ParametricCurve _curve, NQuartersType _type ) {

		double weight = decideWeight( _type );
		Point[] cp = searchRepresentationPoints( _curve, _type );

		QuadraticBezierCurve curve = QuadraticBezierCurve.create( cp[0], cp[1], cp[2], weight, Range.zeroToOne() );
		return EllipticReferenceModel.create( curve, _type );
	}

	/**
	 * 指定されたタイプの重み係数を返します
	 *
	 * @param _type　リダクションモデルのタイプ
	 * @return 重み係数
	 */
	static double decideWeight( NQuartersType _type ) {
		// TODO staticな変数である必要はない?
		double w = -Math.sqrt( 2 ) * 0.5;
		if ( _type == NQuartersType.QUARTER ) {
			w = Math.sqrt( 2 ) * 0.5;
		} else if ( _type == NQuartersType.HALF ) {
			w = 0;
		}
		return w;
	}

	/**
	 * 代表点列の探索を行います。
	 * @param _curve パラメトリック曲線
	 * @return 代表点列
	 */
	static Point[] searchRepresentationPoints( ParametricCurve _curve ) {
		// 直線性を用いて評価点列化
		Point[] evalPoints = _curve.evaluateAllByOptimized( 99, 0.001 );

		// 代表点列
		Point[] rp = null;
		double maxSurfaceArea = Double.NEGATIVE_INFINITY;

		// ベストな代表点列のセットを探索
		for ( int i = 0; i < evalPoints.length / 3; ++i ) {
			Point rp0 = evalPoints[i];
			Point rp2 = evalPoints[evalPoints.length - 1 - i];

			// rp0〜rp2の間のrp0とrp2を除いたすべての点を候補とする
			Point[] slatePoints = new Point[ evalPoints.length - 2 * i ];
			// 評価点列をコピー
			System.arraycopy( evalPoints, i, slatePoints, 0, slatePoints.length );
			// 最遠点候補の取得
			Point rp1 = getBisectingPoint( slatePoints );
			// 代表点によって構成される三角形の頂点
			Point[] triangleVertices = new Point[]{ rp0, rp1, rp2 };
			// 面積を計算
			double surfaceArea = getParallelogramSummation( rp2, rp1, rp0 );

			// 最短の辺の中でも最長であるものに更新
			if ( surfaceArea > maxSurfaceArea ) {
				rp = triangleVertices;
				maxSurfaceArea = surfaceArea;
			}
		}
		return rp;
	}

	/**
	 * 曲線からリダクションモデルの代表点を生成します
	 *
	 * @param _curve FSC
	 * @param _wight 生成したい形状の重み係数
	 * @return リダクションモデル
	 */
	private Point[] searchRepresentationPoints( ParametricCurve _curve, NQuartersType _type ) {
		Point[] points = _curve.evaluateAll( 99, ParametricCurve.EvaluationType.DISTANCE );
		Point[] cp = new Point[ 3 ];
		cp[0] = points[0];
		cp[2] = points[points.length - 1];
		Point f = EllipticReferenceModelGenerator.getBisectingPoint( points );

		cp[1] = f;

		return cp;
	}

	/**
	 * 入力点列から作られる面積を2等分する点を求めます。
	 * @param _points 入力点列
	 * @return 面積を2等分する点
	 */
	static Point getBisectingPoint( Point[] _points ) {
		// 入力点列の始終点を結んだ直線の中点
		Point center = _points[0].internalDivision( _points[_points.length - 1], 1, 1 );
		// 中点と入力点列から生成される面積の配列
		double[] summationList = getSummationList( _points, center );
		double startSum;
		double endSum;
		int start = 0;
		int end = summationList.length - 1;
		int mid = ( summationList.length ) / 2;

		// 2分探索
		while ( true ) {
			startSum = 0;
			endSum = 0;
			for ( int i = 0; i < mid; ++i ) {
				startSum += summationList[i];
			}
			for ( int i = mid; i < summationList.length; ++i ) {
				endSum += summationList[i];
			}
			if ( startSum > endSum ) {
				end = mid;
				mid = start + ( end - start ) / 2;
			} else if ( startSum < endSum ) {
				start = mid;
				mid = start + ( end - start ) / 2;
			} else {
				break;
			}

			if ( ( end - start ) <= 1 && startSum > endSum ) {
				mid = start;
				break;
			} else if ( ( end - start ) <= 1 && startSum < endSum ) {
				mid = end;
				break;
			}
		}

		// 中点を基準とした両側の面積を計算
		double areaA = 0;
		for ( int i = 0; i < mid; ++i ) {
			areaA += summationList[i];
		}
		double areaB = 0;
		for ( int i = mid; i < summationList.length; ++i ) {
			areaB += summationList[i];
		}

		// 最遠点（2分する点）
		Point bisectingPoint;

		if ( areaA > areaB ) {
			// areaAの面積を更新
			areaA = 0;
			for ( int i = 0; i < mid - 1; ++i ) {
				areaA += summationList[i];
			}
			// 面積を2等分する点が含まれる微小平行四辺形の面積
			double sum = summationList[mid - 1];
			if ( sum == 0 ) {
				// 微小平行四辺形が0ならばmidの点を返す
				return _points[mid];
			}
			// 比率の設定
			double ratioA = ( areaB - areaA + sum ) / ( 2 * sum );
			double ratioB = 1 - ratioA;
			bisectingPoint = _points[mid - 1].internalDivision( _points[mid], ratioA, ratioB );
		} else {
			// areaBの面積を更新
			areaB = 0;
			for ( int i = mid + 1; i < summationList.length; ++i ) {
				areaB += summationList[i];
			}
			// 面積を2等分する点が含まれる微小平行四辺形の面積
			double sum = summationList[mid];
			if ( sum == 0 ) {
				// 微小平行四辺形が0ならばmidの点を返す
				return _points[mid];
			}
			// 比率の設定
			double ratioA = ( areaB - areaA + sum ) / ( 2 * sum );
			double ratioB = 1 - ratioA;
			bisectingPoint = _points[mid].internalDivision( _points[mid + 1], ratioA, ratioB );
		}
		return bisectingPoint;
	}

	/**
	 * 重みを導出します。
	 * @param _rp 代表点列
	 * @return 重み
	 */
	static double[] calculateBestWeight( Point[] _rp, ParametricCurve _curve ) {
		// representativePoint[ 0 ] - representativePoint[ 2 ] 間の中点
		Point mid = _rp[0].internalDivision( _rp[2], 1, 1 );

		double alphaPlusBeta = _rp[1].distance( mid );
		// 分割点の設定
		// 分割点は、_curveの始点、代表点3点、_curveの終点とする
		Point[] breakPoints = new Point[]{
			_curve.evaluateAtStart(), _rp[0], _rp[1], _rp[2], _curve.evaluateAtEnd()
		};
		// 重み
		double[] weights = new double[ breakPoints.length - 1 ];

		// alpha + beta == 0 なら計算するまでもない
		if ( alphaPlusBeta > 0 ) {
			// 代表点数
			int rpLength = _rp.length;
			double d_2 = Math.pow( _rp[0].distance( mid ), 2 );
			double alphaPlusBeta_2 = alphaPlusBeta * alphaPlusBeta;

			Vector normal = Vector.createNormal( _rp[0], _rp[1], _rp[2] );
			Vector normal2 = Vector.createSE( _rp[0], _rp[2] ).cross( normal );
			// 重み候補
			if ( normal2.length() > 0 ) {
				for ( int i = 0; i < weights.length; ++i ) {
					// 補助点とそこを通る平面を構築
					Point[] aids = _curve.part( Range.create( breakPoints[i].time(), breakPoints[i + 1].time() ) ).evaluateAll( 20, EvaluationType.TIME );
					Point aid = getBisectingPoint( aids );
					Point t = Plane.create( aid, normal2 ).intersectWith( mid, _rp[1] );
					if ( t == null ) {
						continue;
					}
					double alpha = t.distance( _rp[1] );
					double beta = alphaPlusBeta - alpha;
					double c = t.distance( aid );

					// 分子
					double numerator = 2 * alpha * beta * d_2;
					// 分母
					double denominator = alphaPlusBeta_2 * c * c - alpha * alpha * d_2;
					// 重みの計算
					double w = numerator / denominator - 1;

					if ( !Double.isNaN( w ) ) {
						w = Math.min( Math.max( -0.999, w ), 0.999 );
					}
					weights[i] = w;
				}
			}
		}

		// weights[0]とweights[3]は、どちらか一方を選ぶ
		return new double[]{
			weights[1], weights[2], Math.max( weights[0], weights[3] )
		};
	}

	/**
	 * 重みを導出します。
	 * @param _rp 代表点列
	 * @return 重み
	 */
	static double calculateWeight( Point[] _rp, ParametricCurve _curve ) {
		BrentMethodForElliptic brent = BrentMethodForElliptic.create( _rp, _curve, 1.0e-3 );
		double weight = brent.search( -0.999, 0.999 );
		return weight;
	}

	/**
	 * 存在範囲を導出します。
	 * @param _curve ファジィスプライン曲線
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
	 * 微小な平行四辺形の面積のリストを返します。
	 * @param _point　点列
	 * @param _center 候補点列の始終点を結んだ直線の中点
	 * @return 微小な平行四辺形の配列
	 */
	private static double[] getSummationList( Point[] _point, Point _center ) {
		double[] result = new double[ _point.length - 1 ];
		for ( int i = 0; i < _point.length - 1; ++i ) {
			result[i] = getParallelogramSummation( _center, _point[i], _point[i + 1] );
		}
		return result;
	}

	/**
	 * 3点から外積により平行四辺形の面積を求めます。
	 * @param _base 基点(辺の始点)
	 * @param _baseA 基点から辺Aの終点
	 * @param _baseB 基点から辺Bの終点
	 * @return 面積
	 */
	private static double getParallelogramSummation( Point _base, Point _baseA, Point _baseB ) {
		Vector vecA = Vector.createSE( _baseA, _base );
		Vector vecB = Vector.createSE( _baseB, _base );
		double result = vecA.cross( vecB ).length();
		return result;
	}

	/**
	 * 点と直線の距離の二乗を計算します。
	 * <p>
	 * 直線と点の距離の求め方は「ゲームプログラミングのための3Dグラフィックス数学」より参照。
	 * </p>
	 * @param _base 基点
	 * @param _direction 方向ベクトル
	 * @param _p 点
	 * @return 距離
	 */
	private static double distanceWithPointAndLine( Point _base, Vector _direction, Point _p ) {
		Vector v = Vector.createSE( _base, _p );
		double t = v.dot( _direction );
		return v.square() - t * t / _direction.square();
	}

	/**
	 * 収束の判定を行います。
	 * @param _length 長さ
	 * @param _targetLength 対象の長さ
	 * @return 設定した許容誤差を上回ればtrue
	 */
	private static boolean isConvergence( double _length, double _targetLength ) {
		// 許容誤差を設定
		double lengthRatioTolerance = 1.0E-5;
		double lengthRatio = 1 - ( _length / _targetLength );
		return ( lengthRatio * lengthRatio > lengthRatioTolerance );
	}

	/**
	 * このクラスのインスタンスを生成します。
	 */
	private EllipticReferenceModelGenerator() {
	}
}
