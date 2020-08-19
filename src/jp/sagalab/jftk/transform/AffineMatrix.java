package jp.sagalab.jftk.transform;

import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;

/**
 * アフィン変換行列を表すクラスです。
 * @author watanabe
 */
public final class AffineMatrix extends TransformMatrix{

	/**
	 * アフィン変換行列を生成します。
	 * <p>
	 * アフィン変換前後の3点の組を与える事で、
	 * 変換行列を計算します。
	 * </p>
	 * @param _before 変換前の3点
	 * @param _after 変換後の3点
	 * @return アフィン変換行列
	 * @throws IllegalArgumentException 変換前、および変換後の点列数が3点ではない場合
	 */
	public static AffineMatrix createBy3Points( Point[] _before, Point[] _after ) {
		if ( _before.length != 3 ) {
			throw new IllegalArgumentException( "_before.length must be 3 : _before.length = " + _before.length );
		}
		if ( _after.length != 3 ) {
			throw new IllegalArgumentException( "_after.length must be 3 : _after.length = " + _after.length );
		}
		// 変換前、変換後の点列から行列を生成
		Matrix beforeMatrix = createMatrixBy3Points( _before );
		Matrix afterMatrix = createMatrixBy3Points( _after );
		// (beforeMatrix) * (X^T) = (afterMatrix)を解いて変換行列を算出 
		Matrix result = beforeMatrix.solve( afterMatrix );

		if ( result != null ) {
			return new AffineMatrix( result.transpose().elements() );
		} else {
			return null;
		}
	}

	/**
	 * 3点から4x4の行列を生成します。
	 * @param _points 3点
	 * @return 3点と、その外積ベクトルを含む行列
	 */
	private static Matrix createMatrixBy3Points( Point[] _points ) {
		Vector zeroToOne = Vector.createSE( _points[0], _points[1] );
		Vector zeroToTwo = Vector.createSE( _points[0], _points[2] );
		// 外積ベクトル（引数の3点と同一平面にない点を計算する）
		Vector cross = zeroToOne.cross( zeroToTwo );
		Point np = _points[0].move( cross );
		return Matrix.create( new double[][]{
			{ _points[0].x(), _points[0].y(), _points[0].z(), 1.0 },
			{ _points[1].x(), _points[1].y(), _points[1].z(), 1.0 },
			{ _points[2].x(), _points[2].y(), _points[2].z(), 1.0 },
			{ np.x(), np.y(), np.z(), 1.0 }
		} );
	}

	private AffineMatrix( double[][] _elements ) {
		super( _elements );
	}
}
