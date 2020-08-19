package jp.sagalab.jftk.fuzzysplinecurve;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jp.sagalab.jftk.Matrix;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.ParametricEvaluable.EvaluationType;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.interporation.SplineCurveInterpolator;

/**
 * ファジィスプライン曲線を生成するためのクラスです。
 */
public final class FuzzySplineCurveCreater{

	/**
	 * 指定されたスプライン曲線からファジィスプライン曲線を生成します。
	 *
	 * @param _spline スプライン曲線
	 * @param _vCoeff 速度係数
	 * @param _aCoeff 加速度係数
	 * @return ファジィスプライン曲線
	 */
	public static SplineCurve create( SplineCurve _spline, double _vCoeff, double _aCoeff ) {
		// 入力点列にnullが混入していないかチェック
		if ( _spline == null ) {
			throw new IllegalArgumentException();
		}
		SplineCurve vCurve = _spline.differentiate();
		SplineCurve aCurve = vCurve.differentiate();

		Point origin = Point.createXYZ( 0, 0, 0 );
		int degree = _spline.degree();
		double[] knots = _spline.knots();
		Range range = Range.create( knots[degree - 1], knots[knots.length - degree] );
		SplineCurve spline = SplineCurve.create( degree, _spline.controlPoints(), knots, range );
		Point[] points = spline.evaluateAll( Math.max( (int) Math.ceil( range.length() / 0.01 ), 2 ),
			EvaluationType.TIME );
		for ( int i = 0; i < points.length; ++i ) {
			Point p = points[i];
			double time = p.time();
			Point vP = vCurve.evaluate( time );
			Point aP = aCurve.evaluate( time );
			double fuzziness = _vCoeff * vP.distance( origin ) + _aCoeff * aP.distance( origin );
			points[i] = Point.createXYZTF( p.x(), p.y(), p.z(), time, fuzziness );
		}

		return create( spline, points ).part( _spline.range() );
	}

	/**
	 * 指定されたスプライン曲線からファジィスプライン曲線を生成します。
	 *
	 * @param _spline スプライン曲線
	 * @param _observations ファジィスプライン曲線
	 * @return ファジィスプライン曲線
	 */
	public static SplineCurve create( SplineCurve _spline, Point[] _observations ) {
		if ( _spline == null ) {
			throw new NullPointerException( "_spline is null." );
		}
		if ( _observations == null ) {
			throw new IllegalArgumentException( "_observationalFuzziness is null." );
		}
		double[] observations = new double[_observations.length];
		for ( int i = 0; i < _observations.length; ++i ) {
			observations[i] = _observations[i].fuzziness();
		}

		// 次数
		int degree = _spline.degree();
		// 節点列
		double[] knots = _spline.knots();
		// 重み行列
		Matrix weightMatrix = SplineCurveInterpolator.createWeightMatrix( _observations, degree, knots );

		// 非負制約条件下の最小自乗法により，ファジネスを求める
		double[] fuzzinessElements = nnls( weightMatrix, observations );

		// ファジネスを制御点に付加
		Point[] cp = _spline.controlPoints();
		for ( int i = 0; i < cp.length; ++i ) {
			Point p = cp[i];
			cp[i] = Point.createXYZTF( p.x(), p.y(), p.z(), p.time(), fuzzinessElements[i] );
		}

		return SplineCurve.create( degree, cp, knots, _spline.range() );
	}

