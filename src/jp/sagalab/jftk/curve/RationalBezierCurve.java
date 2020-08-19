package jp.sagalab.jftk.curve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.Vector;

/**
 * 有理ベジェ曲線を表すクラスです。
 * @author kaneko
 */
public class RationalBezierCurve extends ParametricCurve {
	
	/**
	 * 有理ベジェ曲線を生成します。
	 * @param _wcp 重み付き制御点列。
	 * @param _weights 重み列。
	 * @param _range 定義域。
	 * @return 有理ベジェ曲線
	 * @throws IllegalArgumentException 重み列がnullである場合
	 * @throws IllegalArgumentException 重み列の要素数が1未満の場合
	 * @throws IllegalArgumentException この有理ベジェ曲線が標準型ではない場合(重み列の最初または最後が1.0ではない場合)
	 */
	public static RationalBezierCurve create( Point[] _wcp, double[] _weights, Range _range ) {
		// nullでないか
		if ( _weights == null ) {
			throw new IllegalArgumentException( "_weights is null." );
		}
		// 重みの数は適切か
		if ( _weights.length < 1 ) {
			throw new IllegalArgumentException( "_weights.length < 1" );
		}
		// 標準形か
		if ( _weights[0] != 1.0 || _weights[_weights.length - 1] != 1.0 ) {
			throw new IllegalArgumentException( "not standard form." );
		}

		return RationalBezierCurve.createNonstandardForm( _wcp, _weights, _range );
	}

	/**
	 * 非標準形の有理ベジェ曲線を生成します。
	 * @param _wcp 重み付き制御点列。
	 * @param _weights 重み列。
	 * @param _range 定義域。
	 * @return 非標準形の有理ベジェ曲線。
	 * @throws IllegalArgumentException 制御点列が1点未満の場合
	 * @throws IllegalArgumentException 重み列の要素に無限大か非数が含まれている場合
	 * @throws IllegalArgumentException 定義域が存在しない場合
	 * @throws IllegalArgumentException 制御点列の数だけ重みが存在しない場合
	 * @throws OutOfRangeException パラメータ範囲が0から1の範囲に含まれていない場合
	 */
	private static RationalBezierCurve createNonstandardForm( Point[] _wcp, double[] _weights, Range _range ) {
		// 制御点列がnullでないか
		if ( _wcp == null ) {
			throw new IllegalArgumentException( "_wcp is null." );
		}
		// 制御点数は適切か
		if ( _wcp.length < 1 ) {
			throw new IllegalArgumentException( "_wcp.length < 1" );
		}
		// 重み列にinf,NaNが含まれているか
		for ( double w : _weights ) {
			if ( Double.isInfinite( w ) ) {
				throw new IllegalArgumentException( "_weights include inf." );
			}
			if ( Double.isNaN( w ) ) {
				throw new IllegalArgumentException( "_weights include NaN." );
			}
		}
		// rangeがnullでないか
		if ( _range == null ) {
			throw new IllegalArgumentException( "_range is null." );
		}
		// 定義域が[0,1]の範囲内にあるか
		if ( !Range.zeroToOne().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of max range:%s", _range, Range.zeroToOne() ) );
		}
		// 制御点の数だけ重みがあるか
		if ( _wcp.length != _weights.length ) {
			throw new IllegalArgumentException( "_wcp.length != _weights.length" );
		}

		return new RationalBezierCurve( _wcp, _weights, _range );
	}

	/**
	 * 重み付き制御点列を返します。
	 * @return 重み付き制御点列。
	 */
	public Point[] controlPoints() {
		return m_wcp.clone();
	}

	/**
	 * 重み列を返します。
	 * @return 重み列。
	 */
	public double[] weights() {
		return m_weights.clone();
	}

