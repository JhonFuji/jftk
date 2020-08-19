package jp.sagalab.jftk;

import jp.sagalab.jftk.curve.primitive.Circle;
import jp.sagalab.jftk.curve.primitive.CircularArc;
import jp.sagalab.jftk.curve.primitive.Ellipse;
import jp.sagalab.jftk.curve.primitive.EllipticArc;
import jp.sagalab.jftk.curve.primitive.Line;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 単振動モデルを用いて擬似サンプリング点列を生成するクラスです
 * @author yamamoto
 */
public class PseudoSamplingPointsCreater{

	public static Point[] create( PrimitiveCurve _primitive ) {
		Point[] samplingPoint = null;

		if ( _primitive instanceof Line ) {
			Line LPrimitive = (Line) _primitive;
			samplingPoint = createLine( LPrimitive );

		} else if ( _primitive instanceof Circle || _primitive instanceof CircularArc ) {
			CircularArc CAPrimitive = (CircularArc) _primitive;
			samplingPoint = createCircularArc( CAPrimitive );

		} else if ( _primitive instanceof Ellipse || _primitive instanceof EllipticArc ) {
			EllipticArc EAPrimitive = (EllipticArc) _primitive;
			samplingPoint = createEllipticArc( EAPrimitive );
		}
		return samplingPoint;
	}

	/**
	 * 単振動モデルを用いて線形状の擬似サンプリング点列を生成します
	 * @param _startPoint	始点座標
	 * @param _endPoint	終点座標
	 * @return 線形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 各値にnullが指定された場合
	 * @throw IllegalArgumentException 各値にNaNが指定された場合
	 * @throw IllegalArgumentException 各値にInfiniteが指定された場合
	 */
	public static Point[] createLine( Point _startPoint, Point _endPoint ) {

		if ( _startPoint == null ) {
			throw new IllegalArgumentException( "_startPoint is null." );
		}
		if ( _endPoint == null ) {
			throw new IllegalArgumentException( "_endPoint is null." );
		}
		if ( Double.isNaN( _startPoint.x() ) ) {
			throw new IllegalArgumentException( "_startPoint x is NaN." );
		}
		if ( Double.isInfinite( _startPoint.x() ) ) {
			throw new IllegalArgumentException( "_startPoint x is inf." );
		}
		if ( Double.isNaN( _startPoint.y() ) ) {
			throw new IllegalArgumentException( "_startPoint y is NaN." );
		}
		if ( Double.isInfinite( _startPoint.y() ) ) {
			throw new IllegalArgumentException( "_startPoint y is inf." );
		}
		if ( Double.isNaN( _endPoint.x() ) ) {
			throw new IllegalArgumentException( "_endPoint x is NaN." );
		}
		if ( Double.isInfinite( _endPoint.x() ) ) {
			throw new IllegalArgumentException( "_endPoint x is inf." );
		}
		if ( Double.isNaN( _endPoint.y() ) ) {
			throw new IllegalArgumentException( "_endPoint y is NaN." );
		}
		if ( Double.isInfinite( _endPoint.y() ) ) {
			throw new IllegalArgumentException( "_endPoint y is inf." );
		}

		//構成点数
		double time = 2.0;
		int num = (int) ( time * 100 );
		Point[] line = new Point[num];

		//単振動モデルの計算
		for ( int i = 0; i < num; ++i ) {
			double theata = i * Math.PI / ( num - 1 );
			double x = ( _startPoint.x() + _endPoint.x() ) / 2 - ( _endPoint.x() - _startPoint.x() ) / 2 * Math.cos( theata );
			double y = ( _startPoint.y() + _endPoint.y() ) / 2 - ( _endPoint.y() - _startPoint.y() ) / 2 * Math.cos( theata );
			time = i * 0.01;
			//点列の格納
			line[i] = Point.createXYZTF( x, y, 0, time, 0 );
		}
		return line;
	}

	/**
	 * 単振動モデルを用いて線形状の擬似サンプリング点列を生成します
	 * @param _line 線分
	 * @return 線形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 線分にnullが指定された場合
	 */
	public static Point[] createLine( Line _line) {
		if ( _line == null ) {
			throw new IllegalArgumentException( "_line is null." );
		}
		return createLine( _line.start(), _line.end() );

	}

