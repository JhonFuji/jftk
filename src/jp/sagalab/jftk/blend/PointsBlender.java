package jp.sagalab.jftk.blend;

import java.util.Arrays;
import java.util.Collections;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.blend.OverlappingPath.Position;
import jp.sagalab.jftk.curve.ParametricEvaluable;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.fuzzysplinecurve.FSCCreater;

/**
 * 点列を融合するためのクラスです。
 * @author Akira Nishikawa
 */
public class PointsBlender implements FSCBlender{
	
	/**
	 * このクラスのインスタンスを生成します。
	 * @param _fscCreater FSC生成のためのcreater
	 * @return 点列を融合するためのクラス
	 * @throws IllegalArgumentException FSC生成のためのcreaterがnullの場合
	 */
	public static PointsBlender create( FSCCreater _fscCreater ) {
		if ( _fscCreater == null ) {
			throw new IllegalArgumentException( "_fscCreater is null" );
		}
		return new PointsBlender( _fscCreater );
	}
	
	/**
	 * この曲線と他方の曲線をサンプリング点により融合します。
	 * @param _thisSplineCurve この曲線
	 * @param _otherSplineCurve 他方の曲線
	 * @param _weightA この曲線の重み
	 * @param _weightB 他方の曲線の重み
	 * @return 両曲線を融合した結果の曲線
	 * @throws IllegalArgumentException このファジィスプライン曲線がnullの場合
	 * @throws IllegalArgumentException 他のファジィスプライン曲線がnullの場合
	 * @throws IllegalArgumentException このファジィスプライン曲線と他のファジィスプライン曲線の次数が異なる場合
	 * @throws IllegalArgumentException 重みの和が０になる場合
	 * @throws IllegalArgumentException 重みがinfinityもしくはNaNの場合 
	 */
	@Override
	public SplineCurve createBlendedFsc( SplineCurve _thisSplineCurve, SplineCurve _otherSplineCurve, double _weightA, double _weightB ) {
		if ( _thisSplineCurve == null ) {
			throw new IllegalArgumentException("_thisSplineCurve is null." );
		}
		if ( _otherSplineCurve == null ) {
			throw new IllegalArgumentException("_otherSplineCurve is null." );
		}
		if ( Double.isNaN( _weightA ) || Double.isNaN( _weightB ) ) {
			throw new IllegalArgumentException( "internal division ratios is NaN." );
		}
		if ( Double.isInfinite( _weightA ) || Double.isInfinite( _weightB ) ) {
			throw new IllegalArgumentException("internal division ratios is Inf.");
		}
		if( _weightA + _weightB == 0){
			throw new IllegalArgumentException( "_weightA + _weightB = 0" );
		}
		// 融合に用いるサンプリング点列の導出
		// 曲線長に応じてサンプリング点列数を変更
		Point[] evaPointsA = _thisSplineCurve.evaluateAll( Math.max( (int) Math.ceil( _thisSplineCurve.length() / 10.0 ), 2 ), ParametricEvaluable.EvaluationType.TIME );
		Point[] evaPointsB = _otherSplineCurve.evaluateAll( Math.max( (int) Math.ceil( _otherSplineCurve.length() / 10.0 ), 2 ), ParametricEvaluable.EvaluationType.TIME );
		// OSM(重複状態行列)を生成し、経路を導出
		OverlappingPath[] path = OverlappingPathFinder.find( evaPointsA, evaPointsB );
		// 経路を重複度に応じてソート
		Arrays.sort( path, new OverlappingPath.DOLComparator() );
		// サンプリング点列を融合
		Point[] blendedPoints = blend( path[path.length-1].path(), evaPointsA, evaPointsB, _weightA, _weightB );
		// 融合結果からFSCを再生成
		return m_fscCreater.createFSC( blendedPoints );
	}
	
