package jp.sagalab.jftk.shaper.snapper;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 多重解像度ファジィグリッドスナッピング(Multi-Resolution Fuzzy Grid Snapping, MFGS)
 * を行うためのクラスです。<br>
 * このクラスは単一のファジィ集合のスナッピングのみをサポートしています。
 * <p>
 * グリッド( {@link GridSpace} )は多重解像度ファジィグリッドとして扱います。<br>
 * 多重解像度ファジィグリッドは基準グリッドと倍率とグリッド数からなります。
 * 基準グリッドから倍率に従って解像度が荒くなっていくようにグリッド数分のグリッドが定義されます。<br>
 * (例)<br>
 * 基準グリッド:グリッド幅40のグリッド、派生倍率:2、グリッド数:5の場合、各グリッドのグリッド幅は以下のようになります。<br>
 * 40, 80, 160, 320, 640<br>
 * 基準グリッド、倍率の設定は {@link GridSpace} クラスで定義されます。
 * </p>
 * <p>
 * MFGS法では「描画の雑さの程度に基づいて可能な限り低解像度のグリッドを選択する」という
 * ファジィ理論に基づいたファジィ推論規則を用いて解像度の選択を行います。
 * この推論規則は言語的真理値T(Unitary True), F(Unitary False)を用いたファジィ命題を含みます。
 * 言語的真理値T, Fにはシグモイド関数( {@link Sigmoid} )が用いられています。
 * 言語的真理値T, Fを調節することで、例えば"解像度の低いグリッドにスナッピングされやすくなる"
 * といったようにスナッピングの傾向を調節することができます。
 * </p>
 * @author miwa
 */
public class MFGSSnapper implements FuzzyGridSnapper<Point>{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _true 言語的真理値「真」
	 * @param _false 言語的真理値「偽」
	 * @param _num グリッド数
	 * @return インスタンス
	 * @throws IllegalArgumentException 引数の型にnullが指定された場合
	 * @throws IllegalArgumentException グリッド数に0以下の値が指定された場合
	 */
	public static MFGSSnapper create(Sigmoid _true, Sigmoid _false, int _num){
		if( _true == null ){
			throw new IllegalArgumentException("sigmoid true of snapping is null.");
		}
		if(_false == null){
			throw new IllegalArgumentException("sigmoid false of snapping is null.");
		}
		if(_num <= 0){
			throw new IllegalArgumentException("grid num less than 0.");
		}
		return new MFGSSnapper(_true, _false, _num);
	}
	
	/**
	 * 多重解像度ファジィグリッドスナッピングを行います。
	 * @param _grid スナッピングに用いるグリッド
	 * @param _p スナッピング対象となる点
	 * @return スナッピング先のグリッド格子点
	 */
	@Override
	public GridPoint snap( GridSpace _grid, Point _p ) {
		// 倍率、グリッド解像度の数に従って解像度を生成
		GridSpace[] grids = new GridSpace[m_gridNum];
		// 基準グリッドを最高解像度に設定
		grids[m_gridNum - 1] = _grid;
		for ( int i = m_gridNum - 2; i >= 0; --i ) {
			grids[i] = grids[i + 1].downResolution();
		}
		// スナッピング点
		Point snapped = _p;

		// 否定の最小値
		double minInvGrade = 1;

		// グレード値の保持用
		double maxGrade = 0.0;
		GridSpace tmpGrid = _grid;
		// ノースナッピングの場合は一番細かい解像度を返す
		for ( GridSpace grid : grids ) {
			// スナッピング対象の点に最も近いファジィグリッド格子点を生成
			Point gridPoint = nearestPoint( grid, _p );
			// ファジィグリッド格子点がスナップ対象の点に含まれている必然性値
			double nec = gridPoint.includedIn( _p ).necessity();
			// グレード値の導出
			double grade = Math.min( m_true.calculate( nec ), minInvGrade );
			// グレード値が最大のものをスナッピング先に設定
			if ( maxGrade < grade ) {
				maxGrade = grade;
				snapped = gridPoint;
				tmpGrid = grid;
			}
			// 否定の最も小さなものを保持
			minInvGrade = Math.min( minInvGrade, m_false.calculate( nec ) );
		}
		
		return GridPoint.create(snapped, tmpGrid);
	}

	/**
	 * グリッドスナッピングを行います。
	 * @param _point スナッピング対象となる点
	 * @param _mat スナッピング二用いるグリッド
	 * @return スナッピングされた点
	 */
	private static Point nearestPoint( GridSpace _grid, Point _point ) {
		TransformMatrix basedGrid = _grid.grid();
		TransformMatrix invMat = basedGrid.inverse();
		Point p;
		try {
			p = _point.transform( invMat );
		} catch ( IllegalArgumentException e ) {
			p = _point.transform( invMat );
		}
		double x = Math.round( p.x() );
		double y = Math.round( p.y() );
		double z = Math.round( p.z() );
		double t = Double.NaN;
		double f = _grid.basedFuzziness() / basedGrid.scalalize();
		Point point = Point.createXYZTF( x, y, z, t, f ).transform( basedGrid );

		return point;
	}

	private MFGSSnapper(Sigmoid _true, Sigmoid _false, int _num){
		m_true = _true;
		m_false = _false;
		m_gridNum = _num;
	}
	
	/** 言語的真理値「真」 */
	private final Sigmoid m_true;
	/** 言語的真理値「偽」 */
	private final Sigmoid m_false;
	/** グリッド数 */
	private final int m_gridNum;
}