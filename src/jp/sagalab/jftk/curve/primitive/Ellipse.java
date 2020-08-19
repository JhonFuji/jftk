package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.transform.TransformMatrix;
import jp.sagalab.jftk.Vector;

/**
 * 楕円弧を表すクラスです。
 * @author Akira Nishikawa
 * @author ishiguro
 */
public class Ellipse extends EllipticArc {

	/**
	 * 楕円弧を生成します。
	 * @param _center 中心
	 * @param _major 長径
	 * @param _minor 短径
	 * @param _angle 開始角(Radian)
	 * @param _posture 姿勢
	 * @return インスタンス
	 * @throws IllegalArgumentException _centerがnullの場合
	 * @throws IllegalArgumentException _postureがnullの場合
	 */
	public static Ellipse create(Point _center, double _major, double _minor, double _angle,
		TransformMatrix _posture){
		if( _center == null ){
			throw new IllegalArgumentException( "_center is null" );
		}
		if( _posture == null ){
			throw new IllegalArgumentException( "_posture is null" );
		}
		
		return new Ellipse(_center, _major, _minor, _angle, _posture );
	}
	
	@Override
	public Ellipse invert() {
		TransformMatrix mat = TransformMatrix.rotation( Math.PI, Vector.createXYZ( 0, 1, 0 ) );
		TransformMatrix posture = mat.product( posture() );
		Range range = range();
		double start = Math.PI - range.end();
		double end = Math.PI - range.start();
		return new Ellipse( center(), majorRadius(), minorRadius(), start, posture );
	}

	@Override
	public Ellipse transform( TransformMatrix _mat ) {
		Point center = center().transform( _mat );
		double major = majorRadius() * _mat.scalalize();
		double minor = minorRadius() * _mat.scalalize();
		TransformMatrix posture = posture().product( _mat.rotatalize() );
		return new Ellipse( center, major, minor, range().start(), posture );
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	/**
	 * この Ellipse と指定された Object が等しいかどうかを比較します。
	 * @param obj この Ellipse と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 中心、長径、短径、姿勢、閉じているかがまったく同じ Ellipse である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Ellipse other = (Ellipse) obj;
		if ( this.center() != other.center() && ( this.center() == null || !this.center().equals( other.center() ) ) ) {
			return false;
		}
		if ( this.majorRadius() != other.majorRadius() ) {
			return false;
		}
		if ( this.minorRadius() != other.minorRadius() ) {
			return false;
		}
		return this.posture() == other.posture() || ( this.posture() != null && this.posture().equals( other.posture() ) );
	}

	/**
	 * この Ellipse のハッシュコードを返します。
	 * @return この Ellipse のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + ( this.center() != null ? this.center().hashCode() : 0 );
		hash = 59 * hash + (int) ( Double.doubleToLongBits( this.majorRadius() ) ^ ( Double.doubleToLongBits( this.majorRadius() ) >>> 32 ) );
		hash = 59 * hash + (int) ( Double.doubleToLongBits( this.minorRadius() ) ^ ( Double.doubleToLongBits( this.minorRadius() ) >>> 32 ) );
		hash = 59 * hash + ( this.posture() != null ? this.posture().hashCode() : 0 );
		return hash;
	}

	/**
	 * この Circle の文字列表現を返します。
	 * @return 中心、長径、短径、範囲(ラジアン)、姿勢、閉じているかを表す String
	 */
	@Override
	public String toString() {
		return String.format(
			"center:%s major:%.3f minor:%.3f angle:%s posture:%s closed:%s",
			center(), majorRadius(), minorRadius(), range(), posture(), true );
	}
	
	/**
	 * このクラスのインスタンスを生成します。
	 * @param _center 中心
	 * @param _major 長径
	 * @param _minor 短径
	 * @param _angle 開始角と終了角(Radian)
	 * @param _posture 姿勢
	 */
	private Ellipse( Point _center, double _major, double _minor, double _angle,
		TransformMatrix _posture ) {
		super(_center, _major, _minor, Range.create(_angle, _angle + 2 * Math.PI), _posture);
	}
	
}
