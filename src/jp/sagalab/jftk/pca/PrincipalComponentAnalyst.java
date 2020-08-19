package jp.sagalab.jftk.pca;

import java.util.Stack;
import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;

/**
 * 主成分分析を行うためのクラスです。
 * @author kaneko
 */
public final class PrincipalComponentAnalyst {

	/**
	 * 主成分分析を行います。
	 * @param _points 点列
	 * @return 主成分軸の配列
	 */
	public static Vector[] analyze( Point[] _points ) {
		// 分散共分散行列生成
		Matrix varCovMatrix = createVarianceCovarianceMatrix( _points );
		// 分散共分散行列より固有値・固有ベクトルを算出
		Matrix[] eigen = calcEigen( varCovMatrix );

		// 主成分軸をベクトルとして格納
		int rowSize = eigen[1].rowSize();
		Vector[] vectors = new Vector[ rowSize ];
		for ( int i = 0; i < rowSize; ++i ) {
			double x = eigen[1].get( 0, i );
			double y = eigen[1].get( 1, i );
			double z = eigen[1].get( 2, i );
			vectors[i] = Vector.createXYZ( x, y, z );
		}

		return vectors;
	}

	/**
	 * 指定された対称行列の固有値・固有ベクトルを求めます。
	 * 返り値のMatrix配列の一つ目の要素に固有値が格納され、二つ目の要素に固有ベクトルが格納されます。
	 * @param _matrix 対称行列
	 * @return 固有値・固有ベクトル
	 * ただし，固有値は対角成分に格納されます。また，固有ベクトルは列ベクトルとして格納されます。
	 * @throws IllegalArgumentException 指定した行列が正方行列でない場合
	 * @throws IllegalArgumentException 指定した行列の要素が対称でない場合
	 */
	public static Matrix[] calcEigen( Matrix _matrix ) {
		// Jacobi法によって固有値・固有ベクトルを求める
		
		int rowSize = _matrix.rowSize();
		int columnSize = _matrix.columnSize();
		if ( rowSize != columnSize ) {
			throw new IllegalArgumentException( "not square matrix." );
		}
		for ( int i = 0; i < rowSize; ++i ) {
			for ( int j = 0; j < columnSize; ++j ) {
				if ( _matrix.get( i, j ) != _matrix.get( j, i ) ) {
					throw new IllegalArgumentException( "not real symmetric matrix." );
				}
			}
		}

		double[][] elements = _matrix.elements();
		double[][] orthogonalElements = Matrix.identity( rowSize ).elements();
		double maxElement;
		do {
			// 非対角成分の中で絶対値が最大の成分のインデックスを取得する
			maxElement = Double.NEGATIVE_INFINITY;
			int rowIndex = -1;
			int columnIndex = -1;
			for ( int i = 0; i < rowSize; ++i ) {
				for ( int j = i + 1; j < columnSize; ++j ) {
					if ( maxElement < Math.abs( elements[i][j] ) ) {
						maxElement = Math.abs( elements[i][j] );
						rowIndex = i;
						columnIndex = j;
					}
				}
			}

			double alpha = ( elements[rowIndex][rowIndex] - elements[columnIndex][columnIndex] ) / 2.0;
			double beta = -elements[rowIndex][columnIndex];
			double gamma = Math.abs( alpha ) / Math.sqrt( alpha * alpha + beta * beta );

			double sin = Math.sqrt( ( 1.0 - gamma ) / 2.0 );
			double cos = Math.sqrt( ( 1.0 + gamma ) / 2.0 );
			if ( alpha * beta < 0.0 ) {
				sin = -sin;
			}

			// 対称行列の更新
			double mrr = elements[rowIndex][rowIndex] * cos * cos
				+ elements[columnIndex][columnIndex] * sin * sin
				- 2.0 * elements[rowIndex][columnIndex] * sin * cos;
			double mrc = ( elements[rowIndex][rowIndex] - elements[columnIndex][columnIndex] ) * sin * cos
				+ elements[rowIndex][columnIndex] * ( cos * cos - sin * sin );
			double mcr = mrc;
			double mcc = elements[rowIndex][rowIndex] * sin * sin
				+ elements[columnIndex][columnIndex] * cos * cos
				+ 2.0 * elements[rowIndex][columnIndex] * sin * cos;

			for ( int i = 0; i < columnSize; ++i ) {
				double tmp = elements[rowIndex][i] * cos - elements[columnIndex][i] * sin;
				elements[columnIndex][i] = elements[rowIndex][i] * sin + elements[columnIndex][i] * cos;
				elements[rowIndex][i] = tmp;
			}
			for ( int i = 0; i < rowSize; ++i ) {
				elements[i][rowIndex] = elements[rowIndex][i];
				elements[i][columnIndex] = elements[columnIndex][i];
			}
			elements[rowIndex][rowIndex] = mrr;
			elements[rowIndex][columnIndex] = mrc;
			elements[columnIndex][rowIndex] = mcr;
			elements[columnIndex][columnIndex] = mcc;

			// 直交行列の更新
			for ( int i = 0; i < rowSize; ++i ) {
				double tmp = orthogonalElements[i][rowIndex] * cos - orthogonalElements[i][columnIndex] * sin;
				orthogonalElements[i][columnIndex] = orthogonalElements[i][rowIndex] * sin
					+ orthogonalElements[i][columnIndex] * cos;
				orthogonalElements[i][rowIndex] = tmp;
			}
		} while ( maxElement >= ERROR_TOLERANCE );

		return sortEigen( Matrix.create( elements ), Matrix.create( orthogonalElements ) );
	}

