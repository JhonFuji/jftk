package jp.sagalab.jftk.convex;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.Vector;

/**
 * ポリトープ（n次多面）を定義するクラスです。
 * @author kaneko
 */
public abstract class Polytope {
	/**
	 * 他方のポリトープとの最短距離を返します。
	 * @param _other 他方のポリトープ
	 * @return 最短距離
	 * @throws IllegalArgumentException 他方のポリトープがnullの場合
	 */
	public double distance( Polytope _other ) {
		if ( _other == null ) {
			throw new IllegalArgumentException("_other  is null");
		}
		return calcClosestVector( _other ).distance( Point.createXYZ( 0.0, 0.0, 0.0 ) );
	}
	/**
	 * 自身のポリトープ内の要素が他方のポリトープ内の
	 * 要素に含まれているかを評価します。
	 * @param _other 他方のポリトープ
	 * @return 区間真理値
	 * @throws IllegalArgumentException 他方のポリトープがnullの場合
	 */
	TruthValue includedIn( Polytope _other ) {
		if ( _other == null ) {
			throw new IllegalArgumentException("_other  is null");
		}
		// 最近接ベクトルの導出
		Point vector = calcClosestVector( _other );
		double distance = vector.distance( Point.createXYZ( 0.0, 0.0, 0.0 ) );

		double pos;
		if ( Double.isInfinite( vector.fuzziness() ) ) {
			pos = 1.0;
		} else {
			pos = Math.max( ( vector.fuzziness() - distance ) / vector.fuzziness(), 0.0 );
		}
		if ( Double.isNaN( pos ) ) {
			pos = 1.0;
		}

		return TruthValue.create( 0.0, pos );
	}

	/**
	 * サポート写像を行います。
	 * @param _vector ベクトル
	 * @return サポート写像
	 */
	protected abstract Point support( Vector _vector );

	/**
	 * GJKアルゴリズムを用いて自身と他方における最近接ベクトルを求めます。
	 * @param _other 他方のポリトープ
	 * @return 最近接ベクトル
	 * @throws IllegalArgumentException 他方のポリトープがnullの場合
	 */
	private Point calcClosestVector( Polytope _other ) {
		if ( _other == null ) {
			throw new IllegalArgumentException("_other polytope is null");
		}
		// サポートベクトルの初期設定
		Vector v = Vector.createXYZ( 1.0, 0.0, 0.0 );
		Point thisS = support( v.reverse() );
		Point otherS = _other.support( v );
		v = Vector.createSE( otherS, thisS );
		Point result = Point.createXYZTF(
			v.x(), v.y(), v.z(), Double.NaN, thisS.fuzziness() + otherS.fuzziness() );

		Point[] verticesSet = new Point[ 0 ];
		Point[] smallestSet = new Point[ 0 ];
		double smallestMax = Double.NEGATIVE_INFINITY;
		do {
			thisS = support( v.reverse() );
			otherS = _other.support( v );
			Point w = Point.createXYZTF(
				thisS.x() - otherS.x(), thisS.y() - otherS.y(), thisS.z() - otherS.z(),
				Double.NaN, thisS.fuzziness() + otherS.fuzziness() );
			double squaredV = v.dot( v );
			// 「サポート点wからサポートベクトルvが指す位置へのベクトル」と「サポートベクトルv」の内積（2つのベクトルが直交するなら，理想的にはこの値は０になる）
			double dotResult = squaredV - v.dot( Vector.createXYZ( w.x(), w.y(), w.z() ) );
			// 2つのベクトルのなす角が鈍角または直角のとき，終了。
			// ただし，dotResultはサポートベクトルvのノルムの大きさ比例する誤差を含むため，相対誤差によって収束判定。
			if ( dotResult <= 0.0 || dotResult * dotResult <= SQUARED_RELATIVE_ERROR * squaredV ) {
				return result;
			} else {
				// 頂点集合の点とwが同一である場合は明らかに収束している
				for ( Point p : verticesSet ) {
					if ( p.equals( w ) ) {
						return result;
					}
				}
			}

			// 頂点集合を更新
			verticesSet = new Point[ smallestSet.length + 1 ];
			System.arraycopy( smallestSet, 0, verticesSet, 0, smallestSet.length );
			verticesSet[smallestSet.length] = w;

			// 最小集合を更新
			smallestSet = calcSmallestSet( verticesSet );
			if ( smallestSet.length < 4 ) {
				double[] param = calcClosestParameters( smallestSet );
				double[] dvector = new double[]{ 0.0, 0.0, 0.0, 0.0 };
				for ( int i = 0; i < smallestSet.length; ++i ) {
					dvector[0] += param[i] * smallestSet[i].x();
					dvector[1] += param[i] * smallestSet[i].y();
					dvector[2] += param[i] * smallestSet[i].z();
					dvector[3] += param[i] * smallestSet[i].fuzziness();
				}
				result = Point.createXYZTF( dvector[0], dvector[1], dvector[2], Double.NaN, dvector[3] );
				v = Vector.createXYZ( result.x(), result.y(), result.z() );
				smallestMax = Double.NEGATIVE_INFINITY;
				for ( Point vertex : smallestSet ) {
					Vector vector = Vector.createXYZ( vertex.x(), vertex.y(), vertex.z() );
					double square = vector.dot( vector );
					if ( square > smallestMax ) {
						smallestMax = square;
					}
				}
			}
		} while ( smallestSet.length < 4 && v.dot( v ) > ERROR_TOLERANCE * smallestMax );

		return Point.createXYZ( 0.0, 0.0, 0.0 );
	}

