package jp.sagalab.jftk.shaper.snapper;

import java.util.Arrays;
import jp.sagalab.jftk.recognition.BoundingBox;
import jp.sagalab.jftk.Point;

/**
 * 曲線のスナッピングに関する情報を表すクラスです。
 * @author shichijo
 */
public class SnappingTargetData {

	/**
	 * 曲線のスナッピングに関するデータ群を生成します。
	 * 引数の各配列の添字がそれぞれ対応している事が前提です。
	 * @param _snappedPoints	スナッピング後の点列
	 * @param _gridSpaceSets	スナッピングされたグリッド群
	 * @param _boundingBox	スナッピング点が所属するボックス。スナッピング点がボックスに所属しない場合null
	 * @return スナッピングに関するデータ群
	 * @throws IllegalArgumentException _snappedPointsと_gridSpaceSetsの配列の要素数が異なる場合
	 * @throws IllegalArgumentException _snappedPointsにnullが含まれる場合
	 * @throws IllegalArgumentException _gridSpaceSetsにnullが含まれる場合
	 */
	public static SnappingTargetData create( Point[] _snappedPoints, GridSpace[] _gridSpaceSets,
			BoundingBox _boundingBox ){
		
		if ( _snappedPoints.length != _gridSpaceSets.length ) {
			throw new IllegalArgumentException(" _snappedPointsList's length is not equals _gridSpaceSetList's length ");
		}
		if(Arrays.asList(_snappedPoints ).indexOf( null ) > -1 ){
			throw new IllegalArgumentException( " _snappedPointsList include null " );
		}
		if(Arrays.asList(_gridSpaceSets ).indexOf( null ) > -1 ){
			throw new IllegalArgumentException( " _gridSpaceSetList include null " );
		}
		return new SnappingTargetData( _snappedPoints, _gridSpaceSets, _boundingBox);
	}
	
	/**
	 * スナッピング後の点列を返します。
	 * @return スナッピング後の点列
	 */
	public Point[] getSnappedPoints() {
		return m_snappedPoints.clone();
	}

	/**
	 * スナッピングされたグリッド群を返します。
	 * @return スナッピングされたグリッド群
	 */
	public GridSpace[] getGridSpaceSets() {
		return m_gridSpaceSets.clone();
	}

	/**
	 * 曲線のバウンディングボックスを返します。
	 * @return スナッピング後のバウンディングボックス
	 */
	public BoundingBox getBoundingBox() {
		return m_boundingBox;
	}

	private SnappingTargetData( Point[] _snappedPoints, GridSpace[] _gridSpaceSets,
			BoundingBox _boundingBox ) {
		m_snappedPoints = _snappedPoints.clone();
		m_gridSpaceSets = _gridSpaceSets.clone();
		m_boundingBox = _boundingBox;
	}
	
	/** スナッピング後の点列 */
	private final Point[] m_snappedPoints;
	/** スナッピングされたグリッド群 */
	private final GridSpace[] m_gridSpaceSets;
  /** スナッピングされた曲線のバウンディングボックス */
	private final BoundingBox m_boundingBox;
}