	/**
	 * 指定された対称行列の固有値を求めます。
	 * @param _matrix 対称行列
	 * @return 固有値を対角成分にもつ行列
	 * @throws IllegalArgumentException 指定した行列が正方行列でない場合
	 * @throws IllegalArgumentException 指定した行列の要素が対称でない場合
	 */
	public static Matrix calcEigenValues( Matrix _matrix ) {
		// QR法によって固有値を求める
		int rowSize = _matrix.rowSize();
		int columnSize = _matrix.columnSize();
		if ( rowSize != columnSize ) {
			throw new IllegalArgumentException( "not square matrix." );
		}
		for ( int i = 0; i < rowSize; ++i ) {
			for ( int j = 0; j < columnSize; ++j ) {
				if ( _matrix.get( i, j ) != _matrix.get( j, i ) ) {
					throw new IllegalArgumentException( "not real symmetric matrix." );
				}
			}
		}

		// 指定された対称行列を三重対角化
		Matrix matrix = diagonalizeTriplicity( _matrix );

		Matrix identify = Matrix.identity( rowSize );
		double[][] upperElements = matrix.elements();
		int nowSize = rowSize;
		while ( nowSize > 1 ) {
			double[][] orthogonalElements = identify.elements();

			// 収束を早めるために右下の2x2行列の固有値で対角成分を減算
			final double eigen = calcEigenValue( upperElements[nowSize - 2][nowSize - 2], upperElements[nowSize - 2][nowSize - 1],
				upperElements[nowSize - 1][nowSize - 2], upperElements[nowSize - 1][nowSize - 1] );
			for ( int i = 0; i < nowSize; ++i ) {
				upperElements[i][i] -= eigen;
			}

			for ( int i = 0; i < nowSize - 1; ++i ) {
				double denominator = Math.sqrt( upperElements[i][i] * upperElements[i][i]
					+ upperElements[i + 1][i] * upperElements[i + 1][i] );
				double sin = upperElements[i + 1][i] / denominator;
				double cos = upperElements[i][i] / denominator;

				int n = nowSize < i + 3 ? nowSize : i + 3;
				for ( int j = i; j < n; ++j ) {
					double tmp = -upperElements[i][j] * sin + upperElements[i + 1][j] * cos;
					upperElements[i][j] = upperElements[i][j] * cos + upperElements[i + 1][j] * sin;
					upperElements[i + 1][j] = tmp;
				}
				upperElements[i + 1][i] = 0.0;

				for ( int j = 0; j < i + 2; ++j ) {
					double tmp = -orthogonalElements[j][i] * sin + orthogonalElements[j][i + 1] * cos;
					orthogonalElements[j][i] = orthogonalElements[j][i] * cos + orthogonalElements[j][i + 1] * sin;
					orthogonalElements[j][i + 1] = tmp;
				}
			}
			Matrix upper = Matrix.create( upperElements );
			Matrix orthogonal = Matrix.create( orthogonalElements );
			matrix = upper.product( orthogonal );

			// 上で右下の2x2行列の固有値で対角成分を減算した分を加算
			upperElements = matrix.elements();
			for ( int i = 0; i < nowSize; ++i ) {
				upperElements[i][i] += eigen;
			}

			if ( Math.abs( upperElements[nowSize - 1][nowSize - 2] ) < ERROR_TOLERANCE ) {
				--nowSize;
			}
		}

		//固有値のみを返す
		Matrix[] sorted = sortEigen( Matrix.create( upperElements ), null );
		return sorted[0];
	}
	
