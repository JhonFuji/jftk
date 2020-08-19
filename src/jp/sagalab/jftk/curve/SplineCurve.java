package jp.sagalab.jftk.curve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.TruthValue;

/**
 * スプライン曲線を表すクラスです。
 * @author Akira Nishikawa
 */
public class SplineCurve extends ParametricCurve implements CurveConvertible<BezierCurve>{

	/**
	 * スプライン曲線を生成します。
	 * @param _degree 次数
	 * @param _controlPoints 制御点列
	 * @param _knots 節点系列
	 * @param _range 存在範囲
	 * @return スプライン曲線
	 * @throws IllegalArgumentException スプライン曲線の次数が1未満の場合
	 * @throws IllegalArgumentException 存在範囲の始点が節点系列の(次数 - 1)番目よりも小さい場合、
	 *         または、存在範囲の終点が節点系列の(節点系列の要素数 - 次数)番目よりも大きい場合
	 */
	public static SplineCurve create( int _degree, Point[] _controlPoints, double[] _knots, Range _range ){
		// 次数のチェック
		if ( _degree < 1 ) {
			throw new IllegalArgumentException( "_degree < 1" );
		}
		// 存在範囲と節点系列の整合性チェック
		if ( _range.start() < _knots[ _degree - 1 ] || _knots[ _knots.length - _degree ] < _range.end() ) {
			throw new IllegalArgumentException( "There is no consistency of _range and _knots." );
		}
		// 節点系列と制御点列の整合性チェック
		if ( _knots.length != _controlPoints.length + _degree - 1 ) {
			throw new IllegalArgumentException( "_knots.length NOT equals (_controlPoints.length + _degree - 1)." );
		}
		// 節点系列のチェック
		double pre = _knots[ 0 ];
		for ( double d : _knots ) {
			if ( Double.isInfinite( d ) || Double.isNaN( d ) ) {
				throw new IllegalArgumentException( "_knots is included in infinity or NaN." );
			}
			if ( d < pre ) {
				throw new IllegalArgumentException( "There are counter flowed _knots." );
			}
			pre = d;
		}
		return new SplineCurve( _degree, _controlPoints, _knots, _range );
	}
	
	@Override
	public double length() {
		return Point.length( evaluateAllByOptimized( (int) Math.ceil( range().length() / 0.01 ), 0.001 ) );
	}

	@Override
	public Point evaluate( double _t ) {
		// 対象となる節点番号を求める
		int knotNum = searchKnotNum( _t, m_degree - 1, m_knots.length - m_degree );
		// 部分制御点列の抽出
		Point[] part = new Point[ m_degree + 1 ];
		System.arraycopy( m_cp, knotNum - m_degree, part, 0, part.length );
		// de Boor による評価
		for ( int i = 0; i < m_degree; ++i ) {
			for ( int j = 0; j < m_degree - i; ++j ) {
				int k = knotNum - j - 1;
				double w = ( _t - m_knots[ k ] ) / ( m_knots[ k + m_degree - i ] - m_knots[ k ] );
				part[ m_degree - j ] = part[ m_degree - j ].internalDivision( part[ m_degree - j - 1 ], 1 - w, w );
			}
		}
		Point p = part[ m_degree ];

		return Point.createXYZTF( p.x(), p.y(), p.z(), _t, p.fuzziness() );
	}

	@Override
	public SplineCurve part( Range _range ) throws OutOfRangeException {
		if ( !range().isInner( _range ) ) {
			throw new OutOfRangeException( String.format( "_range:%s is out of range:%s", _range, range() ) );
		}

		return new SplineCurve( m_degree, m_cp, m_knots, _range );
	}

	@Override
	public SplineCurve transform( TransformMatrix _mat ) {
		Point[] cp = new Point[ m_cp.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			cp[ i ] = m_cp[ i ].transform( _mat );
		}
		return new SplineCurve( m_degree, cp, m_knots, range() );
	}