	/**
	 * 単振動モデルを用いて円形状の擬似サンプリング点列を生成します
	 * @param _center 中心点
	 * @param _radius	半径
	 * @param _startAngle 開始角
	 * @param _endAngle 終了角
	 * @param _posture 姿勢行列
	 * @return 円形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 各値にnullが指定された場合
	 * @throw IllegalArgumentException 各値にNaNが指定された場合
	 * @throw IllegalArgumentException 各値にInfiniteが指定された場合
	 */
	public static Point[] createCircularArc( Point _center, double _radius, double _startAngle,
		double _endAngle, TransformMatrix _posture ){

		if ( _center == null ) {
			throw new IllegalArgumentException( "_center is null." );
		}
		if ( _posture == null ) {
			throw new IllegalArgumentException( "_posture is null." );
		}
		if ( Double.isNaN( _center.x() ) ) {
			throw new IllegalArgumentException( "_center x is NaN." );
		}
		if ( Double.isInfinite( _center.x() ) ) {
			throw new IllegalArgumentException( "_center x is inf." );
		}
		if ( Double.isNaN( _center.y() ) ) {
			throw new IllegalArgumentException( "_center y is NaN." );
		}
		if ( Double.isInfinite( _center.y() ) ) {
			throw new IllegalArgumentException( "_center y is inf." );
		}
		if ( Double.isNaN( _radius ) ) {
			throw new IllegalArgumentException( "_radius is NaN." );
		}
		if ( Double.isInfinite( _radius ) ) {
			throw new IllegalArgumentException( "_radius is inf." );
		}
		if ( Double.isNaN( _startAngle ) ) {
			throw new IllegalArgumentException( "_startAngle is NaN." );
		}
		if ( Double.isInfinite( _startAngle ) ) {
			throw new IllegalArgumentException( "_startAngle is inf." );
		}
		if ( Double.isNaN( _endAngle ) ) {
			throw new IllegalArgumentException( "_endAngle is NaN." );
		}
		if ( Double.isInfinite( _endAngle ) ) {
			throw new IllegalArgumentException( "_endAngle is inf." );
		}

		//描画時間
		double time = 2.0;
		//構成点
		int num = (int) ( time * 100 );
		Point[] circularArc = new Point[num];

		//単振動モデルの計算
		for ( int i = 0; i < num; ++i ) {
			double theata = _startAngle + ( i * _endAngle / ( num - 1 ) );
			double x = _radius * Math.cos( theata );
			double y = _radius * Math.sin( theata );
			time = 0.01 * i;
			Point pos = Point.createXYZ( x, y, 0 );
			TransformMatrix translation = TransformMatrix.translation( _center.x(), _center.y(), _center.z() );
			pos = pos.transform( _posture.product( translation ) );
			//点列の格納
			circularArc[i] = Point.createXYZTF( pos.x(), pos.y(), 0, time, 0 );
		}

		return circularArc;
	}
	
	/**
	 * 単振動モデルを用いて円形状の擬似サンプリング点列を生成します
	 * @param _circularArc 円弧
	 * @return 円形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 円弧にnullが指定された場合
	 */
	public static Point[] createCircularArc( CircularArc _circularArc ) {
		if ( _circularArc == null ) {
			throw new IllegalArgumentException( "_circularArc is null." );
		}
		return createCircularArc( _circularArc.center(), _circularArc.radius(),
			_circularArc.range().start(), _circularArc.range().end(), _circularArc.posture() );
	}

