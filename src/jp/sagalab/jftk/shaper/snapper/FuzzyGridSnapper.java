package jp.sagalab.jftk.shaper.snapper;

import jp.sagalab.jftk.FuzzySet;
import jp.sagalab.jftk.Point;

/**
 * ファジィグリッドスナッピングを行うためのインタフェースです。
 * <p>
 * ファジィグリッドスナッピングは描画の雑さの程度に基づいてスナッピング先の
 * グリッド解像度を自動的に調節する手法です。
 * これにより描き手は描画の雑さの程度を制御することでスナッピング先のグリッド解像度を
 * 調節することができます。
 * </p>
 * @author miwa
 * @param <T> ファジィ集合
 */
public interface FuzzyGridSnapper<T extends FuzzySet>{

	/**
	 * グリッドの格子点を表すクラスです。<br>
	 * グリッド格子点の点情報に加えて、グリッド情報も持ちます。
	 */
	public static class GridPoint{

		/**
		 * グリッド格子点を生成します。
		 * @param _p 格子点
		 * @param _grid 格子点が属すグリッド空間
		 * @return グリッド格子点
		 * @throws IllegalArgumentException 指定した格子点がnullの場合
		 * @throws IllegalArgumentException 指定したグリッド空間がnullの場合
		 */
		protected static GridPoint create( Point _p, GridSpace _grid ) {
			if ( _p == null ) {
				throw new IllegalArgumentException( "point is null" );
			}
			if ( _grid == null ) {
				throw new IllegalArgumentException( "grid-space is null" );
			}

			return new GridPoint( _p, _grid );
		}

		/**
		 * グリッド格子点の点情報を返します。
		 * @return 点
		 */
		public Point getPoint() {
			return m_point;
		}

		/**
		 * グリッド情報を返します。
		 * @return グリッド。
		 */
		public GridSpace getGrid() {
			return m_grid;
		}

		private GridPoint( Point _p, GridSpace _grid ) {
			m_point = _p;
			m_grid = _grid;
		}

		/** 点 */
		private final Point m_point;
		/** グリッド */
		private final GridSpace m_grid;
	}

	/**
	 * ファジィグリッドスナッピングを行います。
	 * @param _grid スナッピングに用いるグリッド
	 * @param _fuzzySet スナッピング対象となるファジィ集合
	 * @return スナッピング先のグリッド格子点
	 */
	public GridPoint snap( GridSpace _grid, T _fuzzySet );
}
