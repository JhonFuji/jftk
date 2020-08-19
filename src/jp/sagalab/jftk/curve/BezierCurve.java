package jp.sagalab.jftk.curve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import jp.sagalab.jftk.GeomUtil;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * ベジェ曲線を表すクラスです。
 * @author Akira Nishikawa
 */
public class BezierCurve extends ParametricCurve {

	/**
	 * ベジェ曲線を生成します。
	 * @param _cp 制御点列
	 * @param _range パラメータ範囲
	 * @return べジェ曲線
	 * @throws IllegalArgumentException 制御点列の配列長が1より下の場合
	 * @throws OutOfRangeException      パラメータ範囲が0から1の範囲に含まれていない場合
	 */
	public static BezierCurve create( Point[] _cp, Range _range ){	
		if ( !Range.zeroToOne().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of max range:%s", _range, Range.zeroToOne() ) );
		}
		if ( _cp.length < 1 ) {
			throw new IllegalArgumentException( "_cp.length < 1 ." );
		}
		if ( Arrays.asList( _cp ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException();
		}
		return new BezierCurve(_cp, _range);
	}
	
	/**
	 * 1/4円弧を近似する3次ベジェ曲線を生成します。
	 * <p>
	 * 生成されるのはXY平面上の第1象限の半径1の1/4円弧となります。
	 * </p>
	 *
	 * @param _major 長径（X軸方向）
	 * @param _minor 短径（Y軸方向）
	 * @return 1/4円弧を近似する3次ベジェ曲線
	 */
	public static BezierCurve generateQuadArc( double _major, double _minor ) {
		// 1/4楕円弧近似のマジックナンバー
		final double ratio = ( Math.sqrt( 2 ) - 1 ) * 4 / 3;

		Point[] cp = new Point[]{
			Point.createXYZ( _major, 0, 0 ),
			Point.createXYZ( _major, _minor * ratio, 0 ),
			Point.createXYZ( _major * ratio, _minor, 0 ),
			Point.createXYZ( 0, _minor, 0 )
		};

		return new BezierCurve( cp, Range.zeroToOne() );
	}

	/**
	 * 指定されたパラメータでこの曲線を分割します。
	 * @param _parameter パラメータ
	 * @return 分割後の曲線列
	 * @throws OutOfRangeException パラメータがパラメータ範囲に含まれていない場合
	 */
	public BezierCurve[] divide( double _parameter ) {
		if ( !range().isInner( _parameter ) ) {
			throw new OutOfRangeException( String.format( "_parameter:%f is out of range:%s", _parameter, range() ) );
		}
		Point[] cp = m_cp.clone();
		final int degree = cp.length - 1;

		Point[] preCP = new Point[ degree + 1 ];
		Point[] postCP = new Point[ degree + 1 ];

		for ( int i = 0; i < degree; ++i ) {
			preCP[i] = cp[0];
			postCP[postCP.length - 1 - i] = cp[cp.length - 1 - i];
			for ( int j = 0; j < degree - i; ++j ) {
				cp[j] = cp[j].internalDivision( cp[j + 1], _parameter, 1 - _parameter );
			}
		}

		preCP[preCP.length - 1] = cp[0];
		postCP[0] = cp[0];

		return new BezierCurve[]{
				new BezierCurve(preCP, Range.zeroToOne() ),
				new BezierCurve( postCP, Range.zeroToOne() )
			};
	}

	/**
	 * 指定された点までの距離を求めます。
	 * @param _p 点
	 * @return 最小〜最大距離範囲
	 */
	public Range distance( Point _p ) {

		Point start = m_cp[0];
		Point end = m_cp[m_cp.length - 1];
		// 制御点の先頭と点との距離
		double distanceSP = start.distance( _p );
		// 制御点の末尾と点との距離
		double distanceEP = end.distance( _p );
		// 制御点の先頭と末尾を結んだ線分と点との距離
		double distanceP = GeomUtil.distance( start, end, _p );

		// 制御点の先頭と末尾を結んだ線分と各制御点との距離の最大値
		double distanceCP = Double.POSITIVE_INFINITY;
		for ( int i = 1; i < m_cp.length - 1; ++i ) {
			double distance = GeomUtil.distance( start, end, m_cp[i] );
			distanceCP = Math.max( distanceCP, distance );
		}

		double minDistance = Math.max( 0, distanceP - distanceCP );
		double maxDistance = Math.min( distanceSP, distanceEP );
		maxDistance = Math.min( maxDistance, distanceP + distanceCP );

		return Range.create( minDistance, maxDistance );
	}

	@Override
	public double length() {
		return Point.length( evaluateAllByOptimized( m_cp.length, 0.001 ) );
	}

	@Override
	protected Point evaluate( double _parameter ) {
		Point[] cp = m_cp.clone();
		final int degree = cp.length - 1;
		for ( int i = 0; i < degree; ++i ) {
			for ( int j = 0; j < degree - i; ++j ) {
				cp[j] = cp[j].internalDivision( cp[j + 1], _parameter, 1 - _parameter );
			}
		}
		return Point.createXYZTF( cp[0].x(), cp[0].y(), cp[0].z(), _parameter, cp[0].fuzziness() );
	}

	/**
	 * 曲線の存在範囲外の評価点を返します。
	 * @param _parameter パラメータ
	 * @return 評価点
	 */
	public Point evaluateOuter( double _parameter ) {
		return evaluate( _parameter );
	}

	/**
	 * 指定された範囲で曲線をクリップします。
	 * @param _range 範囲
	 * @return クリップされた曲線
	 * @throws OutOfRangeException 指定された範囲がパラメータ範囲に含まれていない場合
	 */
	public BezierCurve clip( Range _range ) {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of max range:%s", _range, range()));
		}
		double end = _range.length() / ( 1.0 - _range.start() );
		BezierCurve bezier = divide( _range.start() )[1];
		if ( !Double.isNaN( end ) ) {
			bezier = bezier.divide( end )[0];
		}
		return bezier;
	}