	/**
	 * 指定された点列の平均座標を求めます。
	 * @param _points 点列
	 * @return 平均座標
	 */
	public static Point calcExpectation( Point[] _points ) {
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		for ( Point p : _points ) {
			x += p.x();
			y += p.y();
			z += p.z();
		}

		return Point.createXYZ( x / _points.length, y / _points.length, z / _points.length );
	}
	
	/**
	 * 相関行列を生成します。
	 * @param _points 点列
	 * @return 相関行列
	 * @throws IllegalArgumentException 指定された点列の要素数が0の場合
	 */
	public static Matrix createCorrelationMatrix( Point[] _points ) {
		if ( _points.length == 0 ) {
			throw new IllegalArgumentException( "_points is empty." );
		}
		// 平均座標
		Point expectation = calcExpectation( _points );
		// 各軸要素の偏差の自乗和平方根
		Point deviation = calcDeviationSquareRootOfSumOfSquares( _points, expectation );

		// 相関行列
		double[][] correlation = new double[ 3 ][ 3 ];

		// 各軸要素の相関係数を算出し，行列の下三角に格納
		correlation[1][0] = calcCorrelationCoefficientXY( _points, expectation, deviation );
		correlation[2][0] = calcCorrelationCoefficientZX( _points, expectation, deviation );
		correlation[2][1] = calcCorrelationCoefficientYZ( _points, expectation, deviation );

		double[] devArray = new double[]{ deviation.x(), deviation.y(), deviation.z() };
		// 下三角行列の成分を上三角行列の成分にコピー
		for ( int i = 0; i < correlation.length; ++i ) {
			correlation[i][i] = ( devArray[i] > 0.0 ) ? 1.0 : 0.0;
			for ( int j = i + 1; j < correlation[i].length; ++j ) {
				correlation[i][j] = correlation[j][i];
			}
		}

		return Matrix.create( correlation );
	}

	
	/**
	 * 分散共分散行列を生成します。
	 * @param _points 点列
	 * @return 分散共分散行列
	 * @throws IllegalArgumentException 指定された点列の要素数が0の場合
	 */
	public static Matrix createVarianceCovarianceMatrix( Point[] _points ) {
		if ( _points.length == 0 ) {
			throw new IllegalArgumentException( "_points is empty." );
		}
		// 平均座標
		Point expectation = calcExpectation( _points );

		// 分散共分散行列
		double[][] varCovElem = new double[ 3 ][ 3 ];
		// 各軸要素の共分散を算出し，行列の下三角行列に格納
		varCovElem[0][0] = calcCovarianceXX( _points, expectation );
		varCovElem[1][0] = calcCovarianceXY( _points, expectation );
		varCovElem[2][0] = calcCovarianceZX( _points, expectation );
		varCovElem[1][1] = calcCovarianceYY( _points, expectation );
		varCovElem[2][1] = calcCovarianceYZ( _points, expectation );
		varCovElem[2][2] = calcCovarianceZZ( _points, expectation );
		// 下三角行列の成分を上三角行列の成分にコピー
		for ( int i = 0; i < varCovElem.length; ++i ) {
			for ( int j = i + 1; j < varCovElem[i].length; ++j ) {
				varCovElem[i][j] = varCovElem[j][i];
			}
		}

		return Matrix.create( varCovElem );
	}
	
