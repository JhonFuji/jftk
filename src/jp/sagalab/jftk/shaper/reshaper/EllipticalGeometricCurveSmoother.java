package jp.sagalab.jftk.shaper.reshaper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.BezierCurve;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.primitive.OpenFreeCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.shaper.ShapedResult;
import jp.sagalab.jftk.shaper.snapper.GridSpace;

/**
 * 楕円弧幾何曲線列をなめらかに整形するクラスです。
 * @author kamiya
 */
public class EllipticalGeometricCurveSmoother implements FuzzyObjectReshaper{

	/** *
	 * このクラスのインスタンスを生成します。
	 * @param _primitives 楕円弧幾何曲線列
	 * @param _separatePoints 分割点
	 * @param _grid グリッド空間
	 * @throws IllegalArgumentException 楕円弧幾何曲線列がnullの場合
	 * @throws IllegalArgumentException 楕円弧幾何曲線列にnullが含まれる場合
	 * @throws IllegalArgumentException 分割点がnullの場合
	 * @throws IllegalArgumentException 分割点にnullが含まれる場合
	 * @throws IllegalArgumentException グリッド空間がnullの場合
	 * @return インスタンス
	 */
	public static EllipticalGeometricCurveSmoother create( PrimitiveCurve[] _primitives, Point[] _separatePoints, GridSpace _grid ) {	
		if ( _primitives == null ) {
			throw new IllegalArgumentException( "_primitives is null." );
		}
		if ( Arrays.asList( _primitives ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException(" _primitives include null ");
		}
		if ( _separatePoints == null ) {
			throw new IllegalArgumentException( "_separatePoints is null." );
		}
		if ( Arrays.asList( _separatePoints ).indexOf( null ) >= 0 ) {
			throw new IllegalArgumentException(" _separatePoints include null ");
		}
		if ( _grid == null ) {
			throw new IllegalArgumentException( "_grid is null." );
		}
		return new EllipticalGeometricCurveSmoother( _primitives, _separatePoints, _grid );
	}

	/**
	 * 楕円弧幾何曲線列を返します。
	 * @return 楕円弧幾何曲線列
	 */
	public PrimitiveCurve[] getEllipticalArcGeometricCurve() {
		return m_ellipticalArcGeometricCurve;
	}

	@Override
	public ShapedResult reshape() {
		PrimitiveCurve primiti = m_ellipticalArcGeometricCurve[0];
		for ( int i = 1; i < m_ellipticalArcGeometricCurve.length; ++i ) {
			PrimitiveCurve back = m_ellipticalArcGeometricCurve[i];
			// XXX 暫定的に分割点は終点側の幾何曲線の始点としている
			primiti = createFormatPrimitiveCurve( primiti, back, back.locus( back.range().start() ), m_separatePoints[i - 1].fuzziness() );
		}

		return ShapedResult.create( primiti, new Point[0], new GridSpace[0], null );
	}

	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final EllipticalGeometricCurveSmoother other = (EllipticalGeometricCurveSmoother) obj;
		if ( !Arrays.equals(this.m_ellipticalArcGeometricCurve, other.m_ellipticalArcGeometricCurve ) ) {
			return false;
		}
		if ( !m_gridSpace.equals( other.m_gridSpace )) {
			return false;
		}
		return Arrays.deepEquals( this.m_separatePoints, other.m_separatePoints );
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 23 * hash + ( this.m_ellipticalArcGeometricCurve != null ? this.m_ellipticalArcGeometricCurve.hashCode() : 0 );
		hash = 23 * hash + Arrays.deepHashCode( this.m_separatePoints );
		hash = 23 * hash + ( this.m_gridSpace != null ? this.m_gridSpace.hashCode() : 0 );
		return hash;
	}
	
	@Override
	public String toString(){
		return String.format( "ellipticalArcGeometricCurve:%s separatePoints:%s gridSpace:%s", 
			Arrays.deepToString( m_ellipticalArcGeometricCurve ),Arrays.deepToString( m_separatePoints ), m_gridSpace.toString() );
	}
	