	/**
	 * 単振動モデルを用いて楕円弧状の擬似サンプリング点列を生成します
	 * @param _center 中心点
	 * @param _major 長径
	 * @param _minor 短径
	 * @param _startAngle 開始角
	 * @param _endAngle 終了角
	 * @param _posture 姿勢行列
	 * @return 楕円形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 各値にnullが指定された場合
	 * @throw IllegalArgumentException 各値にNaNが指定された場合
	 * @throw IllegalArgumentException 各値にInfiniteが指定された場合
	 */
	public static Point[] createEllipticArc( Point _center, double _major, double _minor,
		double _startAngle, double _endAngle, TransformMatrix _posture) {
		if ( _center == null ) {
			throw new IllegalArgumentException( "_center is null." );
		}
		if ( _posture == null ) {
			throw new IllegalArgumentException( "_posture is null." );
		}
		if ( Double.isNaN( _center.x() ) ) {
			throw new IllegalArgumentException( "_center x is NaN." );
		}
		if ( Double.isInfinite( _center.x() ) ) {
			throw new IllegalArgumentException( "_center x is inf." );
		}
		if ( Double.isNaN( _center.y() ) ) {
			throw new IllegalArgumentException( "_center y is NaN." );
		}
		if ( Double.isInfinite( _center.y() ) ) {
			throw new IllegalArgumentException( "_center y is inf." );
		}
		if ( Double.isNaN( _major ) ) {
			throw new IllegalArgumentException( "_major is NaN." );
		}
		if ( Double.isInfinite( _major ) ) {
			throw new IllegalArgumentException( "_major is inf." );
		}
		if ( Double.isNaN( _minor ) ) {
			throw new IllegalArgumentException( "_minor is NaN." );
		}
		if ( Double.isInfinite( _minor ) ) {
			throw new IllegalArgumentException( "_minor is inf." );
		}
		if ( Double.isNaN( _startAngle ) ) {
			throw new IllegalArgumentException( "_startAngle is NaN." );
		}
		if ( Double.isInfinite( _startAngle ) ) {
			throw new IllegalArgumentException( "_startAngle is inf." );
		}
		if ( Double.isNaN( _endAngle ) ) {
			throw new IllegalArgumentException( "_endAngle is NaN." );
		}
		if ( Double.isInfinite( _endAngle ) ) {
			throw new IllegalArgumentException( "_endAngle is inf." );
		}

		//描画時間
		double time = 2.0;
		//構成点数
		int num = (int) ( time * 100 );
		Point[] ellipticArc = new Point[num];
		//真円時の角度に変換
		double startAngle = Math.atan2( _major * Math.sin( _startAngle ), _minor * Math.cos( _startAngle ) );
		double endAngle = Math.atan2( _major * Math.sin( _endAngle ), _minor * Math.cos( _endAngle ) );

		if ( _endAngle > Math.toRadians( 180 ) ) {
			endAngle = endAngle + 2 * Math.PI;
		}
		//単振動モデルの計算
		for ( int i = 0; i < num; ++i ) {
			//真円時の角度を等分
			double theta = startAngle + ( i * ( endAngle - startAngle ) / ( num - 1 ) );
			double x = _major * Math.cos( theta );
			double y = _minor * Math.sin( theta );
		  time = i * 0.01;

			Point pos = Point.createXYZ( x, y, 0 );
			TransformMatrix translation = TransformMatrix.translation( _center.x(), _center.y(), _center.z() );
			pos = pos.transform( _posture.product( translation ) );
			ellipticArc[i] = Point.createXYZTF( pos.x(), pos.y(), 0, time, 0 );

		}
		return ellipticArc;
	}

	/**
	 * 単振動モデルを用いて楕円弧状の擬似サンプリング点列を生成します
	 * @param _ellipticArc 楕円弧
	 * @return 楕円形状の擬似サンプリング点列
	 * @throw IllegalArgumentException 楕円弧にnullが指定された場合
	 */
	public static Point[] createEllipticArc( EllipticArc _ellipticArc ) {
		if ( _ellipticArc == null ) {
			throw new IllegalArgumentException( "_ellipticArc is null." );
		}
		return createEllipticArc( _ellipticArc.center(), _ellipticArc.majorRadius(),
			_ellipticArc.minorRadius(), _ellipticArc.range().start(), _ellipticArc.range().end(),
			_ellipticArc.posture() );
	}

	private PseudoSamplingPointsCreater() {
		throw new UnsupportedOperationException( "can not create instance." );
	}
}