	/**
	 * 指定した二つの点列を融合します。
	 * @param _path 重複経路
	 * @param _pointsA 点列A
	 * @param _pointsB 点列B
	 * @param _weightA 重みA
	 * @param _weightB 重みB
	 * @return 融合後の点列
	 */
	private Point[] blend( Position[] _path, Point[] _pointsA, Point[] _pointsB, double _weightA, double _weightB ) {
		// 経路の開始終了位置
		int sX = _path[0].x();
		int sY = _path[0].y();
		int eX = _path[_path.length - 1].x();
		int eY = _path[_path.length - 1].y();
		// _pointsAを反転させる必要があるか
		boolean needReverse = eY - sY < 0;

		// pointsAを反転させる必要のある経路であれば、反転させておく
		Point[] pointsA = _pointsA;
		if ( needReverse ) {
			// pointsAの反転
			pointsA = pointsA.clone();
			Collections.reverse( Arrays.asList( pointsA ) );
			// 経路の開始終了位置のうち、pointsAに関する部分を更新
			sY = pointsA.length - 1 - sY;
			eY = pointsA.length - 1 - eY;
		}
		double blendStartTime = _pointsB[sX].time();
		double blendEndTime =
			_pointsB[sX].time() + ( _weightB * ( _pointsA[eY].time() - _pointsA[sY].time() ) + _weightA * ( _pointsB[eX].time() - _pointsB[sX].time() ) )
			/ ( _weightA + _weightB );

		// 融合区間前の点列
		Point[] prePoints;
		if ( sX > 0 ) {
			prePoints = Arrays.copyOfRange( _pointsB, 0, sX );
		} else if ( sY > 0 ) {
			prePoints = Arrays.copyOfRange( pointsA, 0, sY );
			if ( needReverse ) {
				prePoints = timeReverse( prePoints );
			}
			prePoints = timeShift( prePoints, blendStartTime - prePoints[prePoints.length - 1].time() );
		} else {
			prePoints = new Point[ 0 ];
		}

		// 融合区間の点列
		Point[] blendPoints = new Point[ _path.length ];
		for ( int i = 0; i < _path.length; ++i ) {
			Point p = _pointsA[_path[i].y()].internalDivision( _pointsB[_path[i].x()], _weightB, _weightA );
			double tA = (double) Math.abs( _path[0].y() - _path[i].y() ) / Math.abs( _path[0].y() - _path[_path.length - 1].y() );
			double tB = (double) Math.abs( _path[0].x() - _path[i].x() ) / Math.abs( _path[0].x() - _path[_path.length - 1].x() );
			if ( Double.isNaN( tA ) ) {
				tA = 0.5;
			}
			if ( Double.isNaN( tB ) ) {
				tB = 0.5;
			}
			double w = ( tA * _weightB + tB * _weightA ) / ( _weightA + _weightB );
			double time = ( 1 - w ) * blendStartTime + w * blendEndTime;
			blendPoints[i] = Point.createXYZTF( p.x(), p.y(), p.z(), time, p.fuzziness() );
		}

		// 融合区間後の点列
		Point[] postPoints;
		if ( eX < _pointsB.length - 1 ) {
			postPoints = Arrays.copyOfRange( _pointsB, eX + 1, _pointsB.length );
			postPoints = timeShift( postPoints, blendEndTime - postPoints[0].time() );
		} else if ( eY < pointsA.length - 1 ) {
			postPoints = Arrays.copyOfRange( pointsA, eY + 1, pointsA.length );
			if ( needReverse ) {
				postPoints = timeReverse( postPoints );
			}
			postPoints = timeShift( postPoints, blendEndTime - postPoints[0].time() );
		} else {
			postPoints = new Point[ 0 ];
		}

		// 統合
		Point[] blendedPoints = new Point[ prePoints.length + blendPoints.length + postPoints.length ];
		System.arraycopy( prePoints, 0, blendedPoints, 0, prePoints.length );
		System.arraycopy( blendPoints, 0, blendedPoints, prePoints.length, blendPoints.length );
		System.arraycopy( postPoints, 0, blendedPoints, prePoints.length + blendPoints.length, postPoints.length );

		return blendedPoints;
	}
	
	/**
	 * 指定した点列の時間軸を移動します。
	 * @param _points 点列
	 * @param _shiftValue 移動量
	 * @return 時間軸移動後の点列
	 */
	private Point[] timeShift( Point[] _points, double _shiftValue ) {
		Point[] points = new Point[ _points.length ];
		for ( int i = 0; i < _points.length; ++i ) {
			Point p = _points[i];
			double time = p.time() + _shiftValue;
			points[i] = Point.createXYZTF( p.x(), p.y(), p.z(), time, p.fuzziness() );
		}
		return points;
	}
	
	/**
	 * 指定した点列の時間軸を反転します。
	 * @param _points 点列
	 * @return 時間列反転後の点列
	 */
	private Point[] timeReverse( Point[] _points ) {
		Point[] points = new Point[ _points.length ];
		double startTime = _points[0].time();
		double endTime = _points[_points.length - 1].time();
		for ( int i = 0; i < _points.length; ++i ) {
			Point p = _points[i];
			double time = endTime - p.time() + startTime;
			points[i] = Point.createXYZTF( p.x(), p.y(), p.z(), time, p.fuzziness() );
		}
		return points;
	}
	
	private PointsBlender( FSCCreater _fscCreater ) {
		m_fscCreater = _fscCreater;
	}
	
	/** FSC生成のストラテジー */
	private final FSCCreater m_fscCreater;
}