	@Override
	public Point[] intersectionWith( Plane _plane ) {
		List<Point> result = new ArrayList<Point>();
		BezierCurve[] beziers = convert();

		Point endIntersection = null;
		for ( int i = 0; i < beziers.length - 1; ++i ) {
			Point[] intersections = beziers[i].intersectionWith( _plane );
			if ( endIntersection != null ) {
				if ( intersections.length == 0
					|| ( intersections.length > 0 && intersections[0].time() > 0.0 ) ) {
					result.add( endIntersection );
				}
				endIntersection = null;
			}
			for ( Point p : intersections ) {
				if ( p.time() < 1.0 ) {
					result.add( p );
				} else if ( p.time() == 1.0 ) {
					endIntersection = p;
				}
			}
		}
		Point[] intersections = beziers[beziers.length - 1].intersectionWith( _plane );
		if ( endIntersection != null
			&& ( intersections.length == 0
			|| intersections.length > 0 && intersections[0].time() > 0.0 ) ) {
			result.add( endIntersection );
		}
		result.addAll( Arrays.asList( intersections ) );

		return result.toArray( new Point[ result.size() ] );
	}

	/**
	 * 節点番号の探索を行います。
	 * @param _t パラメータ
	 * @param _minIndex 探索範囲の最小節点番号
	 * @param _maxIndex 探索範囲の最大節点番号
	 * @return 節点番号
	 * @throws ArrayIndexOutOfBoundsException 指定された節点番号が不正な値の場合
	 * @throws IllegalArgumentException _minIndex が _maxIndex より大きい場合
	 */
	public int searchKnotNum( double _t, int _minIndex, int _maxIndex ) {
		if ( _minIndex < 0 || _maxIndex >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException("_minIndex < 0 || _maxIndex >= m_knots.length");
		}
		if ( _minIndex > _maxIndex ) {
			throw new IllegalArgumentException( "_minIndex > _maxIndex" );
		}

		if ( m_knots[_maxIndex] <= _t ) {
			return _maxIndex;
		}
		if ( _minIndex < _maxIndex ) { // 探索区間がある場合
			// 二分探索法
			do {
				int i = ( _minIndex + _maxIndex ) / 2;
				if ( m_knots[i] <= _t && _t < m_knots[i + 1] ) {
					return i + 1;
				} else if ( _t < m_knots[i] ) {
					_maxIndex = i - 1;
				} else /* if ( _knots[i + 1] <= _t ) */ {
					_minIndex = i + 1;
				}
			} while ( _minIndex <= _maxIndex );
		}

		return _minIndex + 1;
	}

	/**
	 * 指定された範囲の中で指定された節点の多重度を求めます。
	 * @param _knot 節点
	 * @param  _minIndex 探索範囲の最小節点番号
	 * @param _maxIndex 探索範囲の最小節点番号
	 * @return 多重度
	 */
	public int calcKnotMultiplicity( double _knot, int _minIndex, int _maxIndex ) {
		int knotNum = searchKnotNum( _knot, _minIndex, _maxIndex );
		int offset = knotNum - ( ( _knot == m_knots[_maxIndex] ) ? 0 : 1 );
		int multiplicity = 0;
		while ( multiplicity <= offset && m_knots[offset - multiplicity] == _knot ) {
			 ++multiplicity;
		}
		return multiplicity;
	}

	/**
	 * このスプライン曲線を微分します。
	 * @return 微分後のスプライン曲線
	 */
	public SplineCurve differentiate() {
		if ( m_degree == 1 ) {
			throw new UnsupportedOperationException();
		}
		// 微分後の制御点列
		Point[] cp = new Point[ m_cp.length - 1 ];
		for ( int i = 0; i < m_cp.length - 1; ++i ) {
			double w = m_degree / ( m_knots[ i + m_degree ] - m_knots[ i ] );
			cp[ i ] = Point.createXYZ(
				w * ( m_cp[ i + 1 ].x() - m_cp[ i ].x() ),
				w * ( m_cp[ i + 1 ].y() - m_cp[ i ].y() ),
				w * ( m_cp[ i + 1 ].z() - m_cp[ i ].z() )
				);
		}

		// 微分後の節点系列
		double[] knots = new double[ m_knots.length - 2 ];
		System.arraycopy( m_knots, 1, knots, 0, knots.length );

		return new SplineCurve( m_degree - 1, cp, knots, range() );
	}