	/**
	 * 行交換を行います。
	 * @param _elements 行列要素
	 * @param _i 行番号
	 * @param _j 行番号
	 * @return 交換済行列要素
	 */
	private static double[][] swapRowElements( double[][] _elements, int _i, int _j ) {
		double[][] result = _elements.clone();
		for ( int i = 0; i < result.length; ++i ) {
			result[i] = _elements[i].clone();
		}

		double[] tmpRow = result[_j];
		result[_j] = result[_i];
		result[_i] = tmpRow;

		return result;
	}

	/**
	 * 列交換を行います。
	 * @param _elements 行列要素
	 * @param _i 列番号
	 * @param _j 列番号
	 * @return 交換済行列要素
	 */
	private static double[][] swapColumnElements( double[][] _elements, int _i, int _j ) {
		double[][] result = _elements.clone();
		for ( int i = 0; i < result.length; ++i ) {
			result[i] = _elements[i].clone();
		}

		for ( int k = 0; k < result.length; ++k ) {
			double tmp = result[k][_j];
			result[k][_j] = result[k][_i];
			result[k][_i] = tmp;
		}

		return result;
	}

	/**
	 * 指定された固有値・固有ベクトルを固有値の降順でソートします。
	 * 返り値のMatrix配列の一つ目の要素に固有値が格納され、二つ目の要素に固有ベクトルが格納されます。
	 * @param _eigenValues 固有値
	 * @param _eigenVectors 固有ベクトル
	 * @return ソートされた固有値・固有ベクトル
	 */
	private static Matrix[] sortEigen( Matrix _eigenValues, Matrix _eigenVectors ) {
		int rowSize = _eigenValues.rowSize();

		Stack<int[]> stack = new Stack<int[]>();

		double[][] eigenValues = _eigenValues.elements();
		double[][] eigenVectors = _eigenVectors != null ? _eigenVectors.elements() : null;
		int left = 0;
		int right = rowSize - 1;
		while ( true ) {
			while ( left < right ) {
				int center = ( left + right ) / 2;
				double pivot = eigenValues[center][center];
				int i = left;
				int j = right;
				while ( i < j ) {
					while ( eigenValues[i][i] > pivot ) {
						++i;
					}
					while ( eigenValues[j][j] < pivot ) {
						--j;
					}
					if ( i < j ) {
						// スワップ
						eigenValues = swapRowElements( eigenValues, i, j );
						eigenValues = swapColumnElements( eigenValues, i, j );
						if ( eigenVectors != null ) {
							eigenVectors = swapColumnElements( eigenVectors, i, j );
						}
						++i;
						--j;
					}
				}
				stack.push( new int[]{ j + 1, right } );
				right = i - 1;
			}
			
			if(stack.empty()){
				break;
			}
			int[] indexes = stack.pop();
			left = indexes[0];
			right = indexes[1];
		}

		return new Matrix[]{
				Matrix.create( eigenValues ),
				( eigenVectors != null ) ? Matrix.create( eigenVectors ) : null
			};
	}

	/**
	 * 指定された要素の2x2行列の固有値を求めます。
	 * @param _m00 1行1列目の要素
	 * @param _m01 1行2列目の要素
	 * @param _m10 2行1列目の要素
	 * @param _m11 2行2列目の要素
	 * @return 固有値
	 */
	private static double calcEigenValue( double _m00, double _m01, double _m10, double _m11 ) {
		// 2次方程式の解の公式によって2x2行列の固有値を求める
		double diagonalSum = _m00 + _m11;
		double val = diagonalSum * diagonalSum - 4 * ( _m00 * _m11 - _m01 * _m10 );
		val = Math.sqrt( Math.max( 0.0, val ) );
		
		double eig0 = ( diagonalSum + val ) / 2.0;
		double eig1 = ( diagonalSum - val ) / 2.0;

		// 2x2行列の右下の要素に近い方の値を返す
		if ( Math.abs( _m11 - eig0 ) < Math.abs( _m11 - eig1 ) ) {
			return eig0;
		} else {
			return eig1;
		}
	}

