package jp.sagalab.jftk.convex;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jp.sagalab.jftk.Point;

/**
 * n次元超平面の輪郭を表すクラスです。
 * @author kaneko
 */
class Silhouette<T extends Hyperplane> {
	
	/**
	 * 輪郭形状を表すクラスです。
	 * @param <U> n次元超平面
	 */
	static class SilhouetteEdge<U extends Hyperplane> {
		
		/**
		 * 指定されたn次元超平面とインデックスから輪郭形状を生成する。
		 * @param _hyperplane n次元超平面
		 * @param _index インデックス
		 * @throws IllegalArgumentException n次元超平面がnullの場合
		 */
		SilhouetteEdge( U _hyperplane, int _index ) {
			if ( _hyperplane == null ) {
				throw new IllegalArgumentException( "_hyperplane is null" );
			}
			m_hyperplane = _hyperplane;
			m_index = _index;
		}	
		/** n次元超平面 */
		final U m_hyperplane;
		/** インデックス */
		final int m_index;
	}
	
	/**
	 * 輪郭を生成します。
	 * @param _silhouetteEdges 輪郭形状
	 * @param _uppers 凸包対象の点列
	 * @throws IllegalArgumentException 輪郭形状がnull、もしくはnullを含む場合
	 * @throws IllegalArgumentException 凸包対象の点列がnull、もしくはnullを含む場合
	 */
	Silhouette( List<SilhouetteEdge<T>> _silhouetteEdges, Collection<Point> _uppers ) {
		if ( _silhouetteEdges == null ) {
			throw new IllegalArgumentException( "_silhouetteEdges is null" );
		}
		if ( Arrays.asList( _silhouetteEdges ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_silhouetteEdges include null" );
		}
		if( _uppers == null ){
			throw new IllegalArgumentException( "_uppers is null" );
		}
		if ( Arrays.asList( _uppers ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_uppers include null" );
		}
		m_silhouetteEdges = _silhouetteEdges;
		m_uppers = _uppers;
	}
	
	/** 輪郭形状 */
	final List<SilhouetteEdge<T>> m_silhouetteEdges;
	/** 凸包対象の点列 */
	final Collection<Point> m_uppers;
}