	/**
	 * このスプライン曲線を積分します。
	 * @param _origin 原点
	 * @return 積分後のスプライン曲線
	 */
	public SplineCurve integration( Point _origin ) {
		double[] knots = new double[ m_knots.length + 2 ];
		System.arraycopy( m_knots, 0, knots, 1, m_knots.length );
		knots[ 0 ] = knots[ 1 ] - ( knots[ 2 ] - knots[ 1 ] );
		knots[ knots.length - 1 ] = knots[ knots.length - 2 ] + ( knots[ knots.length - 2 ] - knots[ knots.length - 3 ] );

		Point[] cp = new Point[ m_cp.length + 1 ];
		cp[ 0 ] = _origin;
		for ( int i = 1; i < cp.length; ++i ) {
			double x = _origin.x();
			double y = _origin.y();
			double z = _origin.z();
			double f = _origin.fuzziness();
			for ( int j = 0; j < i; ++j ) {
				double w = ( knots[ j + m_degree + 1 ] - knots[ j ] ) / ( m_degree + 1 );
				x += m_cp[ j ].x() * w;
				y += m_cp[ j ].y() * w;
				z += m_cp[ j ].z() * w;
				f += m_cp[ j ].fuzziness() * w;
			}
			cp[ i ] = Point.createXYZTF( x, y, z, Double.NaN, f );
		}

		return new SplineCurve( m_degree + 1, cp, knots, range() );
	}

	/**
	 * このスプライン曲線を反転します。
	 * @return 反転したスプライン曲線
	 */
	@Override
	public SplineCurve invert() {
		// 制御点を反転する
		Point[] cp = new Point[ m_cp.length ];
		for ( int i = 0; i < cp.length; ++i ) {
			cp[i] = m_cp[m_cp.length - i - 1];
		}

		// 節点を反転する
		double[] knots = new double[ m_knots.length ];
		for ( int i = 0; i < knots.length; ++i ) {
			knots[i] = m_knots[m_knots.length - 1] - m_knots[m_knots.length - i - 1] + m_knots[0];
		}

		// 定義域を反転する
		Range range = range();
		Range invertedRange = Range.create( m_knots[m_knots.length - 1] - range.end() + m_knots[0],
			m_knots[m_knots.length - 1] - range.start() + m_knots[0] );

		return new SplineCurve( m_degree, cp, knots, invertedRange );
	}

	/**
	 * 指定されたパラメータで節点挿入を行います。
	 * @param _parameter パラメータ
	 * @return 節点挿入後のスプライン曲線
	 */
	public SplineCurve insertKnot( double _parameter ) {
		if ( _parameter < m_knots[ m_degree - 1 ] || m_knots[ m_knots.length - m_degree ] < _parameter ) {
			throw new OutOfRangeException();
		}
		int n = searchKnotNum( _parameter, m_degree - 1, m_knots.length - m_degree );

		// 付加節点まで含めて節点の多重度を求める
		int multipleNum = calcKnotMultiplicity( _parameter, m_degree - 1, m_knots.length - 1 );

		SplineCurve inserted = this;
		if ( multipleNum < m_degree ) {
			// 節点系列の更新
			double[] newKnots = new double[ m_knots.length + 1 ];
			System.arraycopy( m_knots, 0, newKnots, 0, n );
			newKnots[ n ] = _parameter;
			System.arraycopy( m_knots, n, newKnots, n + 1, m_knots.length - n );

			// 制御点列の更新
			Point[] cp = new Point[ m_cp.length + 1 ];
			System.arraycopy( m_cp, 0, cp, 0, n - m_degree + 1 );
			for ( int k = n - m_degree + 1; k < n + 1; ++k ) {
				double wA = newKnots[ k + m_degree ] - newKnots[ n ];
				double wB = newKnots[ n ] - newKnots[ k - 1 ];
				cp[ k ] = m_cp[ k - 1 ].internalDivision( m_cp[ k ], wB, wA );
			}
			System.arraycopy( m_cp, n, cp, n + 1, m_cp.length - n );

			inserted = new SplineCurve( m_degree, cp, newKnots, range() );
		}

		return inserted;
	}