	/**
	 * 非負制約条件下で最小自乗問題を解きます。<br>
	 * PQN-NNLS(Projected Quasi-Newton NNLS)アルゴリズムの一種であるPQN-LBFGSアルゴリズムを利用します。
	 * @param _matrix 重み行列
	 * @param _observation 点列
	 * @return 最小自乗問題の解
	 */
	public static double[] nnls( Matrix _matrix, double[] _observation ) {
		int rowSize = _matrix.rowSize();
		int columnSize = _matrix.columnSize();

		Matrix transposed = _matrix.transpose();
		Matrix transposedProductMatrix = transposed.product( _matrix );
		double[][] observationalElements = new double[_observation.length][1];
		for ( int i = 0; i < _observation.length; ++i ) {
			observationalElements[i][0] = _observation[i];
		}
		Matrix observation = Matrix.create( observationalElements );
		Matrix transposedProductObservation = transposed.product( observation );

		Matrix vector = calculateInitialVector( _matrix, _observation );
		Matrix gradVector = transposedProductMatrix.product( vector ).minus( transposedProductObservation );
		Matrix directionVector = gradVector;
		double tolerance = 1.0E-14;
		// APA rule のステップ間隔を決定する数値。開区間(0, 1)に含まれる任意の数値。
		double stepRatio = 0.5;
		int maxStoredSize = 7;
		LinkedList<Matrix> vectorQueue = new LinkedList<Matrix>();
		LinkedList<Matrix> gradVectorQueue = new LinkedList<Matrix>();
		LinkedList<Matrix> transposedVectorQueue = new LinkedList<Matrix>();
		LinkedList<Matrix> transposedGradVectorQueue = new LinkedList<Matrix>();
		LinkedList<Double> denominatorQueue = new LinkedList<Double>();
		int iterateCount = 0;
		while ( iterateCount < NNLS_MAX_ITERATE_TIMES ) {
			// compute free variable set indexes
			int[] freeIndexes = searchFreeIndexes( vector, gradVector );

			// extract free variable set
			double[][] freeElements = new double[freeIndexes.length][1];
			double[][] gradFreeElements = new double[freeElements.length][1];
			double[][] freeDirectionElements = new double[freeElements.length][1];
			double[][] subMatrixElements = new double[rowSize][freeElements.length];
			for ( int i = 0; i < freeIndexes.length; ++i ) {
				int freeIndex = freeIndexes[i];
				freeElements[i][0] = vector.get( freeIndex, 0 );
				gradFreeElements[i][0] = gradVector.get( freeIndex, 0 );
				freeDirectionElements[i][0] = directionVector.get( freeIndex, 0 );
				for ( int j = 0; j < subMatrixElements.length; ++j ) {
					subMatrixElements[j][i] = _matrix.get( j, freeIndex );
				}
			}

			// the Armijo along projection arc (APA) rule
			Matrix freeVector = Matrix.create( freeElements );
			Matrix gradFreeVector = Matrix.create( gradFreeElements );
			Matrix transposedGradFreeVector = gradFreeVector.transpose();
			Matrix freeDirectionVector = Matrix.create( freeDirectionElements );
			Matrix subMatrix = Matrix.create( subMatrixElements );
			double freeError = estimateError( subMatrix, freeVector, observation );
			double element = transposedGradFreeVector.product( freeVector ).get( 0, 0 );
			double ratio = 1.0;
			Matrix projectedVector;
			do {
				projectedVector = project( freeVector.minus( freeDirectionVector.magnify( ratio ) ) );
				ratio *= stepRatio;
			} while ( !armijoRule( subMatrix, transposedGradFreeVector, observation, projectedVector, freeError, element ) );

			// update free variable set
			double[][] preElements = new double[columnSize][1];
			double[][] nextElements = new double[columnSize][1];
			for ( int i = 0; i < freeIndexes.length; ++i ) {
				int freeIndex = freeIndexes[i];
				preElements[freeIndex][0] = freeVector.get( i, 0 );
				nextElements[freeIndex][0] = projectedVector.get( i, 0 );
			}
			Matrix preVector = Matrix.create( preElements );
			Matrix nextVector = Matrix.create( nextElements );
			Matrix nextGradVector = transposedProductMatrix.product( nextVector ).minus( transposedProductObservation );

			if ( squaredNorm( projectedVector.minus( freeVector ) ) < tolerance ) {
				break;
			}

			// the limited memory BFGS (L-BFGS) method
			if ( vectorQueue.size() >= maxStoredSize ) {
				vectorQueue.poll();
				gradVectorQueue.poll();
				transposedVectorQueue.poll();
				transposedGradVectorQueue.poll();
				denominatorQueue.poll();
			}
			Matrix diffVector = nextVector.minus( preVector );
			Matrix diffGradVector = nextGradVector.minus( gradVector );
			Matrix transposedGradVector = diffGradVector.transpose();
			vectorQueue.offer( diffVector );
			gradVectorQueue.offer( diffGradVector );
			transposedVectorQueue.offer( diffVector.transpose() );
			transposedGradVectorQueue.offer( transposedGradVector );
			denominatorQueue.offer( transposedGradVector.product( diffVector ).get( 0, 0 ) );

			directionVector = calculateDirection( vectorQueue, gradVectorQueue,
				transposedVectorQueue, transposedGradVectorQueue, denominatorQueue, nextGradVector );

			gradVector = nextGradVector;
			vector = nextVector;
			++iterateCount;
		}
		if ( iterateCount >= NNLS_MAX_ITERATE_TIMES ) {
			System.err.println( "Warnning: nnls iterate count reach " + iterateCount );
		}

		double[][] resultElements = vector.transpose().elements();
		return resultElements[0];
	}

