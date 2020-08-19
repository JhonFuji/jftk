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
 * 1次元超平面(線)を表すクラスです。
 * @author kaneko
 */
public class Side implements Hyperplane {
	
	/**
	 * 指定された1次元超平面群から2次元超平面を生成します。
	 * @param _points 指定された1次元超平面
	 * @return 2次元超平面
	 * @throws IllegalArgumentException 指定された一次元超平面がnullもしくはnullを含む場合
	 */
	public static Side create(Dot[] _points ){
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null" );
		}
		if ( Arrays.asList( _points ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_points include null" );
		}
		return new Side(_points);
	}
	
	
	/**
	 * 指定された1次元超平面群とベクトルから2次元超平面を生成します。
	 * @param _points 指定された1次元超平面群
	 * @param _upVector 指定されたベクトル
	 * @throws IllegalArgumentException 指定された一次元超平面がnullもしくはnullを含む場合
	 */
	public Side( Dot[] _points, Vector _upVector ) {
		if ( _points == null ) {
			throw new IllegalArgumentException( "_points is null" );
		}
		if ( Arrays.asList( _points ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_points include null" );
		}
		if ( _upVector == null ) {
			throw new IllegalArgumentException( "_points is null" );
		}
		System.arraycopy( _points, 0, m_points, 0, m_points.length );
		m_upVector = _upVector;
		m_plane = Plane.create( m_points[0].m_dot, Vector.createSE( m_points[0].m_dot, m_points[1].m_dot ).cross( _upVector ) );
	}
	
	/**
	 * 指定された最も遠い点から自身の輪郭を生成する。
	 * @param _farthest 指定された最も遠い点
	 * @return 自身の輪郭
	 */
	public Silhouette<Side> silhouette( Point _farthest ) {
		if ( m_plane.distance( _farthest ) <= 0 ) {
			return new Silhouette<Side>( Collections.<Silhouette.SilhouetteEdge<Side>>emptyList(), Collections.<Point>emptyList() );
		}

		List<Silhouette.SilhouetteEdge<Side>> silhouetteEdges = new ArrayList<Silhouette.SilhouetteEdge<Side>>();
		Collection<Point> uppers = new HashSet<Point>();
		for ( int i = 0; i < 2; ++i ) {
			Side side = this;
			do {
				uppers.addAll( side.m_uppers );
				side = side.m_neighbors[i];
			} while ( side.m_plane.distance( _farthest ) > 0 );
			uppers.addAll( side.m_uppers );
			silhouetteEdges.add( new Silhouette.SilhouetteEdge<Side>( side, ( i + 1 ) % 2 ) );
		}

		return new Silhouette<Side>( silhouetteEdges, uppers );
	}

	@Override
	public List<Side> expand() {
		List<Side> result = new ArrayList<Side>();
		Set<Side> obsolate = new HashSet<Side>();
		Stack<Side> sStack = new Stack<Side>();
		Stack<Integer> iStack = new Stack<Integer>();
		Side current = this;
		while ( true ) {
			if ( !obsolate.contains(current ) ) {
				Point farthest = current.farthest();
				while ( farthest != null ) {
					Silhouette<Side> silhouette = current.silhouette( farthest );
					Side prev = silhouette.m_silhouetteEdges.get( silhouette.m_silhouetteEdges.size() - 1 ).m_hyperplane;
					for ( Silhouette.SilhouetteEdge<Side> side : silhouette.m_silhouetteEdges ) {
						// 新しい辺を生成
						Dot[] dots = new Dot[ 2 ];
						dots[( side.m_index + 1 ) % dots.length] = side.m_hyperplane.m_points[side.m_index];
						dots[side.m_index] = Dot.create( farthest );
						Side newSide = new Side( dots, m_upVector );
						// 上側集合を割り当てる
						newSide.assign( silhouette.m_uppers );
						// 隣接情報を更新
						side.m_hyperplane.m_neighbors[side.m_index] = newSide;
						newSide.m_neighbors[( side.m_index + 1 ) % dots.length] = side.m_hyperplane;
						newSide.m_neighbors[side.m_index] = prev;
						prev.m_neighbors[( side.m_index + 1 ) % dots.length] = newSide;
						prev = newSide;
					}
					current = prev;
					farthest = current.farthest();
				}
				obsolate.add( current );
				result.add( current );
				sStack.push( current );
				iStack.push( 1 );
				current = current.m_neighbors[0];
			} else {
				if ( sStack.empty() ) {
					break;
				} else {
					Side side = sStack.pop();
					int i = iStack.pop();
					current = side.m_neighbors[i];
				}
			}
		}
		return result;
	}

	@Override
	public Point[] vertices() {
		return new Point[]{ m_points[0].m_dot, m_points[1].m_dot };
	}

	@Override
	public Point farthest() {
		Point farthest = null;
		if ( m_plane != null ) {
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
		}
		return farthest;
	}

	@Override
	public void assign( Point[] _candidate ) {
		if ( m_plane != null ) {
			for ( Point p : _candidate ) {
				double length0 = Vector.createSE( m_points[0].m_dot, p ).cross( m_upVector ).length();
				double length1 = Vector.createSE( p, m_points[1].m_dot ).cross( m_upVector ).length();
				if ( !Double.isInfinite( 1.0 / length0 )
					&& !Double.isInfinite( 1.0 / length1 )
					&& m_plane.distance( p ) > 0 ) {
					m_uppers.add( p );
				}
			}
		}
	}

	@Override
	public void assign( Collection<Point> _candidate ) {
		if ( m_plane != null ) {
			for ( Point p : _candidate ) {
				double length0 = Vector.createSE( m_points[0].m_dot, p ).cross( m_upVector ).length();
				double length1 = Vector.createSE( p, m_points[1].m_dot ).cross( m_upVector ).length();
				if ( !Double.isInfinite( 1.0 / length0 )
					&& !Double.isInfinite( 1.0 / length1 )
					&& m_plane.distance( p ) > 0 ) {
					m_uppers.add( p );
				}
			}
		}
	}

	private Side( Dot[] _points ) {
		System.arraycopy( _points, 0, m_points, 0, m_points.length );
		m_upVector = null;
		m_plane = null;
	}
	
	/** 構成する0次元超平面群 */
	final Dot[] m_points = new Dot[ 2 ];
	/** 隣接する1次元超平面 */
	final Side[] m_neighbors = new Side[ 2 ];
	// TODO 2015/1/27リファクタリングの際に要素が何であるか不明 
	private final Vector m_upVector;
	// TODO 2015/1/27リファクタリングの際に要素が何であるか不明 
	private final Plane m_plane;
	/** 凸包対象の点列 */
	private final Collection<Point> m_uppers = new ArrayList<Point>();
}