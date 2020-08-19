package jp.sagalab.jftk.curve;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.convex.ConvexHull;

/**
 * ノンパラメトリックベジェ曲線を表すクラスです。
 * @author kaneko
 */
final class ExplicitBezierCurve {
	
	/**
	 * ノンパラメトリックベジェ曲線を生成します。
	 * @param _components 軸成分
	 * @param _fuzziness 各軸成分の曖昧さの大きさ
	 * @return ノンパラメトリックベジェ曲線
	 */
	static ExplicitBezierCurve create( double[] _components, double[] _fuzziness ) {
		return ExplicitBezierCurve.create( _components, _fuzziness, 0.0, 1.0 );
	}

	/**
	 * ノンパラメトリックベジェ曲線を生成します。
	 * @param _components 軸成分
	 * @param _fuzziness 各軸成分の曖昧さの大きさ
	 * @param _range 定義域
	 * @return ノンパラメトリックベジェ曲線
	 */
	private static ExplicitBezierCurve create( double[] _components, double[] _fuzziness, Range _range ) {
		return ExplicitBezierCurve.create( _components, _fuzziness, _range.start(), _range.length() );
	}

	/**
	 * ノンパラメトリックベジェ曲線を生成します。
	 * @param _components 軸成分
	 * @param _fuzziness 各軸成分の曖昧さの大きさ
	 * @param _start 定義域の最小値
	 * @param _length 定義域の長さ
	 * @return ノンパラメトリックベジェ曲線
	 */
	private static ExplicitBezierCurve create( double[] _components, double[] _fuzziness, double _start, double _length ) {
		if ( _components == null ) {
			throw new IllegalArgumentException( "_components is null." );
		}
		if ( _fuzziness == null ) {
			throw new IllegalArgumentException( "_fuzziness is null." );
		}
		if ( Double.isNaN( _start ) ) {
			throw new IllegalArgumentException( "_start is NaN." );
		}
		if ( Double.isNaN( _length ) ) {
			throw new IllegalArgumentException( "_length is NaN." );
		}
		if ( _length < 0.0 || _length > 1.0 ) {
			throw new IllegalArgumentException( "_length < 0.0 || _length > 1.0" );
		}
		if ( !Range.zeroToOne().isInner( _start ) ) {
			throw new OutOfRangeException( String.format( "_d:%f is out of max range:%s", _start, Range.zeroToOne() ) );
		}
		if ( _components.length != _fuzziness.length ) {
			throw new IllegalArgumentException( "_components length is not equal to _fuzziness length." );
		}
		Point[] cp = new Point[ _components.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			if ( Double.isNaN( _components[i] ) ) {
				throw new IllegalArgumentException( "_components include NaN." );
			}
			if ( Double.isInfinite( _components[i] ) ) {
				throw new IllegalArgumentException( "_components include inf." );
			}
			if ( Double.isNaN( _fuzziness[i] ) ) {
				throw new IllegalArgumentException( "_fuzziness include NaN." );
			}
			if ( Double.isInfinite( _fuzziness[i] ) ) {
				throw new IllegalArgumentException( "_fuzziness include inf." );
			}
			cp[i] = Point.createXYZTF( i, _components[i], 0.0, Double.NaN, _fuzziness[i] );
		}

		return new ExplicitBezierCurve( BezierCurve.create(cp, Range.zeroToOne() ), _start, _length );
	}

	/**
	 * 定義域を返します。
	 * @return 定義域
	 */
	Range range() {
		return Range.create( m_start, Math.min( m_start + m_length, 1.0 ) );
	}

