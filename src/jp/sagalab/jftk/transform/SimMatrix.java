package jp.sagalab.jftk.transform;

import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;

/**
 * 軸指定により姿勢行列を生成します。
 * @author yamaguchi
 */
public class SimMatrix extends TransformMatrix {
	
	/**
	 * 軸指定により姿勢行列を生成します。
	 * <p>
	 * X軸・Y軸を表すベクトルの大きさ成分は無視され、二つの軸が直交していなかった場合にはX軸が優先されます。
	 * </p>
	 * @param _xAxis X軸方向のベクトル
	 * @param _yAxis Y軸方向のベクトル
	 * @return 姿勢行列
	 * @throws IllegalArgumentException XY軸が同じか真逆の向きの場合
	 */
	public static SimMatrix createByAxis( Vector _xAxis, Vector _yAxis ) {
		Vector xAxis = _xAxis.normalize();
		if(Double.isInfinite( 1 / xAxis.length())){
			xAxis = Vector.createXYZ( 1, 0, 0 );
		}
		
		if(Double.isInfinite( 1 / _yAxis.length())){
			_yAxis = Vector.createXYZ( 0, 1, 0 );
		}
		
		Vector zAxis = xAxis.cross( _yAxis ).normalize();
		if(Double.isInfinite( 1 / zAxis.length())){
			throw new IllegalArgumentException( String.format( "zAxis:%s", zAxis ) );
		}
		
		Vector yAxis = zAxis.cross( xAxis ).normalize();
		
		double[][] elem = new double[][]{
			{ xAxis.x(), yAxis.x(), zAxis.x(), 0 },
			{ xAxis.y(), yAxis.y(), zAxis.y(), 0 },
			{ xAxis.z(), yAxis.z(), zAxis.z(), 0 },
			{ 0, 0, 0, 1 }
		};
		return new SimMatrix( elem );
	}

	/**
	 * 変換行列を生成します。
	 * <p>
	 * ある点ABをA'B'に変換するような相似変換行列を生成します。
	 * 2点を軸とするねじれ方向の回転量はABに直交するベクトルNと
	 * A'B'に直交するベクトルN'から得ます。
	 * </p>
	 * @param _beforeA 変換前の点A
	 * @param _beforeB 変換前の点B
	 * @param _beforeNormal ABに直交するベクトルN
	 * @param _afterA 変換後の点A'
	 * @param _afterB 変換後の点B'
	 * @param _afterNormal A'B'に直交するベクトルN'
	 * @return 変換行列
	 */
	public static SimMatrix createByBeforeAfterPoints( Point _beforeA, Point _beforeB, Vector _beforeNormal, Point _afterA, Point _afterB, Vector _afterNormal ) {
		// 変換前の点から行列生成
		Vector beforeAB = Vector.createSE( _beforeA, _beforeB );
		double beforeABLength = beforeAB.length();
		Vector beforeAT = beforeAB.cross( _beforeNormal ).normalize().magnify( beforeABLength );
		Vector beforeAN = beforeAT.cross( beforeAB ).normalize().magnify( beforeABLength );
		Matrix before = Matrix.create( new double[][]{
				{ beforeAB.x(), beforeAB.y(), beforeAB.z() },
				{ beforeAN.x(), beforeAN.y(), beforeAN.z() },
				{ beforeAT.x(), beforeAT.y(), beforeAT.z() }
			} );

		// 変換後の点から行列生成
		Vector afterAB = Vector.createSE( _afterA, _afterB );
		double afterABLegnth = afterAB.length();
		Vector afterAT = afterAB.cross( _afterNormal ).normalize().magnify( afterABLegnth );
		Vector afterAN = afterAT.cross( afterAB ).normalize().magnify( afterABLegnth );
		Matrix after = Matrix.create( new double[][]{
				{ afterAB.x(), afterAB.y(), afterAB.z() },
				{ afterAN.x(), afterAN.y(), afterAN.z() },
				{ afterAT.x(), afterAT.y(), afterAT.z() }
			} );

		// 変換行列の生成
		Matrix result = before.solve( after );
		if ( result == null ) {
			return null;
		}
		double[][] e = result.elements();
		
		// 移動量の算出
		double dx = _afterA.x() - 
			( e[0][0] * _beforeA.x() + e[1][0] * _beforeA.y() + e[2][0] * _beforeA.z() );
		double dy = _afterA.y() - 
			( e[0][1] * _beforeA.x() + e[1][1] * _beforeA.y() + e[2][1] * _beforeA.z() );
		double dz = _afterA.z() - 
			( e[0][2] * _beforeA.x() + e[1][2] * _beforeA.y() + e[2][2] * _beforeA.z() );

		return new SimMatrix( new double[][]{
				{ e[0][0], e[1][0], e[2][0], dx },
				{ e[0][1], e[1][1], e[2][1], dy },
				{ e[0][2], e[1][2], e[2][2], dz },
				{ 0, 0, 0, 1 }
			} );
	}

	private SimMatrix( double[][] _elements ) {
		super( _elements );
	}
}