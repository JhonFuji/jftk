package jp.sagalab.jftk.convex;

import java.util.Iterator;
import jp.sagalab.jftk.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.Vector;

/**
 * 凸包領域を表すクラスです。
 * @author kaneko
 */
public class ConvexHull extends Polytope implements Iterable<Point[]> {
	
	/**
	 * 次元の種類を定義する識別子。
	 */
	public static enum Dimension {
		/** 0次元 */
		ZERO_DIMENSION,
		/** 1次元 */
		ONE_DIMENSION,
		/** 2次元 */
		TWO_DIMENSION,
		/** 3次元 */
		THREE_DIMENSION;
	}

	/**
	 * 凸包領域を生成します。
	 * @param _points 点列
	 * @param _dimension 次元
	 * @return 凸包領域
	 * @throws IllegalArgumentException 点列がnullの場合
	 * @throws IllegalArgumentException 点列にnullが含まれる場合
	 * @throws IllegalArgumentException 次元数がnullの場合
	 * @throws IllegalArgumentException 点列数が次元数に満たない場合
	 */
	public static ConvexHull create( Point[] _points, Dimension _dimension ) {
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null" );
		}
		if ( Arrays.asList( _points ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException( "_points included null");
		}
		if ( _dimension == null ) {
			throw new IllegalArgumentException( "_dimension is null" );
		}
		if ( _points.length <= _dimension.ordinal() ) {
			throw new IllegalArgumentException( "fail to create convex hull." );
		}
		Point[] simplicalVertices = ConvexHull.simplicialVertices( _points, _dimension );
		if ( simplicalVertices == null ) {
			switch ( _dimension ) {
				case THREE_DIMENSION:
					return ConvexHull.create( _points, Dimension.TWO_DIMENSION );
				case TWO_DIMENSION:
					return ConvexHull.create( _points, Dimension.ONE_DIMENSION );
				case ONE_DIMENSION:
					return ConvexHull.create( _points, Dimension.ZERO_DIMENSION );
			}
		}
		List<? extends Hyperplane> convexVertices = ConvexHull.createSimplex( simplicalVertices, _points, _dimension ).get( 0 ).expand();

		// 最大のファジネスを探索する
		double maxFuzziness = Double.NEGATIVE_INFINITY;
		for ( Hyperplane hplane : convexVertices ) {
			Point[] vertices = hplane.vertices();
			for ( Point p : vertices ) {
				double fuzziness = p.fuzziness();
				if ( maxFuzziness < fuzziness ) {
					maxFuzziness = fuzziness;
				}
			}
		}

		// 凸包領域の頂点のファジネスを正規化する（正規化しないとGJKアルゴリズムを用いた可能性値の算出が正確にできない）
		List<Point[]> uniformVertices = new ArrayList<Point[]>();
		for ( Hyperplane hplane : convexVertices ) {
			Point[] vertices = hplane.vertices();
			Point[] uniformed = new Point[ vertices.length ];
			for ( int i = 0; i < vertices.length; ++i ) {
				uniformed[i] = Point.createXYZTF(
					vertices[i].x(),
					vertices[i].y(),
					vertices[i].z(),
					vertices[i].time(),
					maxFuzziness );
			}
			uniformVertices.add( uniformed );
		}

		return new ConvexHull( _points, Collections.unmodifiableList( uniformVertices ), _dimension );
	}

	/**
	 * この凸包領域の次元を返します。
	 * @return 次元。
	 */
	public Dimension dimension() {
		return m_dimension;
	}

	/**
	 * この凸包領域内の点列を返します。
	 * @return 凸包領域内の点列。
	 */
	public Point[] elements() {
		return m_elements.clone();
	}

	/**
	 * この凸包領域内の要素が指定された凸包領域内の要素に含まれているかを評価します。
	 * @param _other 凸包領域
	 * @return 区間真理値
	 */
	public TruthValue includedIn( ConvexHull _other ) {
		// 凸包 vs 凸包
		TruthValue tv = super.includedIn( _other );
		double pos = tv.possibility();

		// 凸包 vs 点
		for ( Point p : m_elements ) {
			tv = _other.includedIn( p );
			pos = Math.max( tv.possibility(), pos );
		}
		for ( Point p : _other.m_elements ) {
			tv = super.includedIn( p );
			pos = Math.max( tv.possibility(), pos );
		}

		// 点 vs 点
		for ( Point tP : m_elements ) {
			for ( Point oP : _other.m_elements ) {
				tv = tP.includedIn( oP );
				pos = Math.max( tv.possibility(), pos );
			}
		}

		return TruthValue.create( 0.0, pos );
	}

