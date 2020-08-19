package jp.sagalab.jftk.transform;

import java.util.Arrays;
import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;

/**
 * 変換行列を表すクラスです。
 * @author yamaguchi
 */
public class TransformMatrix {

	protected TransformMatrix( double[][] _elements ) {
		m_elements = _elements.clone();
	}
	
	/**
	 * 要素を指定して、変換行列を生成します。
	 * @param _elements 要素
	 * @return 変換行列
	 * @throws IllegalArgumentException 引数の型にnullが指定された場合
	 * @throws IllegalArgumentException 要素サイズが4行4列ではない場合
	 * @throws IllegalArgumentException 引数にnullが含まれている場合
	 * @throws IllegalArgumentException 要素に無限大やNaNが含まれている場合
	 */	
	public static  TransformMatrix create( double[][] _elements ){
		if(_elements == null){
			throw new IllegalArgumentException( "_elements is null " );
		}
		// 行数チェック
		if ( _elements.length != 4 ) {
			throw new IllegalArgumentException( "_elements must be 4x4" );
		}
		for ( int i = 0; i < 4; ++i ) {
			if ( _elements[i] == null ) {
				throw new IllegalArgumentException( "_elements is included null" );
			}
			// 列数チェック
			if ( _elements[i].length != 4 ) {
				throw new IllegalArgumentException( "_elements must be 4x4" );
			}
			// 要素チェック（無限大と非数は認めない方針）
			for ( int j = 0; j < 4; ++j ) {
				if ( Double.isInfinite( _elements[i][j] ) || Double.isNaN( _elements[i][j] ) ) {
					throw new IllegalArgumentException( String.format( "_elements[ %d ][ %d ] is abnormal value:%f", i, j, _elements[i][j] ) );
				}
			}
		}
		return new TransformMatrix(  _elements );
	}
	
	/**
	 * 無変換行列(単位行列)を生成します。
	 * @return 無変換行列(単位行列)
	 */
	public static TransformMatrix identity() {
		if ( c_identity == null ) {
			c_identity = new TransformMatrix( new double[][]{
					{ 1, 0, 0, 0 },
					{ 0, 1, 0, 0 },
					{ 0, 0, 1, 0 },
					{ 0, 0, 0, 1 }
				} );
		}
		return c_identity;
	}