	/**
	 * 指定されたパラメータでこの曲線を分割します。
	 * @param _parameter [0,1]の範囲のパラメータ
	 * @return 分割後の曲線
	 * @throws OutOfRangeException [0,1]の範囲のパラメータが範囲に含まれていない場合
	 */
	ExplicitBezierCurve[] divide( double _parameter ) {
		if ( !m_bezier.range().isInner( _parameter ) ) {
			throw new OutOfRangeException( String.format( "_parameter:%f is out of range:%s", _parameter, range() ) );
		}
		Range range = range();
		double bound = internalDivision( range.start(), range.end(), _parameter );
		Range[] ranges = new Range[]{
			Range.create( range.start(), bound ),
			Range.create( bound, range.end() )
		};
		BezierCurve[] beziers = m_bezier.divide( _parameter );
		ExplicitBezierCurve[] explicits = new ExplicitBezierCurve[ beziers.length ];
		for ( int i = 0; i < beziers.length; ++i ) {
			Point[] cp = beziers[i].controlPoints();
			double[] components = new double[ cp.length ];
			double[] fuzziness = new double[ cp.length ];
			for ( int j = 0; j < cp.length; ++j ) {
				components[j] = cp[j].y();
				fuzziness[j] = cp[j].fuzziness();
			}
			explicits[i] = ExplicitBezierCurve.create( components, fuzziness, ranges[i] );
		}

		return explicits;
	}

	/**
	 * 指定されたバンドでこの曲線をクリップします。
	 * @param _band バンド
	 * @param _threshold 重複可能性閾値
	 * @return クリップされた曲線
	 */
	ExplicitBezierCurve clip( Range _band, double _threshold ) {
		// クリッピング範囲を算出
		Range clippingRange = calcClippingRange( _band, _threshold );
		if ( clippingRange == null ) {
			// バンドと交差する部分がない場合
			return null;
		}
		// クリッピング
		double end = clippingRange.length() / ( 1.0 - clippingRange.start() );
		BezierCurve clipped = m_bezier.divide( clippingRange.start() )[1];
		if ( !Double.isNaN( end ) ) {
			clipped = clipped.divide( end )[0];
		}

		// クリッピング後の制御点列と曖昧さを取得
		Point[] cp = clipped.controlPoints();
		double[] components = new double[ cp.length ];
		double[] fuzziness = new double[ cp.length ];
		for ( int i = 0; i < components.length; ++i ) {
			components[i] = cp[i].y();
			fuzziness[i] = cp[i].fuzziness();
		}

		// クリッピング後の定義域を取得
		Range range = range();
		double start = internalDivision( range.start(), range.end(), clippingRange.start() );
		double length = m_length * clippingRange.length();

		return ExplicitBezierCurve.create( components, fuzziness, start, length );
	}

	/**
	 * クリップする範囲を[0,1]の範囲で求めます。
	 * @param _band バンド
	 * @param _threshold 重複可能性閾値
	 * @return クリップする範囲
	 * @throws 重複可能性閾値がNaNの場合
	 * @throws 重複可能性閾値が0.0より下、または1.0より上の場合
	 */
	Range calcClippingRange( Range _band, double _threshold ) {
		if ( _band == null ) {
			throw new NullPointerException( "_band is null." );
		}
		if ( Double.isNaN( _threshold ) ) {
			throw new IllegalArgumentException( "_threshold is NaN." );
		}
		if ( _threshold < 0.0 || _threshold > 1.0 ) {
			throw new IllegalArgumentException( "_threshold < 0.0 || _threshold > 1.0" );
		}
		if ( !isAbleToClip( _band, _threshold ) ) {
			// バンドと交差する部分がない場合
			return null;
		}

		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		Point[] cp = m_bezier.controlPoints();
		if ( calcBandPossibility( cp[0], _band ) >= _threshold ) {
			// 始点がバンド内にある場合
			min = cp[0].x();
		}
		if ( calcBandPossibility( cp[cp.length - 1], _band ) >= _threshold ) {
			// 終点がバンド内にある場合
			max = cp[cp.length - 1].x();
		}
		double[] bandLines = { _band.start(), _band.end() };

		// 凸包領域の要素を生成
		Point[] convexElements = new Point[ 2 * cp.length ];
		// 凸包領域探索の初期状態の頂点は先頭の要素から優先して選ばれるので，
		// BezierClippingで特に重要視すべき始点と終点を要素の先頭にして格納する
		for ( int i = 0; i < cp.length; ++i ) {
			int j = ( i % 2 == 0 ) ? i / 2 : cp.length - i / 2 - 1;
			convexElements[2 * i] = Point.createXYZ( cp[j].x(), cp[j].y() - cp[j].fuzziness() * ( 1.0 - _threshold ), 0.0 );
			convexElements[2 * i + 1] = Point.createXYZ( cp[j].x(), cp[j].y() + cp[j].fuzziness() * ( 1.0 - _threshold ), 0.0 );
		}
		// 凸包領域を求める
		ConvexHull convex = ConvexHull.create( convexElements, ConvexHull.Dimension.TWO_DIMENSION );
		if ( isDegeneracy( convex ) ) {
			// 凸包領域が縮退した場合
			for ( double bandLine : bandLines ) {
				for ( int i = 1; i < cp.length; ++i ) {
					Range range = calcLineCrossRange( cp[i - 1], cp[i], bandLine );
					if ( range != null ) {
						min = Math.min( range.start(), min );
						max = Math.max( range.end(), max );
					}
				}
			}
		} else {
			for ( double bandLine : bandLines ) {
				for ( Point[] vertices : convex ) {
					Point pre = vertices[0];
					Point post = vertices[1];
					if ( vertices[0].x() > vertices[1].x() ) {
						pre = vertices[1];
						post = vertices[0];
					}
					Range range = calcLineCrossRange( pre, post, bandLine );
					if ( range != null ) {
						min = Math.min( range.start(), min );
						max = Math.max( range.end(), max );
					}
				}
			}
		}

		Range result = null;
		if ( min <= max ) {
			min = ( min - cp[0].x() ) / ( cp[cp.length - 1].x() - cp[0].x() );
			max = ( max - cp[0].x() ) / ( cp[cp.length - 1].x() - cp[0].x() );
			result = Range.create( min, max );
		}

		return result;
	}