	private static int[] searchFreeIndexes( Matrix _vector, Matrix _gradVector ) {
		int rowSize = _vector.rowSize();
		List<Integer> indexList = new ArrayList<Integer>( rowSize );
		for ( int i = 0; i < rowSize; ++i ) {
			if ( !( _vector.get( i, 0 ) == 0.0 && _gradVector.get( i, 0 ) > 0.0 ) ) {
				indexList.add( i );
			}
		}
		int[] indexes = new int[indexList.size()];
		for ( int i = 0; i < indexes.length; ++i ) {
			indexes[i] = indexList.get( i );
		}
		return indexes;
	}

	private static Matrix calculateInitialVector( Matrix _matrix, double[] _observation ) {
		// NtN * d = NtP
		Matrix Nt = _matrix.transpose();
		Matrix NtN = Nt.product( _matrix );
		double[][] elements = new double[_observation.length][1];
		for ( int i = 0; i < _observation.length; ++i ) {
			elements[i][0] = _observation[i];
		}
		Matrix NtP = Nt.product( Matrix.create( elements ) );

		Matrix initialVector = NtN.solve( NtP );

		elements = new double[initialVector.rowSize()][1];
		for ( int i = 0; i < elements.length; ++i ) {
			elements[i][0] = Math.max( initialVector.get( i, 0 ), 0 );
		}
		return Matrix.create( elements );
	}

	private static double squaredNorm( Matrix _vector ) {
		int rowSize = _vector.rowSize();
		double result = 0.0;
		for ( int i = 0; i < rowSize; ++i ) {
			double element = _vector.get( i, 0 );
			result += element * element;
		}
		return result;
	}

	private static Matrix calculateDirection( List<Matrix> _vectors, List<Matrix> _gradVectors,
		List<Matrix> _transposedVectors, List<Matrix> _transposedGradVectors, LinkedList<Double> _denominators, Matrix _nextGradVector ) {
		int size = _vectors.size();
		double[] alpha = new double[size];

		Matrix direction = _nextGradVector;
		//逆順で処理を行う
		for ( int i = size - 1; i >= 0; --i ) {
			Matrix transposed = _transposedVectors.get( i );
			double element = transposed.product( direction ).get( 0, 0 );
			alpha[i] = element / _denominators.get( i );
			if ( Double.isInfinite( alpha[i] ) || Double.isNaN( alpha[i] ) ) {
				alpha[i] = 1.0;
			}
			direction = direction.minus( _gradVectors.get( i ).magnify( alpha[i] ) );
		}
		for ( int i = 0; i < size; ++i ) {
			Matrix transposed = _transposedGradVectors.get( i );
			double element = transposed.product( direction ).get( 0, 0 );
			double beta = element / _denominators.get( i );
			if ( Double.isInfinite( beta ) || Double.isNaN( beta ) ) {
				beta = 1.0;
			}
			direction = direction.plus( _vectors.get( i ).magnify( alpha[i] - beta ) );
		}

		return direction;
	}

	private static boolean armijoRule( Matrix _matrix, Matrix _transposedGradFreeMatrix,
		Matrix _observation, Matrix _direction, double _freeError, double _element ) {
		// 開区間(0, 0.5)に含まれる任意の数値
		double tau = 0.25;

		return ( _freeError - estimateError( _matrix, _direction, _observation )
			>= tau * ( _element - _transposedGradFreeMatrix.product( _direction ).get( 0, 0 ) ) );
	}

	private static double estimateError( Matrix _matrix, Matrix _vector, Matrix _observation ) {
		Matrix matrix = _matrix.product( _vector ).minus( _observation );
		int rowSize = matrix.rowSize();
		double result = 0.0;
		for ( int i = 0; i < rowSize; ++i ) {
			double element = matrix.get( i, 0 );
			result += element * element;
		}
		return result / 2.0;
	}

	private static Matrix project( Matrix _matrix ) {
		double[][] elements = _matrix.elements();
		for ( double[] rowElements : elements ) {
			for ( int i = 0; i < rowElements.length; ++i ) {
				rowElements[i] = Math.max( rowElements[i], 0.0 );
			}
		}
		return Matrix.create( elements );
	}

	private FuzzySplineCurveCreater() {
		throw new UnsupportedOperationException( "can not create instance." );
	}

	/**
	 * NNLSアルゴリズムの最大反復回数
	 */
	private static final int NNLS_MAX_ITERATE_TIMES = 1000;
}
