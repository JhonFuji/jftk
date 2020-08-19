package jp.sagalab.jftk.recognition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.transform.Transformable;

/**
 * 円弧，楕円弧などの簡約化可能な曲線のバウンディングボックスを表すクラスです。
 * <p>
 * バウンディングボックスは円弧や楕円弧を包含する矩形を表します。
 * この矩形を4分割した「小矩形」という頂点列の単位をバウンディングボックス上で扱っています。
 * </p>
 * @author nakajima
 */
public class BoundingBox implements Transformable<BoundingBox>{

	/**
	 * バウンディングボックスを生成します。
	 * @param _center 中心点
	 * @param _axisPoints 軸点列
	 * @param _cornerPoints 角点列
	 * @param _range レンジ
	 * @return バウンディングボックス
	 * @exception IllegalArgumentException 中心点がNullである場合
	 * @exception IllegalArgumentException 軸点列が2点未満である場合
	 * @exception IllegalArgumentException 角点列が1点未満である場合
	 * @exception IllegalArgumentException レンジがNullである場合
	 */
	public static BoundingBox create( Point _center, Point[] _axisPoints, Point[] _cornerPoints, Range _range ) {
		if ( _center == null ) {
			throw new IllegalArgumentException( "_center is null" );
		}
		if ( _axisPoints.length < 2 ) {
			throw new IllegalArgumentException( "_axisPoints.length < 2" );
		}
		if ( _cornerPoints.length < 1 ) {
			throw new IllegalArgumentException( "_cornerPoints.length < 1" );
		}
		if ( _range == null ) {
			throw new IllegalArgumentException( "_range is null" );
		}
		return new BoundingBox( _center, _axisPoints, _cornerPoints, _range );
	}

	/**
	 * ボックスの中心を返します。
	 * @return ボックスの中心
	 */
	public Point getCenter() {
		return m_center;
	}

	/**
	 * ボックスの軸となる点列を返します。
	 * 点列はパラメータ順に整列します。
	 * @return 軸点列
	 */
	public Point[] getAxisPoints() {
		List<Point> points = new ArrayList<Point>();
		points.addAll( Arrays.asList( m_axisPoints ) );
		Collections.sort( points, new Point.ParameterComparator() );
		return points.toArray( new Point[points.size()] );
	}

	/**
	 * ボックスの角となる点列を返します。
	 * 点列はパラメータ順に整列します。
	 * @return 角点列
	 */
	public Point[] getCornerPoints() {
		List<Point> points = new ArrayList<Point>();
		points.addAll( Arrays.asList( m_cornerPoints ) );
		Collections.sort( points, new Point.ParameterComparator() );
		return points.toArray( new Point[points.size()] );
	}

	/**
	 * バウンディングボックスの小矩形の数を返します。
	 * @return 小矩形の数
	 */
	public final int getSmallRectLength() {
		// 「小矩形の数」 = 「角点の数」
		return m_cornerPoints.length;
	}

	/**
	 * 指定された小矩形の頂点列を返します。
	 * @param _num 小矩形の番号
	 * @return 小矩形の頂点列
	 */
	public Point[] getSmallRectangle( int _num ) {
		if ( _num > getSmallRectLength() ) {
			//XXX nullを返すか例外を投げるべき？
			System.err.printf( "Warning: _num(%d) > BoxLength(%d) in getSmallRectangle() .\r\n",
				_num, getSmallRectLength() );
			return new Point[0];
		}
		Point[] miniBox = new Point[4];
		if ( _num == 3 ) {
			miniBox[0] = m_center;
			miniBox[1] = m_axisPoints[3];
			miniBox[2] = m_cornerPoints[3];
			miniBox[3] = m_axisPoints[0];
		} else {
			miniBox[0] = m_center;
			miniBox[1] = m_axisPoints[_num];
			miniBox[2] = m_cornerPoints[_num];
			miniBox[3] = m_axisPoints[_num + 1];
		}
		return miniBox;
	}

	/**
	 * 対応する曲線の径点（軸点）となる特徴点を返します。
	 * @return 特徴点列
	 */
	public Point[] getDiamentrialFeaturePoints() {
		List<Point> featurePoints = new ArrayList<Point>();
		for ( Point p : getAxisPoints() ) {
			if ( m_range.isInner( p.time() ) ) {
				featurePoints.add( p );
			}
		}
		return featurePoints.toArray( new Point[0] );
	}

	/**
	 * 対応する曲線の角点となる特徴点を返します。
	 * @return 特徴点列
	 */
	public Point[] getCornerFeaturePoints() {
		List<Point> featurePoints = new ArrayList<Point>();

		Point[] diametrialFeaPoints = getDiamentrialFeaturePoints();
		double start = diametrialFeaPoints[0].time();
		double end = diametrialFeaPoints[diametrialFeaPoints.length - 1].time();
		Range range = Range.create( start, end );
		for ( Point p : getCornerPoints() ) {
			if ( range.isInner( p.time() ) ) {
				featurePoints.add( p );
			}
		}

		return featurePoints.toArray( new Point[0] );
	}

	/**
	 * バウンディングボックスを指定された行列で変換します。
	 * @param _mat 変換行列
	 * @return 変換されたバウンディングボックス
	 */
	@Override
	public BoundingBox transform( TransformMatrix _mat ) {
		Point transedCenter = getCenter().transform( _mat );
		Point[] transedAxisPoints = new Point[m_axisPoints.length];
		Point[] transedCornerPoints = new Point[m_cornerPoints.length];

		for ( int i = 0; i < getAxisPoints().length; ++i ) {
			transedAxisPoints[i] = m_axisPoints[i].transform( _mat );
		}
		for ( int i = 0; i < getCornerPoints().length; ++i ) {
			transedCornerPoints[i] = m_cornerPoints[i].transform( _mat );
		}

		return new BoundingBox( transedCenter, transedAxisPoints, transedCornerPoints, m_range );
	}

	/**
	 * バウンディングボックスを生成します。
	 * @param _center 中心点
	 * @param _axisPoints 軸点列
	 * @param _cornerPoints 角点列
	 * @param _range レンジ
	 */
	private BoundingBox( Point _center, Point[] _axisPoints, Point[] _cornerPoints, Range _range ) {
		m_center = _center;
		m_axisPoints = _axisPoints.clone();
		m_cornerPoints = _cornerPoints.clone();
		m_range = _range;
	}

	/** バウンディングボックスの中心 */
	private final Point m_center;
	/** 軸点列 */
	private final Point[] m_axisPoints;
	/** 角点列 */
	private final Point[] m_cornerPoints;
	/** 対応する曲線のレンジ */
	private final Range m_range;
}
