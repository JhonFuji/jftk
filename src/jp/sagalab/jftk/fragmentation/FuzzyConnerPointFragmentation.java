package jp.sagalab.jftk.fragmentation;

import java.util.ArrayList;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.ParametricEvaluable.EvaluationType;

/**
 * ファジィ角フラグメンテーションを行います。
 * <p>
 * ファジィ角フラグメンテーションは位置の曖昧さ(ファジネス)を考慮しつつ
 * ファジィスプライン曲線の角で分割します。
 * ファジィ角フラグメンテーションでは、
 * ファジィスプライン曲線の形状的な角のファジネスが大きければ角として判断されやすく、
 * ファジネスが小さければ角として判断されにくくなっています。
 * 角かどうかは閾値により２値化します。
 * </p>
 * @author oyoshi
 */
public class FuzzyConnerPointFragmentation implements FuzzyFragmentation {	
	
	/**
	 * このクラスのインスタンスを生成します。
	 * @param _connerThreshold 角度の閾値(度)
	 * @return インスタンス
	 * @throws IllegalArgumentException 角度の閾値がNaNもしくはInfiniteの時
	 */
	public static FuzzyConnerPointFragmentation create( double _connerThreshold ){
		if( Double.isNaN( _connerThreshold )){
			throw new IllegalArgumentException("_connerThreshold is NaN");
		}
		if( Double.isInfinite( _connerThreshold ) ){
			throw new IllegalArgumentException("_connerThreshold is Inf");
		}
		return new FuzzyConnerPointFragmentation( _connerThreshold );
	}
	
	@Override
	public Fragment[] createFragment( SplineCurve _splineCurve ) {
		List<Fragment> fragments = new ArrayList<Fragment>();

		int evalateNum = (int) Math.max( 2, Math.ceil( _splineCurve.range().length() / 0.01 ) );
		Point[] points = _splineCurve.evaluateAll( evalateNum, EvaluationType.TIME );

		double stateStart = points[0].time();
		Point connerStart = null;
		boolean isConner = false;

		for ( int i = 0; i < points.length; ++i ) {
			Point[] nextPoints = searchNextPoints( points, i );
			// 2点の隣接点がある
			if ( nextPoints.length == 2 ) {
				double angle = calculateAngle( points[i], nextPoints[0], nextPoints[1] );
				// 閾値より角度が小さい場合は角とみなす
				if ( angle < m_connerThreshold ) {
					if ( !isConner ) {
						// 同定単位フラグメント
						SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[i].time() ) );
						fragments.add( IdentificationFragment.create( curve ) );
						stateStart = points[i].time();
						connerStart = points[i];
					}
					isConner = true;
				} else {
					if ( isConner ) {
						// 角フラグメント
						SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[i].time() ) );
						fragments.add( PartitionFragment.create( curve, connerStart, points[i] ) );
						stateStart = points[i].time();
					}
					isConner = false;
				}
			}
		}
		SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[points.length - 1].time() ) );
		if ( isConner ) {
			fragments.add( PartitionFragment.create( curve, connerStart, null ) );
		} else {
			fragments.add( IdentificationFragment.create(curve ) );
		}
		
		// フラグメント列の先頭の接続の足を更新
		Fragment headFragment = fragments.get( 0 );
		if ( headFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) headFragment;
			if ( !partition.isHead() ) {
				fragments.set( 0, PartitionFragment.create( partition, null, partition.end() ) );
			}
		}
		// フラグメント列の末尾の接続の足を更新
		Fragment tailFragment = fragments.get( fragments.size() - 1 );
		if ( tailFragment.getClass() == PartitionFragment.class ) {
			PartitionFragment partition = (PartitionFragment) tailFragment;
			if ( !partition.isTail() ) {
				fragments.set( fragments.size() - 1,
					PartitionFragment.create( partition, partition.start(), null ) );
			}
		}
		
		return fragments.toArray( new Fragment[ fragments.size() ] );
	}
		
	@Override
	public SplineCurve[] divide( SplineCurve _splineCurve ) {
		List<SplineCurve> sp = new ArrayList<SplineCurve>();

		int evalateNum = (int) Math.max( 2, Math.ceil( _splineCurve.range().length() / 0.01 ) );
		Point[] points = _splineCurve.evaluateAll( evalateNum, EvaluationType.TIME );

		double stateStart = points[0].time();
		boolean isConner = false;

		for ( int i = 0; i < points.length; ++i ) {
			Point[] nextPoints = searchNextPoints( points, i );
			// 2点の隣接点がある
			if ( nextPoints.length == 2 ) {
				double angle = calculateAngle( points[i], nextPoints[0], nextPoints[1] );
				// 閾値より角度が小さい場合は角とみなす
				if ( angle < m_connerThreshold ) {
					if ( !isConner ) {
						// 同定単位
						sp.add( _splineCurve.part( Range.create( stateStart, points[i].time() ) ) );
						stateStart = points[i].time();
					}
					isConner = true;
				} else {
					if ( isConner ) {
						stateStart = points[i].time();
					}
					isConner = false;
				}
			}
		}
		SplineCurve curve = _splineCurve.part( Range.create( stateStart, points[points.length - 1].time() ) );
		if ( !isConner ) {
			sp.add( curve );
		}

    return sp.toArray( new SplineCurve[sp.size()]);
	}
	
	/**
	 * 着目点と重ならない前後の点との角度を計算して返します。
	 * @param _target 着目点
	 * @param _pre 着目点の前の点
	 * @param _post 着目点の後の点
	 * @return 角度(度)
	 */
	private static double calculateAngle( Point _target, Point _pre, Point _post ) {
		Vector first = Vector.createSE( _target, _pre );
		Vector second = Vector.createSE( _target, _post );

		return first.angle( second );
	}

	/**
	 * 着目点と重ならない前後の2点を探索し返します
	 * @param _points ファジィ点列
	 * @param _targetIndex 指定した点のインデックス
	 * @return 重ならない前後の点
	 */
	private static Point[] searchNextPoints( Point[] _points, int _targetIndex ) {
		List<Point> next = new ArrayList<Point>();
		Point[] points = _points;
		Point targetPoint = points[_targetIndex];
		int pointNum = points.length;
		for ( int i = _targetIndex; i >= 0; --i ) {
			double distance = targetPoint.distance( points[i] );
			if ( distance >= targetPoint.fuzziness() + points[i].fuzziness() ) {
				next.add( points[i] );
				break;
			}
		}
		for ( int i = _targetIndex; i < pointNum; ++i ) {
			double dis = targetPoint.distance( points[i] );
			if ( dis >= targetPoint.fuzziness() + points[i].fuzziness() ) {
				next.add( points[i] );
				break;
			}
		}

		return next.toArray( new Point[ next.size() ] );
	}
	
	private FuzzyConnerPointFragmentation( double _connerThreshold ) {
		m_connerThreshold = _connerThreshold;
	}
	
	/** ファジィ角フラグメンテーションの角と判断するための角度の閾値（度） */
	private final double m_connerThreshold;
}