	@Override
	public BezierCurve invert() {
		// 制御点を反転する
		Point[] cp = new Point[ m_cp.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			cp[i] = m_cp[m_cp.length - i - 1];
		}

		// 定義域を反転する
		Range range = range();
		Range invertedRange = Range.create( 1.0 - range.end(), 1.0 - range.start() );

		return new BezierCurve( cp, invertedRange );
	}
	
	@Override
	public Point[] intersectionWith( Plane _plane ) {
		// 重複可能性閾値
		final double threshold = 1.0;
		// バンド幅は0
		final Range band = Range.create( 0.0, 0.0 );

		// 定義域の範囲でクリッピング
		BezierCurve bezier = clip( range() );
		Range focusRange = bezier.range();
		double endParam = focusRange.end();
		// 距離関数となるノンパラメトリックBezier曲線の構成要素を設定
		Point[] cp = bezier.m_cp;
		double[] components = new double[ cp.length ];
		double[] fuzziness = new double[ cp.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			components[i] = _plane.distance( cp[i] );
			fuzziness[i] = cp[i].fuzziness();
		}
		// 距離関数となるノンパラメトリックBezier曲線生成
		ExplicitBezierCurve explicit = ExplicitBezierCurve.create( components, fuzziness );

		Stack<ExplicitBezierCurve> stack = new Stack<ExplicitBezierCurve>();
		Point edgeIntersection = null;
		List<Point> result = new ArrayList<Point>();
		int n = 0;
		while ( n < MAX_DIVIDING_NUM ) {
			while ( n < MAX_DIVIDING_NUM ) {
				ExplicitBezierCurve nextExplicit = explicit.clip( band, threshold );
				if ( nextExplicit == null ) {
					// 交差がない場合
					if ( edgeIntersection != null ) {
						// 分割の境界で交点が存在し、その交点が前側の曲線では見つかったにも関わらず、後側の曲線では見つからなかった場合
						result.add( edgeIntersection );
						edgeIntersection = null;
					}
					break;
				} else if ( nextExplicit.range().length() < ERROR_TOLERANCE ) {
					// 1点に収束
					Range range = nextExplicit.range();
					double t = ( range.start() + range.end() ) / 2.0;
					Point intersection = bezier.evaluateAt( t );
					if ( ( focusRange.start() <= t && t < focusRange.end() )
						|| t == endParam ) {
						if ( edgeIntersection != null && t != edgeIntersection.time() ) {
							// 分割の境界で交点が存在し、その交点が前側の曲線では見つかったにも関わらず、後側の曲線では見つからなかった場合
							result.add( edgeIntersection );
							edgeIntersection = null;
						}
						result.add( intersection );
					} else if ( t == focusRange.end() ) {
						edgeIntersection = intersection;
					}
					break;
				} else {
					Range range = explicit.calcClippingRange( band, threshold );
					if ( ( range.start() < REDUCING_RATIO_THRESHOLD )
						|| ( 1.0 - range.end() ) < REDUCING_RATIO_THRESHOLD ) {
						// 交点が始点側もしくは終点側に偏っている場合
						double epsilon = 2.0E-14;
						double start = range.start() + ( 0.5 - range.start() ) * epsilon;
						double end = range.end() + ( 0.5 - range.end() ) * epsilon;
						double w = start / ( start + ( 1.0 - end ) );
						ExplicitBezierCurve[] dividedExplicits = explicit.divide( w );
						explicit = dividedExplicits[0];
						stack.push( dividedExplicits[1] );
						focusRange = explicit.range();
						++n;
					} else {
						// クリッピング
						explicit = nextExplicit;
					}
				}
			}
			if ( stack.empty() ) {
				break;
			} else {
				explicit = stack.pop();
				focusRange = explicit.range();
			}
		}

		if ( n >= MAX_DIVIDING_NUM ) {
			System.err.printf( "Warning: reached a max dviding number(%d) in intersectionWith().\r\n", MAX_DIVIDING_NUM );
		}

		return result.toArray( new Point[ result.size() ] );
	}

