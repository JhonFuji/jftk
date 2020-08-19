package jp.sagalab.jftk.convex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Vector;

/**
 * 0次元の超平面(点)を表すクラスです。
 * @author kaneko
 */
class Dot implements Hyperplane {

	/**
	 * 指定された点から0次元超平面を生成します。
	 * @param _point 指定された点
	 * @return 0次元平面
	 * @throws IllegalArgumentException 指定された点がnullの場合
	 */	
	public static Dot create( Point _point ){
		if ( _point == null ) {
			throw new IllegalArgumentException( "_point is null" );
		}
		return new Dot( _point );
	}
	
	/**
	 * 指定された点とベクトルから0次元超平面を生成します。
	 * @param _point 指定された点
	 * @param _normal 指定されたベクトル
	 * @throws IllegalArgumentException 指定された点がnullの場合
	 * @throws IllegalArgumentException 指定されたベクトルがnullの場合
	 */
	Dot( Point _point, Vector _normal ) {
		if ( _point == null ) {
			throw new IllegalArgumentException( "_point is null" );
		}
		if ( _normal == null ) {
			throw new IllegalArgumentException( "_normal is null" );
		}
		m_dot = _point;
		m_plane = Plane.create( _point, _normal );
	}
	
	@Override
	public List<Dot> expand() {
		List<Dot> result = new ArrayList<Dot>();
		Dot current = this;
		// 1次元の凸包領域は線分になるので，高々2回の探索で十分である
		for ( int i = 0; i < 2; ++i ) {
			Point farthest = current.farthest();
			if ( farthest != null ) {
				Dot newDot = new Dot( farthest, Vector.createSE( current.m_neighbor.m_dot, farthest ) );
				newDot.m_neighbor = current.m_neighbor;
				current.m_neighbor.m_neighbor = newDot;
				result.add( newDot );
			} else {
				result.add( current );
			}
			current = current.m_neighbor;
		}
		return result;
	}

	@Override
	public Point[] vertices() {
		return new Point[]{ m_dot };
	}

	@Override
	public Point farthest() {
		Point farthest = null;
		if ( m_plane != null ) {
			double maxDist = Double.NEGATIVE_INFINITY;
			for ( Point p : m_uppers ) {
				double dist = m_plane.distance( p );
				if ( dist > maxDist ) {
					maxDist = dist;
					farthest = p;
				} else if ( dist == maxDist && farthest.fuzziness() < p.fuzziness() ) {
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
				if ( m_plane.distance( p ) > 0 ) {
					m_uppers.add( p );
				}
			}
		}
	}

	@Override
	public void assign( Collection<Point> _candidate ) {
		if ( m_plane != null ) {
			for ( Point p : _candidate ) {
				if ( m_plane.distance( p ) > 0 ) {
					m_uppers.add( p );
				}
			}
		}
	}
	
	private Dot( Point _point ) {
		m_dot = _point;
		m_plane = null;
	}
	
	/** 構成する頂点 */
	final Point m_dot;
	/** 隣接する点 */
	Dot m_neighbor;
	// TODO 2015/1/27リファクタリングの際に要素が何であるか不明 
	private final Plane m_plane;
	/** 凸包対象の点列 */
	private final Collection<Point> m_uppers = new ArrayList<Point>();
}