	/**
	 * 指定された対称行列を三重対角化します。
	 * @param _matrix 対称行列
	 * @return 三重対角化した行列
	 */
	private static Matrix diagonalizeTriplicity( Matrix _matrix ) {
		// Householder変換を利用して，指定された対称行列を三重対角化する
		
		int rowSize = _matrix.rowSize();
		int columnSize = _matrix.columnSize();

		double[][] elements = _matrix.elements();
		for ( int i = 0; i < rowSize - 1; ++i ) {
			double[] vectorElements = new double[ columnSize - i - 1 ];
			for ( int j = i + 1; j < columnSize; ++j ) {
				vectorElements[j - i - 1] = elements[i][j];
			}
			Matrix transformMatrix = createHouseholderTransformationMatrix( vectorElements );
			Matrix vector = Matrix.create( new double[][]{ vectorElements } ).transpose();
			Matrix transformed = transformMatrix.product( vector );

			elements[i][i + 1] = elements[i + 1][i] = transformed.get( 0, 0 );
			for ( int j = i + 2; j < rowSize; ++j ) {
				elements[i][j] = elements[j][i] = 0.0;
			}

			Matrix subMatrix = createSubMatrix( elements, i + 1, i + 1 );
			Matrix transformedSub = transformMatrix.product( subMatrix ).product( transformMatrix );
			for ( int j = i + 1; j < rowSize; ++j ) {
				for ( int k = i + 1; k < columnSize; ++k ) {
					elements[j][k] = transformedSub.get( j - i - 1, k - i - 1 );
				}
			}
		}

		return Matrix.create( elements );
	}

	/**
	 * 指定された行列の要素から右下の小行列を新しい行列として生成します。
	 * @param _elements 行列要素
	 * @param _i 行番号
	 * @param _j 列番号
	 * @return 小行列
	 */
	private static Matrix createSubMatrix( double[][] _elements, int _i, int _j ) {
		double[][] elements = new double[ _elements.length - _i ][ _elements.length - _j ];

		for ( int i = _i; i < _elements.length; ++i ) {
			for ( int j = _j; j < _elements.length; ++j ) {
				elements[i - _i][j - _i] = _elements[i][j];
			}
		}

		return Matrix.create( elements );
	}

	/**
	 * 指定されたベクトルをHouseholder変換によって，
	 * 最初の要素しか値を持たないベクトルへ変換するための変換行列を生成します。
	 * @param _vector ベクトル
	 * @return 変換行列
	 */
	private static Matrix createHouseholderTransformationMatrix( double[] _vector ) {
		double squareNorm = calcSquareNorm( _vector );
		double norm = Math.sqrt( squareNorm );
		// 計算精度向上のために変換後のベクトルの第一成分の絶対値が0から離れるように符号を設定
		if ( _vector[0] < 0.0 ) {
			norm *= -1;
		}

		double weight = Math.sqrt( 2.0 * ( squareNorm + _vector[0] * norm ) );
		double[] vector = _vector.clone();
		vector[0] += norm;
		for ( int i = 0; i < vector.length; ++i ) {
			vector[i] /= weight;
			if ( Double.isNaN( vector[i] ) ) {
				return Matrix.identity( vector.length );
			}
		}

		double[][] matrix = new double[ vector.length ][ vector.length ];
		for ( int i = 0; i < matrix.length; ++i ) {
			matrix[i][i] = 1.0;
			for ( int j = 0; j < matrix[i].length; ++j ) {
				matrix[i][j] -= 2.0 * vector[i] * vector[j];
			}
		}

		return Matrix.create( matrix );
	}

	/**
	 * 指定されたベクトルの自乗ノルムを求めます。
	 * @param _vector ベクトル
	 * @return 自乗ノルム
	 */
	private static double calcSquareNorm( double[] _vector ) {
		double sum = 0.0;

		for ( double component : _vector ) {
			sum += component * component;
		}

		return sum;
	}

