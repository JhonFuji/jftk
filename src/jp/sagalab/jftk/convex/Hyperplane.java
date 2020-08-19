package jp.sagalab.jftk.convex;

import java.util.Collection;
import java.util.List;
import jp.sagalab.jftk.Point;

/**
 * n次元超平面を表すインタフェース
 * @author kaneko
 */
interface Hyperplane {
	
	/**
	 * 凸包領域の拡張
	 * @return 凸包を構成するn次元超平面
	 */
	List<? extends Hyperplane> expand();

	/**
	 * 頂点列を返す
	 * @return 頂点列
	 */
	Point[] vertices();

	/**
	 * 平面から最も遠い点を算出
	 * @return 平面から最も遠い点
	 */
	Point farthest();

	/**
	 * 指定された点列を新たに割り当てる
	 * @param _candidate 指定された点列
	 */
	void assign( Point[] _candidate );

	/**
	 * 指定された点列を新たに割り当てる
	 * @param _candidate 指定された点列
	 */
	void assign( Collection<Point> _candidate );
}