	/**
	 * 指定されたパラメータでこの曲線を分割します。
	 * @param _parameter パラメータ
	 * @return 分割した曲線
	 * @throws OutOfRangeException 指定されたパラメータが定義域内にない場合
	 */
	public RationalBezierCurve[] divide( double _parameter ) {
		if ( !range().isInner( _parameter ) ) {
			throw new OutOfRangeException( String.format( "_parameter:%f is out of range:%s", _parameter, range() ) );
		}
		Point[] wcp = m_wcp.clone();
		double[] weights = m_weights.clone();
		int degree = m_wcp.length - 1;

		Point[] preWCP = new Point[ m_wcp.length ];
		Point[] postWCP = new Point[ m_wcp.length ];
		double[] preWeights = new double[ m_weights.length ];
		double[] postWeights = new double[ m_weights.length ];

		for ( int i = 0; i < degree; ++i ) {
			preWCP[i] = wcp[0];
			postWCP[degree - i] = wcp[degree - i];
			preWeights[i] = weights[0];
			postWeights[degree - i] = weights[degree - i];
			int n = degree - i;
			for ( int j = 0; j < n; ++j ) {
				wcp[j] = wcp[j].internalDivision( wcp[j + 1], _parameter, 1.0 - _parameter );
				weights[j] = ( 1.0 - _parameter ) * weights[j] + _parameter * weights[j + 1];
			}
		}
		preWCP[degree] = wcp[0];
		postWCP[0] = wcp[0];
		preWeights[degree] = weights[0];
		postWeights[0] = weights[0];

		return new RationalBezierCurve[]{
				RationalBezierCurve.createNonstandardForm( preWCP, preWeights, Range.zeroToOne() ),
				RationalBezierCurve.createNonstandardForm( postWCP, postWeights, Range.zeroToOne() )
			};
	}

	/**
	 * 指定された範囲でこの曲線をクリップします。
	 * @param _range 範囲
	 * @return クリップした曲線
	 * @throws OutOfRangeException 指定された範囲がパラメータ範囲に含まれていない場合
	 */
	public RationalBezierCurve clip( Range _range ) {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of range:%s", _range, range() ) );
		}
		double end = _range.length() / ( 1.0 - _range.start() );
		RationalBezierCurve bezier = divide( _range.start() )[1];
		if ( !Double.isNaN( end ) ) {
			bezier = bezier.divide( end )[0];
		}