	@Override
	public Iterator<Point[]> iterator() {
		return m_verticesList.iterator();
	}

	@Override
	protected Point support( Vector _vector ) {
		Point supportPoint = null;
		double max = Double.NEGATIVE_INFINITY;
		for ( Point[] vertices : m_verticesList ) {
			for ( Point vertex : vertices ) {
				double value = _vector.dot( Vector.createXYZ( vertex.x(), vertex.y(), vertex.z() ) );
				if ( value > max ) {
					max = value;
					supportPoint = vertex;
				}
			}
		}

		return supportPoint;
	}
	
	/**
	 * 点列、凸包対象の点列、次元数からn次元超平面群を生成する。
	 * @param _vertices 指定された点列
	 * @param _points 凸包対象の点列
	 * @param _dimension n次元
	 * @return n次元超平面群
	 * @throws IllegalArgumentException 0次元かつ、指定された点列の長さが1ではない場合
	 */
	private static List<? extends Hyperplane> createSimplex( final Point[] _vertices, Point[] _points, Dimension _dimension ) {
		switch ( _dimension ) {
			case ZERO_DIMENSION:
				if ( _vertices.length != 1 ) {
					throw new IllegalArgumentException( "length of vertices is not one." );
				}
				List<Hyperplane> dot = new ArrayList<Hyperplane>();
				dot.add( new Hyperplane() {
					@Override
					public List<Dot> expand() {
						List<Dot> result = new ArrayList<Dot>();
						result.add( Dot.create( _vertices[0] ) );
						return result;
					}

					@Override
					public Point[] vertices() {
						return new Point[]{ _vertices[0] };
					}

					@Override
					public Point farthest() {
						return null;
					}

					@Override
					public void assign( Point[] _candidate ) {
					}

					@Override
					public void assign( Collection<Point> _candidate ) {
					}
				} );
				return dot;
			case ONE_DIMENSION:
				return createSegment( _vertices, _points );
			case TWO_DIMENSION:
				return createTriangle( _vertices, _points );
			case THREE_DIMENSION:
				return createTetrahedron( _vertices, _points );
			default: // ここには到達しないはず
				throw new UnsupportedOperationException( "Not supported yet." );
		}
	}
	
	/**
	 * 指定された点列から2次元超平面群を生成します。
	 * @param _vertices 指定された点列
	 * @param _points 凸包対象の点列
	 * @return 2次元超平面群
	 * @throws IllegalArgumentException 指定された点列の長さが4ではない場合
	 */
	private static List<Face> createTetrahedron( Point[] _vertices, Point[] _points ) {
		if ( _vertices.length != 4 ) {
			throw new IllegalArgumentException( "length of _points is not four." );
		}

		Point[] vertices = new Point[ _vertices.length - 1 ];
		System.arraycopy( _vertices, 0, vertices, 0, vertices.length );
		Face[] faces = new Face[ 4 ];
		// 四面体の各面を生成
		for ( int i = 0; i < 2; ++i ) {
			Plane plane = Plane.create( vertices[0], vertices[1], vertices[2] );
			if ( plane.distance( _vertices[3] ) > 0 ) {
				faces[1] = Face.create(new Side[]{
						Side.create(new Dot[]{ Dot.create( vertices[2] ), Dot.create( vertices[0] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[0] ), Dot.create( _vertices[3] ) } ),
						Side.create( new Dot[]{ Dot.create( _vertices[3] ), Dot.create( vertices[2] ) } ) } );
				faces[2] = Face.create( new Side[]{
						Side.create( new Dot[]{ Dot.create( _vertices[3] ), Dot.create( vertices[1] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[1] ), Dot.create( vertices[2] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[2] ), Dot.create( _vertices[3] ) } ) } );
				faces[3] = Face.create( new Side[]{
						Side.create( new Dot[]{ Dot.create( vertices[1] ), Dot.create( _vertices[3] ) } ),
						Side.create( new Dot[]{ Dot.create( _vertices[3] ), Dot.create( vertices[0] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[0] ), Dot.create( vertices[1] ) } ) } );
			} else {
				faces[0] = Face.create( new Side[]{
						Side.create( new Dot[]{ Dot.create( vertices[0] ), Dot.create( vertices[1] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[1] ), Dot.create( vertices[2] ) } ),
						Side.create( new Dot[]{ Dot.create( vertices[2] ), Dot.create( vertices[0] ) } ) } );
			}
			// 面の向きをひっくり返す
			Point tmp = vertices[1];
			vertices[1] = vertices[2];
			vertices[2] = tmp;
		}
		List<Face> tetrahedron = Arrays.asList( faces );
		if ( tetrahedron.indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException( "fail to create tetrahedron." );
		}
		// 隣接面の情報を更新
		faces[1].m_neighbors[0] = faces[2].m_neighbors[1] = faces[3].m_neighbors[2] = faces[0];
		faces[0].m_neighbors[0] = faces[2].m_neighbors[2] = faces[3].m_neighbors[1] = faces[1];
		faces[0].m_neighbors[1] = faces[1].m_neighbors[2] = faces[3].m_neighbors[0] = faces[2];
		faces[0].m_neighbors[2] = faces[1].m_neighbors[1] = faces[2].m_neighbors[0] = faces[3];

		for ( Face face : faces ) {
			// 上側集合を割り当てる
			face.assign( _points );
		}
		return tetrahedron;
	}