	/**
	 * 指定された点列の各成分の偏差の自乗和平方根を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 各成分の偏差の自乗和平方根
	 */
	private static Point calcDeviationSquareRootOfSumOfSquares( Point[] _points, Point _expectation ) {
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		for ( Point p : _points ) {
			x += ( p.x() - _expectation.x() ) * ( p.x() - _expectation.x() );
			y += ( p.y() - _expectation.y() ) * ( p.y() - _expectation.y() );
			z += ( p.z() - _expectation.z() ) * ( p.z() - _expectation.z() );
		}

		return Point.createXYZ( Math.sqrt( x ), Math.sqrt( y ), Math.sqrt( z ) );
	}

	/**
	 * 指定された点列のx成分とy成分の相関係数を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @param _deviation 偏差の自乗和平方根
	 * @return x成分とy成分の相関係数
	 */
	private static double calcCorrelationCoefficientXY( Point[] _points, Point _expectation, Point _deviation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.x() - _expectation.x() ) * ( p.y() - _expectation.y() ) );
		}

		double result = cov / ( _deviation.x() * _deviation.y() );
		if ( Double.isNaN( result ) ) {
			result = 0.0;
		}

		return result;
	}

	/**
	 * 指定された点列のy成分とz成分の相関係数を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @param _deviation 偏差の自乗和平方根
	 * @return y成分とz成分の相関係数
	 */
	private static double calcCorrelationCoefficientYZ( Point[] _points, Point _expectation, Point _deviation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.y() - _expectation.y() ) * ( p.z() - _expectation.z() ) );
		}

		double result = cov / ( _deviation.y() * _deviation.z() );
		if ( Double.isNaN( result ) ) {
			result = 0.0;
		}

		return result;
	}

	/**
	 * 指定された点列のz成分とy成分の相関係数を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @param _deviation 偏差の自乗和平方根
	 * @return z成分とy成分の相関係数
	 */
	private static double calcCorrelationCoefficientZX( Point[] _points, Point _expectation, Point _deviation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.z() - _expectation.z() ) * ( p.x() - _expectation.x() ) );
		}

		double result = cov / ( _deviation.z() * _deviation.x() );
		if ( Double.isNaN( result ) ) {
			result = 0.0;
		}

		return result;
	}

	/**
	 * 指定された点列のx成分の分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 分散
	 */
	private static double calcCovarianceXX( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			double deviation = p.x() - _expectation.x();
			cov += deviation * deviation;
		}

		return cov / _points.length;
	}

	/**
	 * 指定された点列のy成分の分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 分散
	 */
	private static double calcCovarianceYY( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			double deviation = p.y() - _expectation.y();
			cov += deviation * deviation;
		}

		return cov / _points.length;
	}

	/**
	 * 指定された点列のz成分の分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 分散
	 */
	private static double calcCovarianceZZ( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			double deviation = p.z() - _expectation.z();
			cov += deviation * deviation;
		}

		return cov / _points.length;
	}

	/**
	 * 指定された点列のx成分とy成分の共分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 共分散
	 */
	private static double calcCovarianceXY( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.x() - _expectation.x() ) * ( p.y() - _expectation.y() ) );
		}

		return cov / _points.length;
	}

	/**
	 * 指定された点列のy成分とz成分の共分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 共分散
	 */
	private static double calcCovarianceYZ( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.y() - _expectation.y() ) * ( p.z() - _expectation.z() ) );
		}

		return cov / _points.length;
	}

	/**
	 * 指定された点列のz成分とx成分の共分散を求めます。
	 * @param _points 点列
	 * @param _expectation 平均座標
	 * @return 共分散
	 */
	private static double calcCovarianceZX( Point[] _points, Point _expectation ) {
		double cov = 0.0;

		for ( Point p : _points ) {
			cov += ( ( p.z() - _expectation.z() ) * ( p.x() - _expectation.x() ) );
		}

		return cov / _points.length;
	}

	private PrincipalComponentAnalyst() {
		throw new UnsupportedOperationException("can not create instance.");
	}
	
	/** 収束許容誤差 */
	private static final double ERROR_TOLERANCE = 1.0E-6;
}