	/**
	 * 指定された番号の節点を除去します。
	 * 基本的には挿入した節点を除去することを想定しています。
	 * 挿入した節点以外のものを除去する場合，元の曲線より一つ少ない制御点で形状を表現しようとするため，
	 * 一般に変形誤差が生じます。
	 * @param _index 除去する節点の添字
	 * @return 節点除去後の曲線
	 * @throws OutOfRangeException 曲線の存在範囲が変化してしまうような節点(付加節点や曲線の存在範囲の両端)が指定された場合
	 */
	public SplineCurve deleteKnot( int _index ) {
		if ( _index < 0 || _index >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException( _index );
		}

		int knotNum = searchKnotNum( m_knots[_index], m_degree - 1, m_knots.length - m_degree );
		int index = knotNum - ( m_knots[_index] == m_knots[m_knots.length - m_degree] ? 0 : 1 );
		int multipleNum = calcKnotMultiplicity( m_knots[_index], m_degree - 1, m_knots.length - m_degree );

		// 曲線の存在範囲が変化してしまうような節点(付加節点や曲線の存在範囲の両端)が指定された場合
		if ( ( index <= m_degree - 1 )
			|| ( index > m_knots.length - m_degree )
			|| ( index == m_knots.length - m_degree && multipleNum == 1 ) ) {
			throw new OutOfRangeException( "failure to delete knot." );
		}

		// 制御点列の更新
		Point[] cp = new Point[ m_cp.length - 1 ];
		System.arraycopy( m_cp, 0, cp, 0, index - m_degree + 1 );
		int times = ( 2 * index - m_degree - multipleNum + 1 ) / 2;
		for ( int i = index - m_degree + 1; i <= times; ++i ) {
			double denominator = m_knots[index] - m_knots[i - 1];
			double wA = m_knots[i + m_degree] - m_knots[i - 1];
			double wB = m_knots[index] - m_knots[i + m_degree];
			cp[i] = Point.createXYZTF(
				( wA * m_cp[i].x() + wB * cp[i - 1].x() ) / denominator,
				( wA * m_cp[i].y() + wB * cp[i - 1].y() ) / denominator,
				( wA * m_cp[i].z() + wB * cp[i - 1].z() ) / denominator,
				( wA * m_cp[i].time() + wB * cp[i - 1].time() ) / denominator,
				Math.max( ( wA * m_cp[i].fuzziness() + wB * cp[i - 1].fuzziness() ) / denominator, 0 ) );
		}

		System.arraycopy( m_cp, index - multipleNum + 2,
			cp, index - multipleNum + 1, m_cp.length - index + multipleNum - 2 );
		for ( int i = index - multipleNum; i >= times + 1; --i ) {
			double denominator = m_knots[i + m_degree + 1] - m_knots[index];
			double wA = m_knots[i + m_degree + 1] - m_knots[i];
			double wB = m_knots[i] - m_knots[index];
			cp[i] = Point.createXYZTF(
				( wA * m_cp[i + 1].x() + wB * cp[i + 1].x() ) / denominator,
				( wA * m_cp[i + 1].y() + wB * cp[i + 1].y() ) / denominator,
				( wA * m_cp[i + 1].z() + wB * cp[i + 1].z() ) / denominator,
				( wA * m_cp[i + 1].time() + wB * cp[i + 1].time() ) / denominator,
				Math.max( ( wA * m_cp[i + 1].fuzziness() + wB * cp[i + 1].fuzziness() ) / denominator, 0 ) );
		}

		// 節点系列の更新
		double[] knots = new double[ m_knots.length - 1 ];
		System.arraycopy( m_knots, 0, knots, 0, index );
		System.arraycopy( m_knots, index + 1, knots, index, knots.length - index );

		return new SplineCurve( m_degree, cp, knots, range() );
	}