	/**
	 * 指定された点列から1次元超平面群を生成します。
	 * @param _vertices 指定された点列
	 * @param _points 凸包対象の点列
	 * @return 1次元超平面群
	 * @throws IllegalArgumentException 指定された点列の長さが3ではない場合
	 */
	private static List<Side> createTriangle( Point[] _vertices, Point[] _points ) {
		if ( _vertices.length != 3 ) {
			throw new IllegalArgumentException( "length of vertices is not three." );
		}

		Vector faceNormal = Vector.createNormal( _vertices[0], _vertices[1], _vertices[2] );
		Point[] vertices = new Point[ _vertices.length - 1 ];
		System.arraycopy( _vertices, 0, vertices, 0, vertices.length );
		Side[] sides = new Side[ 3 ];
		// 三角形の辺を生成
		for ( int i = 0; i < 2; ++i ) {
			Plane plane = Plane.create( _vertices[0], Vector.createSE( vertices[0], vertices[1] ).cross( faceNormal ) );
			if ( plane.distance( _vertices[2] ) > 0 ) {
				sides[1] = new Side( new Dot[]{ Dot.create(vertices[0] ), Dot.create( _vertices[2] ) }, faceNormal );
				sides[2] = new Side( new Dot[]{ Dot.create( _vertices[2] ), Dot.create( vertices[1] ) }, faceNormal );
			} else {
				sides[0] = new Side( new Dot[]{ Dot.create( vertices[0] ), Dot.create( vertices[1] ) }, faceNormal );
			}
			// 辺の向きをひっくり返す
			Point tmp = vertices[0];
			vertices[0] = vertices[1];
			vertices[1] = tmp;
		}

		List<Side> triangle = Arrays.asList( sides );
		if ( triangle.indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException( "fail to create triangle." );
		}

		// 隣接情報を更新
		sides[1].m_neighbors[0] = sides[2].m_neighbors[1] = sides[0];
		sides[0].m_neighbors[1] = sides[2].m_neighbors[0] = sides[1];
		sides[0].m_neighbors[0] = sides[1].m_neighbors[1] = sides[2];

		for ( Side side : sides ) {
			// 上側集合を割り当てる
			side.assign( _points );
		}

		return triangle;
	}

	/**
	 * 指定された点列から0次元超平面群を生成します。
	 * @param _vertices 指定された点列
	 * @param _points 凸包対象の点列
	 * @return 0次元超平面群
	 * @throws IllegalArgumentException 指定された点列の長さが2ではない場合
	 */
	private static List<Dot> createSegment( Point[] _vertices, Point[] _points ) {
		if ( _vertices.length != 2 ) {
			throw new IllegalArgumentException( "length of vertices is not two." );
		}
		Dot[] dots = new Dot[]{
			new Dot( _vertices[0], Vector.createSE( _vertices[1], _vertices[0] ) ),
			new Dot( _vertices[1], Vector.createSE( _vertices[0], _vertices[1] ) )
		};
		dots[0].m_neighbor = dots[1];
		dots[1].m_neighbor = dots[0];
		for ( Dot dot : dots ) {
			dot.assign( _points );
		}

		return Arrays.asList( dots );
	}

