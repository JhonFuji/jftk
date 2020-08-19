package jp.sagalab.jftk.shaper;

import java.util.Arrays;
import jp.sagalab.jftk.recognition.BoundingBox;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.shaper.snapper.GridSpace;


/**
 * スナッピング結果を表すクラスです。
 * <p>
 * このクラスはスナッピング後の状態であるというFSCI上での状態をクラスとして明確に表現しています。
 * (デザインパターンのStateパターンではありません。)<br>
 * </p>
 * @author miwa
 */
public class ShapedResult {

	/**
	 * スナッピング結果を生成します。
	 * @param _primitiveCurve 認識結果の幾何曲線
	 * @param _snappedPoints スナッピング後のスナッピング点列
	 * @param _grids スナッピングに用いられたグリッド群
	 * @param _boundingBox バウンディングボックス
	 * @return スナッピング結果
	 */
	public static ShapedResult create( PrimitiveCurve _primitiveCurve, Point[] _snappedPoints,
			GridSpace[] _grids, BoundingBox _boundingBox ) {

		if ( _primitiveCurve == null ) {
			throw new IllegalArgumentException( "_primitiveCurve is null." );
		}
		if ( _snappedPoints.length != _grids.length ) {
			throw new IllegalArgumentException( "_snappedPoints's length is not equals _grids's length." );
		}
		if ( Arrays.asList( _snappedPoints ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_snappedPoints include null." );
		}
		if ( Arrays.asList( _grids ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_grids include null." );
		}

		return new ShapedResult(_primitiveCurve, _snappedPoints, _grids, _boundingBox );
	}

	/**
	 * 幾何曲線を返します。
	 * @return 幾何曲線
	 */
	public PrimitiveCurve getCurve(){
		return m_primitive;
	}

	/**
	 * スナッピング後のスナッピング点を返します。
	 * @return スナッピング後のスナッピング点
	 */
	public Point[] getSnappedPoints(){
		return m_snappedPoints.clone();
	}

	/**
	 * スナッピングに用いられたグリッドを返します。
	 * @return グリッド
	 */
	public GridSpace[] getSnappedGrids(){
		return m_grids.clone();
	}

	/**
	 * バウンディングボックスを返す
	 * @return バウンディングボックス
	 */
	public BoundingBox getBoundingBox(){
		return m_boundingBox;
	}
	
	private ShapedResult( PrimitiveCurve _primitiveCurve, Point[] _snappedPoints,
			GridSpace[] _grids, BoundingBox _boundingBox ) {

		m_primitive = _primitiveCurve;
		m_snappedPoints = _snappedPoints.clone();
		m_grids = _grids.clone();
		m_boundingBox = _boundingBox;
	}

	/** プリミティブ曲線 */
	private final PrimitiveCurve m_primitive;
	/** スナッピング後点列 */
	private final Point[] m_snappedPoints;
	/** スナッピング先のグリッド */
	private final GridSpace[] m_grids;
	/** スナッピング後の曲線のバウンディングボックス */
	private final BoundingBox m_boundingBox;
}