	@Override
	public BezierCurve part( Range _range ) {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of range:%s", _range, range() ) );
		}
		return new BezierCurve( m_cp, _range );
	}

	@Override
	public BezierCurve transform( TransformMatrix _mat ) {
		Point[] cp = new Point[ m_cp.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			cp[i] = m_cp[i].transform( _mat );
		}
		return new BezierCurve( cp, range() );
	}

	/**
	 * 次数を返します。
	 * @return 次数
	 */
	public int degree() {
		return m_cp.length - 1;
	}

	/**
	 * 制御点列を返します。
	 * @return 制御点列
	 */
	public Point[] controlPoints() {
		return m_cp.clone();
	}

	/**
	 * この BezierCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この BezierCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 制御点列がまったく同じ BezierCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final BezierCurve other = (BezierCurve) obj;
		if ( !Arrays.deepEquals( this.m_cp, other.m_cp ) ) {
			return false;
		}
		return super.equals( obj );
	}

	/**
	 * この BezierCurve のハッシュコードを返します。
	 * @return この BezierCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + super.hashCode();
		hash = 17 * hash + Arrays.deepHashCode( this.m_cp );
		return hash;
	}

	/**
	 * この BezierCurve の文字列表現を返します。
	 * @return 制御点列、パラメータ範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format( "cp:%s %s", Arrays.deepToString( m_cp ), super.toString() );
	}

	private BezierCurve( Point[] _cp, Range _range ) {
		super( _range );
		m_cp = _cp;
	}
	
	/** 制御点列 */
	private final Point[] m_cp;
	/** 収束許容誤差 */
	private static final double ERROR_TOLERANCE = 1.0E-14;
	/** 減少比率閾値 */
	private static final double REDUCING_RATIO_THRESHOLD = 0.1;
	/** 最大分割回数 */
	private static final int MAX_DIVIDING_NUM = 1000;
}