	/**
	 * 各次元空間の単体の頂点列を返します。
	 * @param _points 空間上の点列
	 * @param _dimension 次元
	 * @return 単体の頂点列
	 */
	private static Point[] simplicialVertices( Point[] _points, Dimension _dimension ) {
		Point[] simplicialVertices = new Point[ _dimension.ordinal() + 1 ];
		Vector zero = Vector.createXYZ( 0.0, 0.0, 0.0 );
		if ( _dimension == Dimension.ZERO_DIMENSION ) {
			simplicialVertices[0] = _points[0];
			return simplicialVertices;
		}
		for ( int h = 0; h < _points.length; ++h ) {
			simplicialVertices[0] = _points[h];
			for ( int i = h + 1; i < _points.length; ++i ) {
				if ( simplicialVertices[0].x() != _points[i].x()
					|| simplicialVertices[0].y() != _points[i].y()
					|| simplicialVertices[0].z() != _points[i].z() ) { // 位置が異なる2点を選出
					simplicialVertices[1] = _points[i];
					for ( int j = i + 1; j < _points.length; ++j ) { // 選出した2点と位置が等しくそれらよりもファジネスの大きい点を探索
						if ( simplicialVertices[0].x() == _points[j].x()
							&& simplicialVertices[0].y() == _points[j].y()
							&& simplicialVertices[0].z() == _points[j].z()
							&& simplicialVertices[0].fuzziness() < _points[j].fuzziness() ) {
							simplicialVertices[0] = _points[j];
						} else if ( simplicialVertices[1].x() == _points[j].x()
							&& simplicialVertices[1].y() == _points[j].y()
							&& simplicialVertices[1].z() == _points[j].z()
							&& simplicialVertices[1].fuzziness() < _points[j].fuzziness() ) {
							simplicialVertices[1] = _points[j];
						}
					}
					if ( _dimension == Dimension.ONE_DIMENSION ) {
						return simplicialVertices;
					}
					Vector v = Vector.createSE( simplicialVertices[0], simplicialVertices[1] );
					for ( int j = i + 1; j < _points.length; ++j ) {
						Vector normal = Vector.createNormal( simplicialVertices[0], simplicialVertices[1], _points[j] );
						if ( !normal.equals( zero ) ) {
							Plane plane = Plane.create( simplicialVertices[0], normal.cross( v ) );
							if ( plane.distance( _points[j] ) != 0 ) {
								simplicialVertices[2] = _points[j];
								for ( int k = j + 1; k < _points.length; ++k ) {
									if ( simplicialVertices[2].x() == _points[k].x()
										&& simplicialVertices[2].y() == _points[k].y()
										&& simplicialVertices[2].z() == _points[k].z()
										&& simplicialVertices[2].fuzziness() < _points[k].fuzziness() ) {
										simplicialVertices[2] = _points[k];
									}
								}
								if ( _dimension == Dimension.TWO_DIMENSION ) {
									return simplicialVertices;
								}
								plane = Plane.create( simplicialVertices[0], normal );
								for ( int k = j + 1; k < _points.length; ++k ) {
									if ( plane.distance( _points[k] ) != 0 ) {
										simplicialVertices[3] = _points[k];
										for ( int m = k + 1; m < _points.length; ++m ) {
											if ( simplicialVertices[3].x() == _points[m].x()
												&& simplicialVertices[3].y() == _points[m].y()
												&& simplicialVertices[3].z() == _points[m].z()
												&& simplicialVertices[3].fuzziness() < _points[m].fuzziness() ) {
												simplicialVertices[3] = _points[m];
											}
										}
										if ( _dimension == Dimension.THREE_DIMENSION ) {
											return simplicialVertices;
										}
									}
								}
							}
						}
					}
				} else { // 位置が等しい2点を見つけたとき、ファジネスの大きい方を選出
					if ( simplicialVertices[0].fuzziness() < _points[i].fuzziness() ) {
						simplicialVertices[0] = _points[i];
					}
				}
			}
		}
		return null;
	}

	private ConvexHull( Point[] _elements, List<Point[]> _vertices, Dimension _dimension ) {
		m_elements = _elements;
		m_verticesList = _vertices;
		m_dimension = _dimension;
	}
	
	/** 次元 */
	private final Dimension m_dimension;
	/** 凸包領域内の点列 */
	private final Point[] m_elements;
	/** 凸包領域の頂点列 */
	private final List<Point[]> m_verticesList;
}