package jp.sagalab.jftk.shaper.snapper;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.recognition.BoundingBox;
import jp.sagalab.jftk.shaper.snapper.FuzzyGridSnapper.GridPoint;

/**
 * スナッピング候補を表すクラスです。
 *
 * @author nagase
 */
public class SnappingCandidate{

	/**
	 * スナッピング点が曲線のどのような点かを表す識別子です。
	 */
	public enum SnappingPointType{
		/**
		 * 端点。
		 * 始点及び終点のこと。
		 */
		EDGE,
		/**
		 * 径点。
		 * 円においては、始終点の垂直2等分線と図形が交わる2点。及び、前述の2点の垂直2等分線と図形が交わる点のこと。
		 * 楕円においては、長径点、及び短径点のこと。
		 */
		DIAMETER,
		/**
		 * 頂点。
		 * 楕円を含む最小長方形の角の点のこと。
		 */
		CORNER,
		PARTITION;
	}

	/**
	 * スナッピング候補を生成します。
	 * 各引数の配列の添字がそれぞれ対応している事が前提です。
	 * 例えば、_types[1]は_points[1]の種類を表す、と言った具合です。
	 * @param _points 候補点列
	 * @param _types 候補点列の種類
	 * @param _box 候補点列が所属するバウンディングボックス。点列がボックス上の点ではないのならばnullを指定してください。
	 * @throws IllegalArgumentException _pointsにnullが含まれる場合
	 * @throws IllegalArgumentException _typesにnullが含まれる場合
	 */
	public static SnappingCandidate create( Point[] _points, SnappingPointType[] _types, BoundingBox _box ) {
		if ( _points.length != _types.length ) {
			throw new IllegalArgumentException( "_points's length is not equals _types's length." );
		}
		if ( Arrays.asList( _points ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_points include null." );
		}
		if ( Arrays.asList( _types ).indexOf( null ) > -1 ) {
			throw new IllegalArgumentException( "_types include null." );
		}
		return new SnappingCandidate( _points, _types, _box );
	}

	/**
	 * グリット格子点列を計算します。
	 * @param _snapper グリッドスナッピングを行うためのスナッパー
	 * @param _grid 格子点が属すグリッド空間
	 * @return グリット格子点列
	 */
	public GridPoint[] calculateSnappedPointsData( FuzzyGridSnapper _snapper, GridSpace _grid ) {
		Point[] points = getPoints();
		GridPoint[] gridPoints = new GridPoint[points.length];
		for ( int i = 0; i < points.length; i++ ) {
			gridPoints[i] = _snapper.snap( _grid, points[i] );
		}
		return gridPoints;
	}

	/**
	 * スナッピング先の点列を計算します。
	 * @param _snapper グリッドスナッピングを行うためのスナッパー
	 * @param _grid 格子点が属すグリッド空間
	 * @return スナッピング先の点列
	 */
	public Point[] calculateSnappedPoints( FuzzyGridSnapper _snapper, GridSpace _grid ) {
		Point[] multiPoints = getPoints();
		Point[] points = new Point[multiPoints.length];
		for ( int i = 0; i < multiPoints.length; i++ ) {
			GridPoint gridPoint = _snapper.snap( _grid, multiPoints[i] );
			points[i] = gridPoint.getPoint();
		}
		return points;
	}

	/**
	 * グリッド空間の計算をします
	 * @param _snapper グリッドスナッピングを行うためのスナッパー
	 * @param _grid 格子点が属すグリッド空間
	 * @return グリッド空間の配列
	 */
	public GridSpace[] calculateSnappedGridSpaces( FuzzyGridSnapper _snapper, GridSpace _grid ) {
		Point[] multiPoints = getPoints();
		GridSpace[] gridSpaces = new GridSpace[multiPoints.length];
		for ( int i = 0; i < multiPoints.length; i++ ) {
			GridPoint gridPoint = _snapper.snap( _grid, multiPoints[i] );
			gridSpaces[i] = gridPoint.getGrid();
		}
		return gridSpaces;
	}

	/**
	 * スナッピング候補点列を返します。
	 * @return 候補点列
	 */
	public Point[] getPoints() {
		return m_points.clone();
	}

	/**
	 * 候補点列の種類を返します。
	 * @return 候補点列の種類
	 */
	public SnappingPointType[] getTypes() {
		return m_types.clone();
	}

	/**
	 * 候補点列が所属するバウンディングボックスを返します。
	 * @return バウンディングボックス
	 */
	public BoundingBox getBox() {
		return m_box;
	}
	
	private SnappingCandidate( Point[] _points, SnappingPointType[] _types, BoundingBox _box ) {
		m_points = _points.clone();
		m_types = _types.clone();
		m_box = _box;
	}

	/** スナッピング候補点列 */
	private final Point[] m_points;
	/** 候補点列の種類 */
	private final SnappingPointType[] m_types;
	/** 候補点列が所属するボックス */
	private final BoundingBox m_box;
}
