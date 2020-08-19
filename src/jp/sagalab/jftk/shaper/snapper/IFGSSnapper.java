package jp.sagalab.jftk.shaper.snapper;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 無限解像度ファジィグリッドスナッピング(Infinite-resolution Fuzzy Grid Snapping, IFGS)
 * を行うためのクラスです。<br>
 * このクラスは単一のファジィ集合のスナッピングのみをサポートしています。
 * <p>
 * グリッド( {@link GridSpace} )は無限解像度ファジィグリッドとして扱います。<br>
 * 無限解像度ファジィグリッドは基準グリッドと倍率からなります。
 * 基準グリッドから倍率に従い、両側に無限に広がるようにグリッド群が定義されます。<br>
 * (例)<br>
 * 基準グリッド:グリッド幅40のグリッド、派生倍率:2の場合、各グリッドのグリッド幅は以下のようになります。<br>
 * ..., 5, 10, 20, 40, 80, 160, 320, ...<br>
 * これらの設定は {@link GridSpace} クラスで定義されます。
 * </p>
 * <p>
 * IFGSでは「描画の雑さの程度に基づいて可能な限り低解像度のグリッドを選択する」という
 * ファジィ理論に基づいたファジィ推論規則を用いて解像度の選択を行います。
 * この推論規則は言語的真理値T(Unitary True), F(Unitary False)を用いたファジィ命題を含みます。
 * 言語的真理値T, Fにはシグモイド関数( {@link Sigmoid} )が用いられています。
 * 言語的真理値T, Fを調節することで、例えば"解像度の低いグリッドにスナッピングされやすくなる"
 * といったようにスナッピングの傾向を調節することができます。
 * </p>
 * <p>
 * IFGSは，アルゴリズム上においては無限解像度ファジィグリッドの中からスナッピング解像度の
 * 選択が可能であるが，計算機上は無限の計算を行うことができないため，基準グリッドから上限解像度の
 * 探索を行う回数について最大値を設けています。このような解像度を最大上限解像度と呼び，上限解像度探索が
 * これを上回る場合は最大上限解像度にスナッピングする。
 * </p>
 * @author miwa
 */
public class IFGSSnapper implements FuzzyGridSnapper<Point>{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _true 言語的真理値「真」
	 * @param _false 言語的真理値「偽」
	 * @return インスタンス
	 * @throws IllegalArgumentException 引数の型にnullが指定された場合
	 */
	public static IFGSSnapper create( Sigmoid _true, Sigmoid _false ) {
		if ( _true == null ) {
			throw new IllegalArgumentException( "sigmoid true of snapping is null." );
		}
		if ( _false == null ) {
			throw new IllegalArgumentException( "sigmoid false of snapping is null." );
		}
		return new IFGSSnapper( _true, _false );
	}

	/**
	 * 指定されたグリッドの基準グリッドでグリッドスナッピングを行います。<br>
	 * 一般的なグリッドスナッピングです。
	 * @param _grid スナッピングに用いるグリッド
	 * @param _p スナッピング対象となる点
	 * @return スナッピング先のグリッド格子点
	 */
	public static Point snapBasedGrid( GridSpace _grid, Point _p ) {
		//XXX このメソッドがここにあるのは適当か？ BasedGridSnapper的なクラスを作るべき？
		return nearestPoint( _grid, _p );
	}

