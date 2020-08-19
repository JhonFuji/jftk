package jp.sagalab.jftk.convex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Vector;

/**
 * 2次元超平面(面)を表すクラスです
 * @author kaneko
 */
class Face implements Hyperplane {
	
	/**
	 * 指定された1次元超平面群(線分)から2次元超平面を生成します。
	 * @param _sides 指定された1次元超平面群
	 * @return 2次元超平面
	 * @throws IllegalArgumentException 一次元超平面群がnullの場合、もしくはnullを含む場合
	 */	
	public static Face create( Side[] _sides ){
		if ( _sides == null ) {
			throw new IllegalArgumentException( "_side is null" );
		}
		if ( Arrays.asList( _sides ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_side include null" );
		}
		return new Face(_sides);
	}
	
	/**
	 * 指定された最も遠い点から輪郭を生成する。
	 * @param _farthest 指定された最も遠い点
	 * @return 自身の輪郭
	 */
	public Silhouette<Face> silhouette( Point _farthest ) {
		if ( m_plane.distance( _farthest ) <= 0 ) {
			return new Silhouette<Face>( Collections.<Silhouette.SilhouetteEdge<Face>>emptyList(), Collections.<Point>emptyList() );
		}

		List<Silhouette.SilhouetteEdge<Face>> silhouetteEdges = new ArrayList<Silhouette.SilhouetteEdge<Face>>();
		Collection<Point> uppers = new HashSet<Point>();
		Set<Face> obsolate = new HashSet<Face>();
		Stack<Face> fStack = new Stack<Face>();
		Stack<Integer> iStack = new Stack<Integer>();
		Face current = this;
		Face prev = current;
		int index = current.m_sides.length - 1;
		fStack.push( current );
		iStack.push( index );
		while ( true ) {
			if ( !obsolate.contains( current ) ) { // 未探索
				if ( current.m_plane.distance( _farthest ) > 0 ) { // visible
					obsolate.add( current );
					uppers.addAll( current.m_uppers );
					fStack.push( current );
					for ( int i = 0; i < current.m_sides.length; ++i ) {
						int j = ( index + i ) % current.m_sides.length;
						if ( current.m_sides[j].m_points[0].m_dot == prev.m_sides[index].m_points[1].m_dot
							&& current.m_sides[j].m_points[1].m_dot == prev.m_sides[index].m_points[0].m_dot ) {
							index = j;
							break;
						}
					}
					iStack.push( ( index + 2 ) % current.m_sides.length );
					index = ( index + 1 ) % current.m_sides.length;
					prev = current;
					current = prev.m_neighbors[index];
					continue;
				} else { // invisible
					for ( int i = 0; i < current.m_sides.length; ++i ) {
						int j = ( index + i ) % current.m_sides.length;
						if ( current.m_sides[j].m_points[0].m_dot == prev.m_sides[index].m_points[1].m_dot
							&& current.m_sides[j].m_points[1].m_dot == prev.m_sides[index].m_points[0].m_dot ) {
							silhouetteEdges.add( new Silhouette.SilhouetteEdge<Face>( current, j ) );
							break;
						}
					}
				}
			}
			if ( fStack.empty() ) {
				break;
			} else {
				prev = fStack.pop();
				index = iStack.pop();
				current = prev.m_neighbors[index];
			}
		}
		for ( Silhouette.SilhouetteEdge<Face> edge : silhouetteEdges ) {
			uppers.addAll( edge.m_hyperplane.m_uppers );
		}

		return new Silhouette<Face>( silhouetteEdges, uppers );
	}

	@Override
	public List<Face> expand() {
		List<Face> result = new ArrayList<Face>();
		Set<Face> obsolate = new HashSet<Face>();
		Stack<Face> fStack = new Stack<Face>();
		Stack<Integer> iStack = new Stack<Integer>();
		Face current = this;
		while ( true ) {
			if ( !obsolate.contains(current ) ) { // 未探索
				Point farthest = current.farthest();
				while ( farthest != null ) {
					Dot farthestDot = Dot.create( farthest );
					Silhouette<Face> silhouette = current.silhouette( farthest );
					Face[] newFaces = new Face[ silhouette.m_silhouetteEdges.size() ];
					int i = 0;
					for ( Silhouette.SilhouetteEdge<Face> edge : silhouette.m_silhouetteEdges ) {
						Face face = edge.m_hyperplane;
						int index = edge.m_index;
						// 新しい面を生成
						Side[] sides = new Side[ 3 ];
						sides[index] = Side.create( new Dot[]{ face.m_sides[index].m_points[1], face.m_sides[index].m_points[0] } );
						sides[( index + 1 ) % sides.length] = Side.create( new Dot[]{ face.m_sides[index].m_points[0], farthestDot } );
						sides[( index + 2 ) % sides.length] = Side.create( new Dot[]{ farthestDot, face.m_sides[index].m_points[1] } );
						Face newFace = new Face( sides );
						// 新しい面の上側集合を割り当てる
						newFace.assign( silhouette.m_uppers );
						// 隣接情報を更新
						face.m_neighbors[index] = newFace;
						newFace.m_neighbors[index] = face;
						newFaces[i++] = newFace;
					}
					// 新しい面同士を繋ぐように隣接情報を更新
					for ( i = 0; i < newFaces.length; ++i ) {
						int neightborIndex0 = silhouette.m_silhouetteEdges.get( i ).m_index;
						for ( int j = i + 1; j < newFaces.length; ++j ) {
							int neighborIndex1 = silhouette.m_silhouetteEdges.get( j ).m_index;
							CONNECT_LOOP:
							for ( int k = 1; k < newFaces[i].m_sides.length; ++k ) {
								int index0 = ( neightborIndex0 + k ) % newFaces[i].m_sides.length;
								for ( int h = 1; h < newFaces[j].m_sides.length; ++h ) {
									int index1 = ( neighborIndex1 + h ) % newFaces[j].m_sides.length;
									if ( newFaces[i].m_sides[index0].m_points[0].m_dot == newFaces[j].m_sides[index1].m_points[1].m_dot
										&& newFaces[i].m_sides[index0].m_points[1].m_dot == newFaces[j].m_sides[index1].m_points[0].m_dot ) {
										newFaces[i].m_neighbors[index0] = newFaces[j];
										newFaces[j].m_neighbors[index1] = newFaces[i];
										break CONNECT_LOOP;
									}
								}
							}
						}
					}
					current = newFaces[0];
					farthest = current.farthest();
				}
				obsolate.add( current );
				result.add( current );
				for ( int i = 2; i > 0; --i ) {
					fStack.push( current );
					iStack.push( i );
				}
				current = current.m_neighbors[0];
			} else { // 探索済み
				if ( fStack.empty() ) { // スタックが空の場合，探索終了
					break;
				} else { // スタックからポップする
					Face face = fStack.pop();
					int i = iStack.pop();
					current = face.m_neighbors[i];
				}
			}
		}
		return result;
	}

	@Override
	public Point[] vertices() {
		return new Point[]{
				m_sides[0].m_points[0].m_dot,
				m_sides[1].m_points[0].m_dot,
				m_sides[2].m_points[0].m_dot };
	}

	@Override
	public Point farthest() {
		Point farthest = null;
		double maxDist = Double.NEGATIVE_INFINITY;
		for ( Point p : m_uppers ) {
			double dist = m_plane.distance( p );
			if ( maxDist < dist ) {
				maxDist = dist;
				farthest = p;
			} else if ( maxDist == dist && farthest.fuzziness() < p.fuzziness() ) {
				farthest = p;
			}
		}
		return farthest;
	}

	@Override
	public void assign( Point[] _candidate ) {
		for ( Point point : _candidate ) {
			double length0 = Vector.createNormal( m_sides[0].m_points[1].m_dot, m_sides[0].m_points[0].m_dot, point ).length();
			double length1 = Vector.createNormal( point, m_sides[1].m_points[1].m_dot, m_sides[1].m_points[0].m_dot ).length();
			double length2 = Vector.createNormal( m_sides[2].m_points[0].m_dot, point, m_sides[2].m_points[1].m_dot ).length();
			if ( !Double.isInfinite( 1.0 / length0 )
				&& !Double.isInfinite( 1.0 / length1 )
				&& !Double.isInfinite( 1.0 / length2 )
				&& m_plane.distance( point ) > 0 ) {
				m_uppers.add( point );
			}
		}
	}

	@Override
	public void assign( Collection<Point> _candidate ) {
		for ( Point point : _candidate ) {
			double length0 = Vector.createNormal( m_sides[0].m_points[1].m_dot, m_sides[0].m_points[0].m_dot, point ).length();
			double length1 = Vector.createNormal( point, m_sides[1].m_points[1].m_dot, m_sides[1].m_points[0].m_dot ).length();
			double length2 = Vector.createNormal( m_sides[2].m_points[0].m_dot, point, m_sides[2].m_points[1].m_dot ).length();
			if ( !Double.isInfinite( 1.0 / length0 )
				&& !Double.isInfinite( 1.0 / length1 )
				&& !Double.isInfinite( 1.0 / length2 )
				&& m_plane.distance( point ) > 0 ) {
				m_uppers.add( point );
			}
		}
	}

	private Face( Side[] _sides ) {
		System.arraycopy( _sides, 0, m_sides, 0, m_sides.length );
		m_plane = Plane.create(
			m_sides[0].m_points[0].m_dot,
			m_sides[1].m_points[0].m_dot,
			m_sides[2].m_points[0].m_dot );
	}
	
	/** 隣接する2次元超平面 */
	final Face[] m_neighbors = new Face[ 3 ];
	// TODO 2015/1/27リファクタリングの際に要素が何であるか不明 
	private final Plane m_plane;
	/** 凸包対象の点列 */
	private final Collection<Point> m_uppers = new ArrayList<Point>();
	/** 構成する1次元超平面 */
	private final Side[] m_sides = new Side[ 3 ];

}