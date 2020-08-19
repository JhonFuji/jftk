package jp.sagalab.jftk.shaper.snapper;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.transform.Transformable;

/**
 * グリッド空間を表すクラスです。
 * @author Akira Nishikawa
 */
public final class GridSpace implements Transformable<GridSpace>{

	/**
	 * グリッド空間を生成します。
	 * @param _basedGrid グリッド空間への変換行列
	 * @param _basedFuzziness グリッドのファジネス
	 * @param _factor グリッドの倍率。倍率には２か４を推奨します。
	 * @return グリッド空間
	 * @throws IllegalArgumentException 指定された型がnullの場合
	 * @throws IllegalArgumentException _basedFuzzinessが有限の正の数でない場合
	 * @throws IllegalArgumentException _factorが1より大きい有限の正の数でない場合
	 */
	public static GridSpace create( TransformMatrix _basedGrid, double _basedFuzziness, double _factor ){
		if ( _basedGrid == null ) {
			throw new IllegalArgumentException( "based grid is null." );
		}
		if ( Double.isNaN( _basedFuzziness ) ) {
			throw new IllegalArgumentException( "fuzziness of based grid is NaN." );
		}
		if ( Double.isInfinite( _basedFuzziness ) ) {
			throw new IllegalArgumentException( "fuzziness of based grid is inf." );
		}
		if ( _basedFuzziness <= 0 ) {
			throw new IllegalArgumentException( "fuzziness of based grid less than 0." );
		}
		if ( Double.isNaN( _factor ) ) {
			throw new IllegalArgumentException( "grid factor is NaN." );
		}
		if ( Double.isInfinite( _factor ) ) {
			throw new IllegalArgumentException( "grid factor is inf." );
		}
		if ( _factor <= 1 ) {
			throw new IllegalArgumentException( "grid factor less than 1." );
		}

		return new GridSpace( _basedGrid, _basedFuzziness, _factor );
	}
	
	/**
	 * グリッド空間への変換行列を返します。
	 * @return 基準グリッド空間への変換行列
	 */
	public TransformMatrix grid() {
		return m_basedGrid;
	}

	/**
	 * グリッド格子点のファジネスを返します。
	 * @return 基準グリッド格子点のファジネス
	 */
	public double basedFuzziness() {
		return m_basedFuzziness;
	}

	/**
	 * グリッドの倍率を返します。
	 * @return 倍率
	 */
	public double factor() {
		return m_factor;
	}

	/**
	 * グリッドの解像度を倍率に従って下げます。
	 * @return 基準グリッドの解像度を下げたグリッド空間
	 */
	public GridSpace downResolution() {
		return changeResolution( factor());
	}

	/**
	 * グリッドの解像度を倍率に従って上げます。
	 * @return 基準グリッドの解像度を上げたグリッド
	 */
	public GridSpace upResolution() {
		return changeResolution( 1 / factor() );
	}

	/**
	 * 指定された変換行列を用いて相似変換を行います。
	 * @param _matrix 変換行列
	 * @return 変換されたグリッド空間
	 */
	@Override
	public GridSpace transform( TransformMatrix _matrix ) {
		TransformMatrix mat = m_basedGrid.product( _matrix );
		return new GridSpace( mat, m_basedFuzziness * _matrix.scalalize(), m_factor );
	}

	/**
	 * グリッドの解像度を指定されたスケール倍します。
	 * @param _scale スケール
	 * @return グリッドの解像度を変更したグリッド
	 */
	private GridSpace changeResolution(double _scale){
		TransformMatrix matrix = grid();
		Point p = Point.createXYZ(
			matrix.get( 0, 3 ),
			matrix.get( 1, 3 ),
			matrix.get( 2, 3 ) );
		matrix = matrix.scale( _scale, p );

		return new GridSpace( matrix, basedFuzziness() * _scale, factor() );
	}

	private GridSpace( TransformMatrix _basedGrid, double _basedFuzziness, double _factor ) {
		m_basedGrid = _basedGrid;
		m_basedFuzziness = _basedFuzziness;
		m_factor = _factor;
	}

	/** グリッド空間への変換行列 */
	private final TransformMatrix m_basedGrid;
	/** 基準グリッドのファジネス（あいまいさの大きさ） */
	private final double m_basedFuzziness;
	/** グリッド解像度の上げ下げの指標となるグリッドの倍率 */
	private final double m_factor;
}