	/**
	 * この曲線の指定された箇所と一方の曲線の指定された箇所の重複度を求めます。
	 * @param _thisIndex この曲線の箇所指定。
	 * @param _other もう一方の曲線
	 * @param _otherIndex もう一方の曲線の箇所指定。
	 * @return 指定箇所の重複度を表す真理値。
	 */
	public TruthValue overlappedWith( int _thisIndex, SplineCurve _other, int _otherIndex ) {
		if ( m_degree != _other.m_degree ) {
			throw new IllegalArgumentException();
		}
		TruthValue result = TruthValue.create( 1, 1 );

		for ( int j = 0; j <= m_degree; ++j ) {
			TruthValue tv = m_cp[_thisIndex + j - m_degree].includedIn( _other.m_cp[_otherIndex + j - m_degree] );
			result = TruthValue.create( Math.min( tv.necessity(), result.necessity() ), Math.min( tv.possibility(), result.possibility() ) );
		}

		return result;
	}

	/**
	 * 指定された番号の節点に重ねて節点挿入を行います。
	 * @param _index 節点番号
	 * @return 曲線
	 */
	public SplineCurve insertKnot( int _index ) {
		SplineCurve result = insertKnot( m_knots[_index] );
		if ( result.m_knots.length == m_knots.length ) {
			result = insertMultipleKnots( _index, 1 );
		}
		return result;
	}

	/**
	 * 次数以上の多重度をもつ節点に重ねて節点挿入を行います。
	 * @param _index 節点番号
	 * @param _num 追加節点数
	 * @return 曲線
	 */
	public SplineCurve insertMultipleKnots( int _index, int _num ) {
		if ( _index < 0 || _index >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException( _index );
		}
		int index = _index;
		while ( index > 0 && m_knots[index - 1] == m_knots[index] ) {
			--index;
		}
		if ( m_knots[index] != m_knots[index + m_degree - 1] ) {
			throw new IllegalArgumentException();
		}
		double[] newKnots = new double[ m_knots.length + _num ];
		System.arraycopy( m_knots, 0, newKnots, 0, index + 1 );
		Arrays.fill( newKnots, index + 1, index + _num + 1, m_knots[index] );
		System.arraycopy( m_knots, index + 1, newKnots, index + _num + 1, m_knots.length - index - 1 );

		Point[] newCP = new Point[ m_cp.length + _num ];
		System.arraycopy( m_cp, 0, newCP, 0, index + 1 );
		Arrays.fill( newCP, index + 1, index + _num + 1, m_cp[index] );
		System.arraycopy( m_cp, index + 1, newCP, index + _num + 1, m_cp.length - index - 1 );
		return new SplineCurve( m_degree, newCP, newKnots, range() );
	}

	/**
	 * この曲線の指定された節点から指定された値だけ時系列をシフトします。
	 * @param _shift シフト量
	 * @param _index 節点番号
	 * @return 時系列をシフトした曲線
	 * @throws IllegalArgumentException シフト量が無限大または、非数の場合
	 * @throws IllegalArgumentException シフト量が負の数でかつ、節点番号が0ではない場合
	 * @throws ArrayIndexOutOfBoundsException 節点番号が負の数であるか、節点系列の数よりも大きな値を節点番号で指定した場合
	 */
	public SplineCurve shiftTimeSeries( double _shift, int _index ) {
		if ( Double.isNaN( _shift ) ) {
			throw new IllegalArgumentException( "_shift is NaN." );
		}
		if ( Double.isInfinite( _shift ) ) {
			throw new IllegalArgumentException("_shift is Inf.");
		}
		if ( _shift < 0.0 && _index != 0 ) {
			throw new IllegalArgumentException( "_shift < 0 && _index != 0" );
		}
		if ( _index < 0 || _index >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException( _index );
		}

		double[] knots = m_knots.clone();
		for ( int i = _index; i < knots.length; ++i ) {
			knots[i] += _shift;
		}

		Range range = range();
		double start = range.start();
		double end = range.end();
		if ( m_knots[_index] <= start ) {
			start += _shift;
		}
		if ( m_knots[_index] <= end ) {
			end += _shift;
		}

		return new SplineCurve( m_degree, m_cp.clone(), knots, Range.create( start, end ) );
	}

