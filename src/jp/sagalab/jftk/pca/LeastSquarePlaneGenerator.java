package jp.sagalab.jftk.pca;

import jp.sagalab.jftk.Plane;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;

/**
 * 最小自乗平面を生成するためのクラスです。
 * @author kaneko
 */
public final class LeastSquarePlaneGenerator {
	
	/**
	 * 最小自乗平面を生成します。
	 * @param _points 点列
	 * @return 最小自乗平面。
	 * @throws IllegalArgumentException 点列が一直線上もしくは全て同一座標である場合
	 */
	public static Plane generate( Point[] _points ) {
		// 平面の基点となる平均座標を求める
		Point expectation = PrincipalComponentAnalyst.calcExpectation( _points );
		// 主成分分析により主成分軸を求める
		Vector[] vectors = PrincipalComponentAnalyst.analyze( _points );
		// 第一主成分軸と第二主成分軸がなす平面の法線ベクトルを外積によって求める
		Vector normal = vectors[0].cross( vectors[1] );

		if(Double.isInfinite( 1.0 / normal.length() )){
			// 点列が一直線上もしくは全て同一座標である場合、平面は不定となり求めることができない
			throw new IllegalArgumentException( "_points are on the same plane or same point " );
		}
		
		return Plane.create( expectation, normal );
	}
	
	private LeastSquarePlaneGenerator() {
		throw new UnsupportedOperationException("can not create instance.");
	}
}