	/**
	 * 2つの幾何曲線を再接続し、一つの幾何曲線として返します。
	 * @param _start 始点側の幾何曲線
	 * @param _end 終点側の幾何曲線
	 * @param _p 接続点
	 * @return 整形後の幾何曲線
	 * //TODO 再接続の際に刈り込みの量が幾何曲線全てを包含する場合，制御点を求められずnullとなってしまう
	 */
	private PrimitiveCurve createFormatPrimitiveCurve( PrimitiveCurve _start, PrimitiveCurve _end, Point _p, double _fuzziness ) {
		BezierCurve[] startBeziers = _start.convert();
		BezierCurve[] endBeziers = _end.convert();
		BezierCurve[] constantStartBeziers = null;
		BezierCurve[] constantEndBeziers = null;
		BezierCurve start = null;
		BezierCurve end = null;

		// 分割点のファジネスの量子化
		// TODO 量子化については検討の必要あり
		//double length = getQuantizationFuzziness( _fuzziness );
		double length = _fuzziness;
		
		// 始点側の幾何曲線の刈り込みを行う
		for ( int i = startBeziers.length - 1; i > -1; --i ) {
			BezierCurve bezier = startBeziers[i];
			// 始点側の幾何曲線の始点が分割点のファジネス外に居る場合,幾何曲線とファジネスの交点を求める
      // ファジネス内に居る場合、幾何曲線自体がファジネス内に居るので、始点のみを返す
			if ( bezier.controlPoints()[3].distance( _p ) < length ) {
				if ( bezier.controlPoints()[0].distance( _p ) > length ) {
					double evaluateParm = getBinarySearchParameter( bezier, _p, length );
					start = bezier.divide( evaluateParm )[0];
					constantStartBeziers = getPartBezier( startBeziers, 0, i - 1 );
					break;
				} else if ( i == 0 ) {
					// XXX 幾何曲線全体が刈り込み範囲に入ってしまった場合どうするのか。
					double evaluateParm = getBinarySearchParameter( bezier, _p, length );
					start = bezier.divide( evaluateParm )[0];
					constantStartBeziers = new BezierCurve[]{};
				}
			}
		}

		// 終点側の幾何曲線の刈り込みを行う
		for ( int i = 0; i < endBeziers.length; ++i ) {
			BezierCurve bezier = endBeziers[i];
			// 終点側の幾何曲線の終点が分割点のファジネス外に居る場合,幾何曲線とファジネスの交点を求める
      // ファジネス内に居る場合、幾何曲線自体がファジネス内に居るので、終点のみを返す
			if ( bezier.controlPoints()[0].distance( _p ) < length ) {
				if ( bezier.controlPoints()[3].distance( _p ) > length ) {
					double evaluateParm = getBinarySearchParameter( bezier, _p, length );
					end = bezier.divide( evaluateParm )[1];
					constantEndBeziers = getPartBezier( endBeziers, i + 1, endBeziers.length - 1 );
					break;
				} else if ( i == endBeziers.length - 1 ) {
					// XXX 幾何曲線全体が刈り込み範囲に入ってしまった場合どうするのか。
					double evaluateParm = getBinarySearchParameter( bezier, _p, length );
					end = bezier.divide( evaluateParm )[1];
					constantEndBeziers = new BezierCurve[]{};
				}
			}
		}

		// 刈り込んだ曲線間を3次Bezier曲線で補間する
		BezierCurve[] conectBeziers = createConectBezier( start, end, _p );
		// 始点側，補間，終点側の順に保存
		List<BezierCurve> bezierList = new ArrayList<BezierCurve>();
		bezierList.addAll( Arrays.asList( constantStartBeziers ) );
		bezierList.addAll( Arrays.asList( conectBeziers ) );
		bezierList.addAll( Arrays.asList( constantEndBeziers ) );
		BezierCurve[] beziers = bezierList.toArray( new BezierCurve[bezierList.size()] );

		return OpenFreeCurve.create( beziers );
	}

	/**
	 * 指定した要素の範囲のBezier曲線を取り出す
	 * @param _bezier Bezier曲線列
	 * @param _start 最初の要素
	 * @param _end 最後の要素
	 * @return Bezier曲線列
	 */
	private BezierCurve[] getPartBezier( BezierCurve[] _bezier, int _start, int _end ) {
		BezierCurve[] bezier = new BezierCurve[( _end + 1 ) - _start];

		for ( int i = 0; i < bezier.length; ++i ) {
			bezier[i] = _bezier[_start + i];
		}

		return bezier;
	}