	/**
	 * 無限解像度ファジィグリッドスナッピングを行います。
	 * @param _grid IFGSを行う基準グリッド
	 * @param _p スナッピング対象となる点
	 * @return スナッピング先のグリッド格子点
	 */
	@Override
	public FuzzyGridSnapper.GridPoint snap( GridSpace _grid, Point _p ) {
		// スナッピング点
		Point snapped = _p;
		// 上限解像度グリッド
		GridSpace grid = searchHigherLimitResolution( _grid, _p, m_true, m_false );
		// 解像度を1段階下げる
		grid = grid.downResolution();
		// スナッピング解像度
		GridSpace snappedGrid = grid;

		while ( true ) {
			// スナッピング対象の点に最も近いファジィグリッド交点を生成
			Point gridPoint = nearestPoint( grid, _p );
			// ファジィグリッド交点がスナップ対象の点に含まれている必然性値
			double nec = gridPoint.includedIn( _p ).necessity();

			if ( m_true.calculate( nec ) < m_false.calculate( nec ) ) {
				break;
			}
			snapped = gridPoint;
			snappedGrid = grid;

			// 解像度を1段階下げる
			grid = grid.downResolution();
		}

		return FuzzyGridSnapper.GridPoint.create( snapped, snappedGrid );
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		
		final IFGSSnapper other = (IFGSSnapper) obj;
		if ( !this.m_true.equals( other.m_true ) ) {
			return false;
		}

		return this.m_false.equals( other.m_false );
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + ( this.m_true != null ? this.m_true.hashCode() : 0 );
		hash = 37 * hash + ( this.m_false != null ? this.m_false.hashCode() : 0 );
		return hash;
	}

	@Override
	public String toString() {
		return String.format( "SigmoidTrue:" + m_true.toString() + " SigmoidFalse:" + m_false.toString() );
	}
	
	
	/**
	 * 指示されたグリッド、ファジィ点から上限解像度グリッドを探索します。<br>
	 * ここで上限解像度グリッドとは、IFGSによってスナッピング先を高解像度側に探索した際に
	 * グレードがはじめて非増加となる解像度グリッドを指します。<br>
	 * また、引数で指定されたグリッドの解像度上げが最大上限解像度の探索回数に達した場合は
	 * その時点の解像度を上限解像度として探索を終了します。
	 * @param _grid 探索を開始するグリッド
	 * @param _p スナッピング対象となる点
	 * @param _true 言語的真理値「真」
	 * @param _false 言語的真理値「偽」
	 * @return 上限解像度グリッド
	 */
	private static GridSpace searchHigherLimitResolution( GridSpace _grid, Point _p, Sigmoid _true, Sigmoid _false ) {
		GridSpace targetGrid = _grid;
		double nec;
		int i = 0;
		// 解像度を上げながら探索を行う
		while ( true ) {
			Point gridPoint = nearestPoint( targetGrid, _p );
			nec = gridPoint.includedIn( _p ).necessity();

			if ( _true.calculate( nec ) > _false.calculate( nec ) ) {
				return targetGrid.upResolution();
			}

			// 最大上限解像度の判定
			if ( i >= MAX_HIGHER_LIMIT ) {
				return targetGrid.upResolution();
			}
			++i;
			targetGrid = targetGrid.upResolution();
		}
	}

	/**
	 * グリッドスナッピングを行います。
	 * @param _point スナッピング対象となる点
	 * @param _mat スナッピングに用いるグリッド
	 * @return スナッピングされた点
	 */
	private static Point nearestPoint( GridSpace _grid, Point _point ) {
		TransformMatrix basedGrid = _grid.grid();
		TransformMatrix invMat = basedGrid.inverse();
		Point p = _point.transform( invMat );
		double x = Math.round( p.x() );
		double y = Math.round( p.y() );
		double z = Math.round( p.z() );
		double t = Double.NaN;
		double f = _grid.basedFuzziness() / basedGrid.scalalize();
		Point point = Point.createXYZTF( x, y, z, t, f ).transform( basedGrid );

		return point;
	}

	private IFGSSnapper( Sigmoid _true, Sigmoid _false ) {
		m_true = _true;
		m_false = _false;
	}

	/** 言語的真理値「真」 */
	private final Sigmoid m_true;
	/** 言語的真理値「偽」 */
	private final Sigmoid m_false;
	/** 最大上限解像度の探索回数 */
	private static final int MAX_HIGHER_LIMIT = 20;
}