		return bezier;
	}

	@Override
	protected Point evaluate( double _parameter ) {
		Point[] wcp = m_wcp.clone();
		double[] weights = m_weights.clone();
		int degree = m_wcp.length - 1;

		for ( int i = 0; i < degree; ++i ) {
			int n = degree - i;
			for ( int j = 0; j < n; ++j ) {
				wcp[j] = wcp[j].internalDivision( wcp[j + 1], _parameter, 1.0 - _parameter );
				weights[j] = ( 1.0 - _parameter ) * weights[j] + _parameter * weights[j + 1];
			}
		}

		return Point.createXYZTF(
			wcp[0].x() / weights[0],
			wcp[0].y() / weights[0],
			wcp[0].z() / weights[0],
			_parameter,
			wcp[0].fuzziness() / Math.abs( weights[0] ) );
	}

	@Override
	public RationalBezierCurve invert() {
		// 重み付き制御点を反転する
		Point[] wcp = new Point[ m_wcp.length ];
		for ( int i = 0; i < wcp.length; ++i ) {
			wcp[i] = m_wcp[m_wcp.length - i - 1];
		}

		// 重み列を反転する
		double[] weights = new double[ m_weights.length ];
		for ( int i = 0; i < weights.length; ++i ) {
			weights[i] = m_weights[m_weights.length - i - 1];
		}

		// 定義域を反転する
		Range range = range();
		Range invertedRange = Range.create( 1.0 - range.end(), 1.0 - range.start() );

		return RationalBezierCurve.createNonstandardForm( wcp, weights, invertedRange );
	}

	@Override
	public Point[] intersectionWith( Plane _plane ) {
		// 重複可能性閾値
		final double threshold = 1.0;
		// バンド幅は0
		final Range band = Range.create( 0.0, 0.0 );

		// 定義域の範囲でクリッピング
		RationalBezierCurve bezier = clip( range() );
		Range focusRange = bezier.range();
		double endParam = focusRange.end();
		// 距離関数となるノンパラメトリックBezier曲線の構成要素を設定
		Point[] wcp = bezier.m_wcp;
		double[] weights = bezier.m_weights;
		double[] components = new double[ wcp.length ];
		double[] fuzziness = new double[ wcp.length ];
		Point base = _plane.base();
		Vector vector = Vector.createXYZ( base.x(), base.y(), base.z() );
		double dot = vector.dot( _plane.normal() );
		for ( int i = 0; i < wcp.length; ++i ) {
			components[i] = _plane.distance( wcp[i] ) + ( 1 - weights[i] ) * dot;
			fuzziness[i] = wcp[i].fuzziness();
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
	public RationalBezierCurve part( Range _range ) {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of range:%s", _range, range() ) );
		}

		return RationalBezierCurve.createNonstandardForm( m_wcp, m_weights, _range );
	}

	@Override
	public RationalBezierCurve transform( TransformMatrix _mat ) {
		Point[] wcp = new Point[ m_wcp.length ];

		for ( int i = 0; i < m_wcp.length; ++i ) {
			double x = m_wcp[i].x();
			double y = m_wcp[i].y();
			double z = m_wcp[i].z();
			double w = m_weights[i];
			x = _mat.get( 0, 0 ) * x + _mat.get( 0, 1 ) * y + _mat.get( 0, 2 ) * z + _mat.get( 0, 3 ) * w;
			y = _mat.get( 1, 0 ) * x + _mat.get( 1, 1 ) * y + _mat.get( 1, 2 ) * z + _mat.get( 1, 3 ) * w;
			z = _mat.get( 2, 0 ) * x + _mat.get( 2, 1 ) * y + _mat.get( 2, 2 ) * z + _mat.get( 2, 3 ) * w;
			double f = Math.abs( _mat.scalalize() * m_wcp[i].fuzziness() );
			wcp[i] = Point.createXYZTF( x, y, z, m_wcp[i].time(), f );
		}

		return createNonstandardForm( wcp, m_weights, range() );
	}
	
	/**
	 * この RationalBezierCurve と指定された Object が等しいかどうかを比較します。 
	 * @param obj この RationalBezierCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 重み付き制御点列、重み列がまったく同じ RationalBezierCurve である限りtrue
	 */
	@Override
	public boolean equals(Object obj){
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final RationalBezierCurve other = (RationalBezierCurve) obj;
		if(!Arrays.equals( m_wcp, other.m_wcp)){
			return false;
		}
		if(!Arrays.equals( m_weights, other.m_weights)){
			return false;
		}
		
		return super.equals( obj );
	}

	/**
	 * この RationalBezierCurve のハッシュコードを返します。 
	 * @return この RationalBezierCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + Arrays.deepHashCode( this.m_wcp );
		hash = 47 * hash + Arrays.hashCode( this.m_weights );
		return hash;
	}
	
	/**
	 * この RationalBezierCurve の文字列表現を返します。
	 * @return 重み付き制御点列、重み列、パラメータ範囲を表す String
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append( "wcp:[ ");
		for(Point p : m_wcp){
			builder.append( p.toString() ).append( ", " );
		}
		builder.append( "]\n");
		builder.append( "w:[ " );
		for(double w : m_weights){
			builder.append( w ).append( ", " );
		}
		builder.append( "]\n" );
		builder.append( super.toString() );
		
		return builder.toString();
	}

	private RationalBezierCurve( Point[] _wcp, double[] _weights, Range _range ) {
		super( _range );
		m_wcp = _wcp;
		m_weights = _weights;
	}
	
	/** 重み付き制御点列 */
	private final Point[] m_wcp;
	/** 重み列 */
	private final double[] m_weights;
	/** 収束許容誤差 */
	private static final double ERROR_TOLERANCE = 1.0E-14;
	/** 減少比率閾値 */
	private static final double REDUCING_RATIO_THRESHOLD = 0.1;
	/** 最大分割回数 */
	private static final int MAX_DIVIDING_NUM = 1000;
}