	/**
	 * 指定された点とバンドの重複可能性値を求めます。
	 * @param _point 点
	 * @param _band バンド
	 * @return 重複可能性値
	 */
	private static double calcBandPossibility( Point _point, Range _band ) {
		double bandCenterLine = ( _band.start() + _band.end() ) / 2.0;
		double fuzziness = _point.fuzziness();
		double pos = 1.0 - Math.max( Math.abs( _point.y() - bandCenterLine ) - _band.length() / 2.0, 0 ) / fuzziness;

		if ( Double.isNaN( pos ) ) {
			if ( _band.isInner( _point.y() ) ) {
				pos = 1.0;
			} else {
				pos = 0.0;
			}
		}
		pos = Math.max( pos, 0 );

		return pos;
	}

	/**
	 * 指定された二点を結ぶ線分とバンドラインとの交点が存在する範囲を求めます。
	 * @param _pre 始点
	 * @param _post 終点
	 * @param _bandLine バンドライン
	 * @return 交点の存在範囲
	 */
	private static Range calcLineCrossRange( Point _pre, Point _post, double _bandLine ) {
		if ( _pre.x() > _post.x() ) {
			throw new IllegalArgumentException( "_pre.x > _post.x" );
		}
		double param = ( _bandLine - _pre.y() ) / ( _post.y() - _pre.y() );
		Range result = null;
		if ( 0.0 <= param && param <= 1.0 ) {
			double t;
			if ( _pre.x() == _post.x() ) {
				t = _pre.x();
			} else {
				t = internalDivision( _pre.x(), _post.x(), param );
			}
			result = Range.create( t, t );
		} else if ( Double.isNaN( param ) ) {
			result = Range.create( _pre.x(), _post.x() );
		}

		return result;
	}

	/**
	 * 指定された二つの値を_ratio:(1-_ratio)で内分します。
	 * @param _pre 内分される値
	 * @param _post 内分される値
	 * @param _ratio 内分比
	 * @return 内分値
	 */
	private static double internalDivision( double _pre, double _post, double _ratio ) {
		if ( !Range.zeroToOne().isInner( _ratio ) ) {
			throw new OutOfRangeException( String.format( "_ratio:%f is out of range:%s", _ratio, Range.zeroToOne() ) );
		}
		double result = ( 1.0 - _ratio ) * _pre + _ratio * _post;
		return result;
	}
	