	/**
	 * 指定された範囲の時間長を指定された時間長にします。
	 * @param _start 節点の添字
	 * @param _end 節点の添字
	 * @param _length 時間長
	 * @return 時間伸縮した曲線
	 * @throws IllegalArgumentException 時間長が無限大、NaN、負数のいずれかの場合
	 * @throws IllegalArgumentException 節点の始点と終点の添字が逆転している場合
	 * @throws ArrayIndexOutOfBoundsException _startが負数、または、節点系列の個数よりも大きい場合
	 * @throws ArrayIndexOutOfBoundsException _endが負数、または、節点系列の個数よりも大きい場合
	 * @throws IllegalArgumentException 時間長が0より大きくかつ、_startと_endが同じである場合
	 */
	public SplineCurve matchTimeLength( int _start, int _end, double _length ) {
		if ( Double.isInfinite( _length ) ) {
			throw new IllegalArgumentException( "_length is Inf." );
		}
		if ( Double.isNaN( _length ) ) {
			throw new IllegalArgumentException( "_length is NaN." );
		}
		if ( _length < 0.0 ) {
			throw new IllegalArgumentException( "_length < 0" );
		}
		if ( _start > _end ) {
			throw new IllegalArgumentException( "_start = " + _start + ", _end = " + _end + ", _start > _end" );
		}
		if ( _start < 0 || _start >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException( _start );
		}
		if ( _end < 0 || _end >= m_knots.length ) {
			throw new ArrayIndexOutOfBoundsException( _end );
		}
		if ( _length > 0.0 && m_knots[_end] == m_knots[_start] ) {
			throw new IllegalArgumentException( "_length > 0 and length of range is zero." );
		}

		// 節点列を伸縮する
		double rangeLength = m_knots[_end] - m_knots[_start];
		double[] knots = new double[ m_knots.length ];
		int i;
		for ( i = 0; i < m_knots.length && m_knots[i] <= m_knots[_start]; ++i ) {
			knots[i] = m_knots[i];
		}
		for ( ; i < m_knots.length && m_knots[i] < m_knots[_end]; ++i ) {
			knots[i] = m_knots[_start] + ( m_knots[i] - m_knots[_start] ) / rangeLength * _length;
		}
		double shift = _length - rangeLength;
		for ( ; i < m_knots.length; ++i ) {
			knots[i] = m_knots[i] + shift;
		}

		// 定義域を伸縮する
		double resultRangeStart;
		double resultRangeEnd;
		Range range = range();
		if ( range.start() <= m_knots[_start] ) {
			resultRangeStart = range.start();
		} else if ( m_knots[_start] < range.start() && range.start() < m_knots[_end] ) {
			resultRangeStart = m_knots[_start] + ( range.start() - m_knots[_start] ) / rangeLength * _length;
		} else {
			resultRangeStart = range.start() + shift;
		}
		if ( range.end() <= m_knots[_start] ) {
			resultRangeEnd = range.end();
		} else if ( m_knots[_start] < range.end() && range.end() < m_knots[_end] ) {
			resultRangeEnd = m_knots[_start] + ( range.end() - m_knots[_start] ) / rangeLength * _length;
		} else {
			resultRangeEnd = range.end() + shift;
		}

		return new SplineCurve( m_degree, m_cp.clone(), knots, Range.create( resultRangeStart, resultRangeEnd ) );
	}