	/**
	 * 原点を含むボロノイ特徴領域の頂点を取得します。
	 * @param _verticesSet 頂点集合
	 * @return ボロノイ特徴領域の頂点
	 * @throws IllegalArgumentException 頂点集合がnullの場合
	 * @throws IllegalArgumentException 頂点集合にnullが含まれている場合
	 * @throws IllegalArgumentException 頂点集合の長さが5以上の場合
	 * @throws IllegalArgumentException 頂点集合が空の場合
	 */
	private Point[] calcSmallestSet( Point[] _verticesSet ) {
		if ( _verticesSet == null ) {
			throw new IllegalArgumentException("_vertices Set is null");
		}
		if ( Arrays.asList( _verticesSet ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException("_vertices included null");
		}
		if ( _verticesSet.length > 4 ) {
			throw new IllegalArgumentException("_verticesSet is "+_verticesSet.length +" _verticesSet length less than 4");
		}
		if ( _verticesSet.length <= 0 ) {
			throw new IllegalArgumentException( "_verticesSet is empty" );
		}

		// 頂点集合からベクトルを生成
		Vector[] verticesVector = new Vector[ _verticesSet.length ];
		for ( int i = 0; i < _verticesSet.length; ++i ) {
			verticesVector[i] = Vector.createXYZ( _verticesSet[i].x(), _verticesSet[i].y(), _verticesSet[i].z() );
		}

		// 頂点領域を探索
		double[][] d = new double[ _verticesSet.length ][ _verticesSet.length ];
		for ( int i = 0; i < d.length; ++i ) {
			boolean vertice = true;
			for ( int j = 0; j < d[i].length; ++j ) {
				d[i][j] = verticesVector[i].dot( Vector.createSE( _verticesSet[j], _verticesSet[i] ) );
				if ( d[i][j] > 0 ) {
					vertice = false;
				}
			}
			if ( vertice ) {
				return new Point[]{ _verticesSet[i] };
			}
		}

		// 辺領域を探索
		for ( int i = 0; i < _verticesSet.length; ++i ) {
			for ( int j = i + 1; j < _verticesSet.length; ++j ) {
				if ( d[i][j] >= 0 && d[j][i] >= 0 ) {
					Vector v = Vector.createSE( _verticesSet[i], _verticesSet[j] );
					double squareNorm = v.dot( v );
					boolean edge = true;
					for ( int k = 0; k < _verticesSet.length; ++k ) {
						if ( k != i && k != j ) {
							double value = v.dot( Vector.createSE( _verticesSet[i], _verticesSet[k] ) ) * d[i][j] - squareNorm * d[i][k];
							if ( value < 0 ) {
								edge = false;
							}
						}
					}
					if ( edge ) {
						return new Point[]{ _verticesSet[i], _verticesSet[j] };
					}
				}
			}
		}

		// 面領域を探索
		for ( int i = 0; i < _verticesSet.length; ++i ) {
			for ( int j = i + 1; j < _verticesSet.length; ++j ) {
				Vector v = Vector.createSE( _verticesSet[i], _verticesSet[j] );
				for ( int k = j + 1; k < _verticesSet.length; ++k ) {
					Vector n = v.cross( Vector.createSE( _verticesSet[i], _verticesSet[k] ) );
					int index = ( _verticesSet.length - 1 ) * _verticesSet.length / 2 - ( i + j + k );
					if ( -verticesVector[i].dot( n ) * Vector.createSE( _verticesSet[i], _verticesSet[index] ).dot( n ) < 0 ) {
						return new Point[]{ _verticesSet[i], _verticesSet[j], _verticesSet[k] };
					}
				}
			}
		}

		return _verticesSet.clone();
	}

	/**
	 * 指定されたボロノイ特徴領域の頂点から原点との最近接点のパラメータを求めます。
	 * @param _volonoiRegion ボロノイ特徴領域の頂点
	 * @return 原点との最近接点のパラメータ
	 * @throws IllegalArgumentException 指定されたボロノイ特徴領域の頂点列がnullの場合
	 * @throws IllegalArgumentException 指定されたボロノイ特徴領域の頂点列にnullが含まれる場合
	 * @throws IllegalArgumentException 指定されたボロノイ特徴領域の頂点列が空の場合
	 * @throws UnsupportedOperationException 指定されたボロノイ特徴領域の頂点列の長さが4以上の場合
	 */
	private double[] calcClosestParameters( Point[] _volonoiRegion ) {
		if ( _volonoiRegion == null ) {
			throw new IllegalArgumentException("_volonoiRegion is null");
		}
		if ( Arrays.asList( _volonoiRegion ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException("_volonoiRegion included null");
		}
		if ( _volonoiRegion.length <= 0 ) {
			throw new IllegalArgumentException( "set is empty" );
		}
		if ( _volonoiRegion.length > 3 ) {
			throw new UnsupportedOperationException("_volonoiRegion is "+_volonoiRegion.length+" _volonoiRegion is less than 4");
		}

		double[][] matrix = new double[ _volonoiRegion.length ][ _volonoiRegion.length ];
		for ( int i = 0; i < matrix[0].length; ++i ) {
			matrix[0][i] = 1.0;
		}
		for ( int i = 1; i < matrix.length; ++i ) {
			Vector v = Vector.createSE( _volonoiRegion[0], _volonoiRegion[i] );
			for ( int j = 0; j < matrix[i].length; ++j ) {
				matrix[i][j] = v.dot( Vector.createXYZ( _volonoiRegion[j].x(), _volonoiRegion[j].y(), _volonoiRegion[j].z() ) );
			}
		}

		switch ( _volonoiRegion.length ) {
			case 1:
				return new double[]{ 1.0 };
			case 2:
				double d = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
				return new double[]{ matrix[1][1] / d, -matrix[1][0] / d };
			case 3:
				d = matrix[0][0] * ( matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1] )
					+ matrix[0][1] * ( matrix[1][2] * matrix[2][0] - matrix[1][0] * matrix[2][2] )
					+ matrix[0][2] * ( matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0] );
				return new double[]{
						( matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1] ) / d,
						-( matrix[1][0] * matrix[2][2] - matrix[1][2] * matrix[2][0] ) / d,
						( matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0] ) / d
					};
		}
		// エラー処理を行っているため、ここに処理が移ることはない。
		throw new UnsupportedOperationException();
	}

	/** 零ベクトルと見なすときの誤差許容量 */
	private static final double ERROR_TOLERANCE = 1.1102230246251565E-14;
	private static final double SQUARED_RELATIVE_ERROR = 0.1;
}