	/**
	 * 2つのBezier曲線の間を補間するBezier曲線を生成。
	 * @param _start 始点側のBezier
	 * @param _end 終点側のBezier
	 * @param _dividedPoint ２つのBezier曲線の分割点
	 * @return 補間するBezier曲線
	 */
	private BezierCurve[] createConectBezier( BezierCurve _start, BezierCurve _end, Point _dividedPoint ) {
		Point endPoint = _start.controlPoints()[3];
		Point startPoint = _end.controlPoints()[0];
		Point[] controlPoints = new Point[4];
		Range r = Range.create( 0, 1 );
		// 制御点を求める
		controlPoints[0] = endPoint;
		controlPoints[1] = getIntersectionVector( _start.controlPoints()[2], endPoint, _dividedPoint );
		controlPoints[2] = getIntersectionVector( _end.controlPoints()[1], startPoint, _dividedPoint );
		controlPoints[3] = startPoint;
		// 接続するBezierCurveを作成
		BezierCurve conectBezier = BezierCurve.create( controlPoints, r );
		BezierCurve[] beziers = { _start, conectBezier, _end };
		return beziers;
	}

	/**
	 * ファジネスの量子化を行う．
	 * @param _fuzziness 量子化するファジネス
	 * @return 引数と近傍であるグリッドのファジネス
	 */
	private double getQuantizationFuzziness( double _fuzziness ) {
		GridSpace grid = m_gridSpace;
		GridSpace newGrid;
		double ratio;
		double newRatio;

		while ( true ) {
			// 基準となる割合
			ratio = _fuzziness / grid.basedFuzziness();
			// ファジネスがグリッドのファジネスより大きい場合，グリッドを低解像度にする
			//                                   小さい場合，グリッドを高解像度にする
			if ( _fuzziness > grid.basedFuzziness() ) {
				newGrid = grid.downResolution();
				newRatio = _fuzziness / newGrid.basedFuzziness();
			} else {
				newGrid = grid.upResolution();
				newRatio = _fuzziness / newGrid.basedFuzziness();
			}
			// 基準値の方が大きい場合，解像度を変更したグリッドで処理を繰り返す
			if ( Math.abs( 1 - ratio ) > Math.abs( 1 - newRatio ) ) {
				grid = newGrid;
			} else {
				return grid.basedFuzziness();
			}
		}
	}

	/**
	 * 二分探索をして、Bezier曲線とファジネスの半径と重なる部分の割合を返す
	 * @param _bez	探索するBezier曲線
	 * @param _p Bezier曲線の端点
	 * @param _length 削る大きさ
	 * @return 割合
	 */
	private double getBinarySearchParameter( BezierCurve _bez, Point _p, double _length ) {
		double parm = 0.5;
		double evaluateParm = parm;
		Point evaluatePoint;
		// 二分探索を10回繰り返す（数が大きくなるほど，探索の精度が向上する）
		for ( int j = 0; j < 10; ++j ) {
			evaluatePoint = _bez.evaluateAt( evaluateParm );
			parm = parm / 2;
			// 半分にした時に，ファジネス内に居るかどうかの判定をする
			if ( _bez.controlPoints()[0].distance( _p ) > _length ) {
				if ( evaluatePoint.distance( _p ) > _length ) {
					evaluateParm = evaluateParm + parm;
				} else {
					evaluateParm = evaluateParm - parm;
				}
			} else if ( evaluatePoint.distance( _p ) > _length ) {
				evaluateParm = evaluateParm - parm;
			} else {
				evaluateParm = evaluateParm + parm;
			}
		}
		return evaluateParm;
	}

	/**
	 * 点BからベクトルOAへの垂線を求め、その交点を返す。
	 * @param _o 始点O
	 * @param _a 点A
	 * @param _b 点B
	 * @return 交点
	 */
	private Point getIntersectionVector( Point _o, Point _a, Point _b ) {
		Vector oa = Vector.createSE( _o, _a );
		Vector ob = Vector.createSE( _o, _b );

		// ベクトルOAとベクトルOBの内積を求める
		double dot = oa.dot( ob );
		// 始点Oから交点までの大きさを求める
		// XXX 縮退するとエラーでる length zero
		double intersectionPointLength = dot / oa.length();
		// 比率を求める
		double ratioLength = intersectionPointLength / oa.length();
		Vector intersectionVector = oa.magnify( ratioLength );
		Point intersectionPoint = _o.move( intersectionVector );

		return intersectionPoint;
	}
	
	private EllipticalGeometricCurveSmoother( PrimitiveCurve[] _primitives, Point[] _separatePoints, GridSpace _grid ) {
		m_ellipticalArcGeometricCurve = _primitives;
		m_separatePoints = _separatePoints;
		m_gridSpace = _grid;
	}

	/** 楕円弧幾何曲線列 */
	private final PrimitiveCurve[] m_ellipticalArcGeometricCurve;
	/** 分割点 */
	private final Point[] m_separatePoints;
	/** グリッド */
	private final GridSpace m_gridSpace;
}