	@Override
	public BezierCurve[] convert() {
		double start = range().start();
		double end = range().end();

		// 開始時刻に多重節点挿入
		SplineCurve inserted = this;
		for ( int i = 0; i < m_degree; ++i ) {
			inserted = inserted.insertKnot( start );
		}

		// 終了時刻未満に多重節点挿入
		int n = searchKnotNum( start, m_degree - 1, m_knots.length - m_degree );
		for ( double u = m_knots[ n ]; u < end; u = m_knots[ ++n ] ) {
			for ( int i = 1; i < m_degree; ++i ) {
				inserted = inserted.insertKnot( u );
			}
		}

		// 終了時刻に多重節点挿入
		for ( int i = 0; i < m_degree; ++i ) {
			inserted = inserted.insertKnot( end );
		}

		// 開始・終了の制御点番号を導出
		int startN = inserted.searchKnotNum( start, m_degree - 1, inserted.m_knots.length - 1 );
		startN -= m_degree - ( ( start == inserted.m_knots[inserted.m_knots.length - 1] ) ? 1 : 0 );
		int endN = inserted.searchKnotNum( end, m_degree - 1, inserted.m_knots.length - 1 );
		endN -= m_degree - ( ( end == inserted.m_knots[inserted.m_knots.length - 1] ) ? 1 : 0 );

		// ベジェ曲線列の構築
		Point[] cp = inserted.controlPoints();
		BezierCurve[] bezierCurves = new BezierCurve[ ( endN - startN ) / m_degree ];
		for ( int i = 0; i < bezierCurves.length; ++i ) {
			Point[] legCP = new Point[ m_degree + 1 ];
			System.arraycopy( cp, m_degree * i + startN, legCP, 0, m_degree + 1 );
			bezierCurves[ i ] = BezierCurve.create(legCP, Range.zeroToOne() );
		}

		return bezierCurves;
	}

	/**
	 * 次数を返します。
	 * @return 次数
	 */
	public int degree() {
		return m_degree;
	}

	/**
	 * 制御点列を返します。
	 * @return 制御点列
	 */
	public Point[] controlPoints() {
		return m_cp.clone();
	}

	/**
	 * 節点系列を返します。
	 * @return 節点系列
	 */
	public double[] knots() {
		return m_knots.clone();
	}

	/**
	 * この SplineCurve と指定された Object が等しいかどうかを比較します。
	 * @param obj この SplineCurve と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 次数、制御点列、節点系列、パラメータ範囲がまったく同じ SplineCurve である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final SplineCurve other = (SplineCurve) obj;
		if ( this.m_degree != other.m_degree ) {
			return false;
		}
		if ( !Arrays.deepEquals( this.m_cp, other.m_cp ) ) {
			return false;
		}
		if ( !Arrays.equals( this.m_knots, other.m_knots ) ) {
			return false;
		}
		return super.equals( obj );
	}

	/**
	 * この SplineCurve のハッシュコードを返します。
	 * @return この SplineCurve のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + super.hashCode();
		hash = 89 * hash + this.m_degree;
		hash = 89 * hash + Arrays.deepHashCode( this.m_cp );
		hash = 89 * hash + Arrays.hashCode( this.m_knots );
		return hash;
	}

	/**
	 * この SplineCurve の文字列表現を返します。
	 * @return 次数、制御点列、節点系列、パラメータ範囲を表す String
	 */
	@Override
	public String toString() {
		return String.format(
			"cp:%s knots:%s degree:%d %s", Arrays.deepToString( m_cp ),
			Arrays.toString( m_knots ), m_degree, super.toString() );
	}
	
	private SplineCurve( int _degree, Point[] _controlPoints, double[] _knots, Range _range ) {
		super( _range );

		m_degree = _degree;
		m_cp = _controlPoints;
		m_knots = _knots;
	}
	
	/** 次数 */
	private final int m_degree;
	/** 制御点列 */
	private final Point[] m_cp;
	/** 節点系列 */
	private final double[] m_knots;
}