	/**
	 * 平行移動の変換行列を生成します。
	 * @param _x X軸方向の移動量
	 * @param _y Y軸方向の移動量
	 * @param _z Z軸方向の移動量
	 * @return 平行移動の変換行列
	 */
	public static TransformMatrix translation( double _x, double _y, double _z ) {
		return new TransformMatrix( new double[][]{
				{ 1, 0, 0, _x },
				{ 0, 1, 0, _y },
				{ 0, 0, 1, _z },
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * 平行移動の変換行列を生成します。
	 * @param _v 移動量、移動方向を表すベクトル
	 * @return 平行移動の変換行列
	 */
	public static TransformMatrix translation( Vector _v ) {
		return new TransformMatrix( new double[][]{
				{ 1, 0, 0, _v.x() },
				{ 0, 1, 0, _v.y() },
				{ 0, 0, 1, _v.z() },
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * 回転の変換行列を生成します。
	 * @param _angle 回転量（単位：radian）
	 * @param _axis 回転軸
	 * @return 回転の変換行列
	 */
	public static TransformMatrix rotation( double _angle, Vector _axis ) {
		double sin = Math.sin( _angle );
		double cos = Math.cos( _angle );
		Vector normal = _axis.normalize();

		// 回転軸の単位ベクトル normal 回りに回転量 _angle だけ回転する
		return new TransformMatrix( new double[][]{
				{
					cos + normal.x() * normal.x() * ( 1 - cos ),
					normal.x() * normal.y() * ( 1 - cos ) - normal.z() * sin,
					normal.x() * normal.z() * ( 1 - cos ) + normal.y() * sin,
					0
				},
				{
					normal.x() * normal.y() * ( 1 - cos ) + normal.z() * sin,
					cos + normal.y() * normal.y() * ( 1 - cos ),
					normal.y() * normal.z() * ( 1 - cos ) - normal.x() * sin,
					0
				},
				{
					normal.x() * normal.z() * ( 1 - cos ) - normal.y() * sin,
					normal.y() * normal.z() * ( 1 - cos ) + normal.x() * sin,
					cos + normal.z() * normal.z() * ( 1 - cos ),
					0
				},
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * 拡大縮小の変換行列を生成します。
	 * @param _ratio 拡大縮小率
	 * @return 拡大縮小の変換行列
	 */
	public static TransformMatrix scaling( double _ratio ) {
		return new TransformMatrix( new double[][]{
				{ _ratio, 0, 0, 0 },
				{ 0, _ratio, 0, 0 },
				{ 0, 0, _ratio, 0 },
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * 拡大縮小の変換行列を生成します。
	 * @param _x x方向の拡大縮小率
	 * @param _y y方向の拡大縮小率
	 * @param _z z方向の拡大縮小率
	 * @return 拡大縮小の変換行列
	 */
	public static TransformMatrix scaling( double _x, double _y, double _z ) {
		return new TransformMatrix( new double[][]{
				{ _x, 0, 0, 0 },
				{ 0, _y, 0, 0 },
				{ 0, 0, _z, 0 },
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * 要素群を取得します。
	 * @return 要素群
	 */
	public double[][] elements() {
		return new double[][]{
				m_elements[0].clone(),
				m_elements[1].clone(),
				m_elements[2].clone(),
				m_elements[3].clone()
			};
	}

	/**
	 * 指定された番号の要素を取得します。
	 * @param _row 行番号
	 * @param _column 列番号
	 * @return 要素
	 */
	public double get( int _row, int _column ) {
		return m_elements[_row][_column];
	}

	/**
	 * 指定された変換行列との掛け算を行います。
	 * <p>
	 * 前方から指定された変換行列をかけます。
	 * this.product( other ) → other * this
	 * </p>
	 * @param _mat 指定された変換行列
	 * @return 積演算結果の行列
	 */
	public TransformMatrix product( TransformMatrix _mat ) {
		double[][] e = m_elements;
		// 指定された変換行列の要素
		double[][] oe = _mat.m_elements;
		return new TransformMatrix( new double[][]{
				{
					oe[0][0] * e[0][0] + oe[0][1] * e[1][0] + oe[0][2] * e[2][0] + oe[0][3] * e[3][0],
					oe[0][0] * e[0][1] + oe[0][1] * e[1][1] + oe[0][2] * e[2][1] + oe[0][3] * e[3][1],
					oe[0][0] * e[0][2] + oe[0][1] * e[1][2] + oe[0][2] * e[2][2] + oe[0][3] * e[3][2],
					oe[0][0] * e[0][3] + oe[0][1] * e[1][3] + oe[0][2] * e[2][3] + oe[0][3] * e[3][3]
				},
				{
					oe[1][0] * e[0][0] + oe[1][1] * e[1][0] + oe[1][2] * e[2][0] + oe[1][3] * e[3][0],
					oe[1][0] * e[0][1] + oe[1][1] * e[1][1] + oe[1][2] * e[2][1] + oe[1][3] * e[3][1],
					oe[1][0] * e[0][2] + oe[1][1] * e[1][2] + oe[1][2] * e[2][2] + oe[1][3] * e[3][2],
					oe[1][0] * e[0][3] + oe[1][1] * e[1][3] + oe[1][2] * e[2][3] + oe[1][3] * e[3][3]
				},
				{
					oe[2][0] * e[0][0] + oe[2][1] * e[1][0] + oe[2][2] * e[2][0] + oe[2][3] * e[3][0],
					oe[2][0] * e[0][1] + oe[2][1] * e[1][1] + oe[2][2] * e[2][1] + oe[2][3] * e[3][1],
					oe[2][0] * e[0][2] + oe[2][1] * e[1][2] + oe[2][2] * e[2][2] + oe[2][3] * e[3][2],
					oe[2][0] * e[0][3] + oe[2][1] * e[1][3] + oe[2][2] * e[2][3] + oe[2][3] * e[3][3]
				},
				{
					oe[3][0] * e[0][0] + oe[3][1] * e[1][0] + oe[3][2] * e[2][0] + oe[3][3] * e[3][0],
					oe[3][0] * e[0][1] + oe[3][1] * e[1][1] + oe[3][2] * e[2][1] + oe[3][3] * e[3][1],
					oe[3][0] * e[0][2] + oe[3][1] * e[1][2] + oe[3][2] * e[2][2] + oe[3][3] * e[3][2],
					oe[3][0] * e[0][3] + oe[3][1] * e[1][3] + oe[3][2] * e[2][3] + oe[3][3] * e[3][3]
				}
			} );
	}

	/**
	 * この行列の逆行列を返します。
	 * @return 逆行列
	 */
	public TransformMatrix inverse() {
		Matrix mat = Matrix.create( new double[][]{
				m_elements[0], m_elements[1], m_elements[2], m_elements[3]
			} );
		// 逆行列を算出( Ax = E )
		mat = mat.solve( Matrix.identity( 4 ) );
		return new TransformMatrix(  mat.elements() );
	}

	/**
	 * 平行移動変換を行います。
	 * @param _x x方向の移動量
	 * @param _y y方向の移動量
	 * @param _z z方向の移動量
	 * @return 平行移動変換後の相似変換行列
	 */
	public TransformMatrix translate( double _x, double _y, double _z ) {
		return new TransformMatrix( new double[][]{
				{ m_elements[0][0], m_elements[0][1], m_elements[0][2], m_elements[0][3] + _x },
				{ m_elements[1][0], m_elements[1][1], m_elements[1][2], m_elements[1][3] + _y },
				{ m_elements[2][0], m_elements[2][1], m_elements[2][2], m_elements[2][3] + _z },
				{ m_elements[3][0], m_elements[3][1], m_elements[3][2], 1 }
			} );
	}

	/**
	 * 平行移動変換を行います。
	 * @param _v 移動量、移動方向を表すベクトル
	 * @return 平行移動変換後の相似変換行列
	 */
	public TransformMatrix translate( Vector _v ) {
		return translate( _v.x(), _v.y(), _v.z() );
	}

	/**
	 * 回転変換を行います。
	 * @param _angle 回転角
	 * @param _axis 回転軸
	 * @return 回転変換後の相似変換行列
	 */
	public TransformMatrix rotate( double _angle, Vector _axis ) {
		return product( TransformMatrix.rotation( _angle, _axis ) );
	}

	/**
	 * 指定された軸による回転変換を行います。
	 * @param _angle 回転量（単位：radian）
	 * @param _axis 回転軸
	 * @param _center 回転の中心点
	 * @return 回転変換後の相似変換行列
	 */
	public TransformMatrix rotate( double _angle, Vector _axis, Point _center ) {
		Vector move = Vector.createXYZ( _center.x(), _center.y(), _center.z() );
		return product( TransformMatrix.translation( move.reverse() ) ).product( TransformMatrix.rotation( _angle, _axis ) ).product( TransformMatrix.translation( move ) );
	}

	/**
	 * 拡大縮小変換を行います。
	 * @param _ratio 拡大縮小率
	 * @return 回転変換後の相似変換行列
	 */
	public TransformMatrix scale( double _ratio ) {
		return product( TransformMatrix.scaling( _ratio ) );
	}

	/**
	 * 拡大縮小変換を行います。
	 * @param _ratio 拡大縮小率
	 * @param _center 拡大縮小の中心点
	 * @return 拡大縮小変換後の相似変換行列
	 */
	public TransformMatrix scale( double _ratio, Point _center ) {
		Vector move = Vector.createXYZ( _center.x(), _center.y(), _center.z() );
		return product( TransformMatrix.translation( move.reverse() ) ).product( TransformMatrix.scaling( _ratio ) ).product( TransformMatrix.translation( move ) );
	}

	/**
	 * この変換行列を平行移動行列化します。
	 * @return 平行移動行列化後の相似変換行列
	 */
	public TransformMatrix translatalize() {
		return new TransformMatrix( new double[][]{
				{ 1, 0, 0, m_elements[0][3] },
				{ 0, 1, 0, m_elements[1][3] },
				{ 0, 0, 1, m_elements[2][3] },
				{ 0, 0, 0, 1 }
			} );
	}

	/**
	 * この変換行列を回転行列化します。
	 * @return 回転行列化後の相似変換行列
	 */
	public TransformMatrix rotatalize() {
		// サイズの逆数
		double inverseSize = 1.0 / scalalize();
		double[][] e = m_elements;
		return new TransformMatrix( new double[][]{
			{ e[0][0] * inverseSize, e[0][1] * inverseSize, e[0][2] * inverseSize, 0 },
			{ e[1][0] * inverseSize, e[1][1] * inverseSize, e[1][2] * inverseSize, 0 },
			{ e[2][0] * inverseSize, e[2][1] * inverseSize, e[2][2] * inverseSize, 0 },
			{ 0, 0, 0, 1 }
		} );
	}

	/**
	 * この変換行列の拡大縮小率を取得します。
	 * @return 拡大縮小率
	 */
	public double scalalize() {
		double[][] e = m_elements;
		return Math.sqrt( e[0][0] * e[0][0] + e[0][1] * e[0][1] + e[0][2] * e[0][2] );
	}

	/**
	 * この TransformMatrix と指定された Object が等しいかどうかを比較します。
	 * @param obj この TransformMatrix と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 行列の要素がまったく同じ TransformMatrix である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final TransformMatrix other = (TransformMatrix) obj;

		return Arrays.deepEquals( this.m_elements, other.m_elements );
	}

	/**
	 * この TransformMatrix のハッシュコードを返します。
	 * @return この TransformMatrix のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41 * hash + Arrays.deepHashCode( this.m_elements );
		return hash;
	}

	/**
	 * この TransformMatrix の文字列表現を返します。
	 * @return 行列の要素を表す String
	 */
	@Override
	public String toString() {
		return String.format( "elements:%s", Arrays.deepToString( m_elements ) );
	}
	
	/** 行列（本体） */
	private final double[][] m_elements;
	/** 無変換行列 */
	private static volatile TransformMatrix c_identity = null;
}