	/**
	 * 縮退したかを返します。
	 * @param _convex 凸包領域
	 * @return 縮退したならtrue
	 */
	private boolean isDegeneracy( ConvexHull _convex ) {
		boolean result;
		switch ( _convex.dimension() ) {
			case ONE_DIMENSION:
				result = true;
				break;
			case TWO_DIMENSION:
				Point[] cp = m_bezier.controlPoints();
				int degree = cp.length - 1;
				boolean startFlag = false;
				boolean endFlag = false;
				result = true;
				for ( Point[] vertices : _convex ) {
					for ( Point p : vertices ) {
						if ( p.x() == 0.0 ) {
							startFlag = true;
						} else if ( p.x() == degree ) {
							endFlag = true;
						}
					}
					// 始終点が凸包領域の頂点になっているか
					if ( startFlag && endFlag ) {
						result = false;
						break;
					}
				}
				break;
			default:
				throw new UnsupportedOperationException( "Not supported yet." );
		}

		return result;
	}
	
	/**
	 * クリッピング可能かどうかを返します。
	 * @param _band バンド
	 * @param _threshold 重複可能性閾値
	 * @return クリッピング可能ならtrue
	 */
	private boolean isAbleToClip( Range _band, double _threshold ) {
		boolean result = false;
		Point[] cp = m_bezier.controlPoints();
		double startBottom = cp[0].y() - cp[0].fuzziness() * ( 1.0 - _threshold );
		double startTop = cp[0].y() + cp[0].fuzziness() * ( 1.0 - _threshold );
		double endBottom = cp[cp.length - 1].y() - cp[cp.length - 1].fuzziness() * ( 1.0 - _threshold );
		double endTop = cp[cp.length - 1].y() + cp[cp.length - 1].fuzziness() * ( 1.0 - _threshold );

		if ( ( startBottom <= _band.end() && endTop >= _band.start() )
			|| ( startTop >= _band.start() && endBottom <= _band.end() ) ) {
			result = true;
		} else {
			if ( ( startBottom > _band.end() && endBottom > _band.end() ) ) {
				int n = cp.length - 1;
				for ( int i = 1; i < n; ++i ) {
					double bottom = cp[i].y() - cp[i].fuzziness() * ( 1.0 - _threshold );
					if ( bottom < _band.end() ) {
						result = true;
						break;
					}
				}
			} else /* if ( endTop < _band.start() && startTop < _band.start() ) */ {
				int n = cp.length - 1;
				for ( int i = 1; i < n; ++i ) {
					double top = cp[i].y() + cp[i].fuzziness() * ( 1.0 - _threshold );
					if ( top > _band.start() ) {
						result = true;
						break;
					}
				}
			}
		}

		return result;
	}
	
	/**
	 * この ExplicBezierCurve と指定された Object が等しいかどうかを比較します。 
	 * @param obj この ExplicBezierCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * ベジェ曲線、定義域の最小値、定義域の長さがまったく同じ ExplicBezierCurve である限りtrue
	 */
	@Override
	public boolean equals(Object obj){
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final ExplicitBezierCurve other = (ExplicitBezierCurve) obj;
		if ( !m_bezier.equals( other.m_bezier ) ) {
			return false;
		}
		if ( m_start != other.m_start ) {
			return false;
		}

		return m_length == other.m_length;
	}

	/**
	 * この ExplicBezierCurve のハッシュコードを返します。 
	 * @return この ExplicBezierCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + ( this.m_bezier != null ? this.m_bezier.hashCode() : 0 );
		hash = 47 * hash + (int) ( Double.doubleToLongBits( this.m_start ) ^ ( Double.doubleToLongBits( this.m_start ) >>> 32 ) );
		hash = 47 * hash + (int) ( Double.doubleToLongBits( this.m_length ) ^ ( Double.doubleToLongBits( this.m_length ) >>> 32 ) );
		return hash;
	}
	
	/**
	 * この ExplicBezierCurve の文字列表現を返します。
	 * @return ベジェ曲線、定義域の最小値、定義域の長さを表す String
	 */
	@Override
	public String toString() {
		return String.format( "%s start:%.3f length:%.3f", m_bezier.toString(), m_start, m_length );
	}

	private ExplicitBezierCurve( BezierCurve _bezier, double _start, double _length ) {
		m_bezier = _bezier;
		m_start = _start;
		m_length = _length;
	}
	
	/** Bezier曲線 */
	private final BezierCurve m_bezier;
	/** 定義域の最小値 */
	private final double m_start;
	/** 定義域の長さ */
	private final double m_length;
}
