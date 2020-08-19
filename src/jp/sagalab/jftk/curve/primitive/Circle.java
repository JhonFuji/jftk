package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 円を表すクラスです。
 * @author Akira Nishikawa
 * @author ishiguro
 */
public class Circle extends CircularArc {
	
	/**
	 * 円を生成します。
	 * @param _center 中心
	 * @param _radius 半径
	 * @param _posture 姿勢
	 * @return インスタンス
	 * @throws IllegalArgumentException _centerがnullの場合
	 * @throws IllegalArgumentException _postureがnullの場合
	 */
	public static Circle create( Point _center, double _radius, TransformMatrix _posture ){
		if(_center == null){
			throw new IllegalArgumentException( "_center is null" );
		}
		if(_posture == null){
			throw new IllegalArgumentException( "_posture is null" );
		}
		
		return new Circle(_center, _radius, _posture );
	}
	
	@Override
	public Circle invert() {
		TransformMatrix posture = TransformMatrix.rotation( Math.PI, Vector.createXYZ( 0, 1, 0 ) ).product( posture() );
		double[][] elements = posture.elements();
		double angle = range().end();
		double start = Math.PI - angle;
		TransformMatrix mat = TransformMatrix.rotation( start, Vector.createXYZ( elements[2][0], elements[2][1], elements[2][2] ) );
		return new Circle( center(), radius(), posture.product( mat ) );
	}

	@Override
	public Circle transform( TransformMatrix _mat ) {
		Point center = center().transform( _mat );
		double radius = radius() * _mat.scalalize();
		TransformMatrix posture = posture().product( _mat.rotatalize() );
		
		return new Circle(center, radius, posture );
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	/**
	 * この Circle と指定された Object が等しいかどうかを比較します。 
	 * @param obj この Circle と比較される Object
	 * @return 指定された Object が、このオブジェクトと
	 * 中心、半径、姿勢、閉じているかがまったく同じ Circle である限りtrue
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final Circle other = (Circle) obj;
		if ( this.center() != other.center() && ( this.center() == null || !this.center().equals( other.center() ) ) ) {
			return false;
		}
		if ( this.radius() != other.radius() ) {
			return false;
		}
		return this.posture() == other.posture() || ( this.posture() != null && this.posture().equals( other.posture() ) );
	}

	/**
	 * この Circle のハッシュコードを返します。 
	 * @return この Circle のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + ( this.center() != null ? this.center().hashCode() : 0 );
		hash = 47 * hash + (int) ( Double.doubleToLongBits( this.radius() ) ^ ( Double.doubleToLongBits( this.radius() ) >>> 32 ) );
		hash = 47 * hash + ( this.posture() != null ? this.posture().hashCode() : 0 );
		return hash;
	}

	/**
	 * この Circle の文字列表現を返します。
	 * @return 中心、半径、範囲(ラジアン)、姿勢、閉じているかを表す String
	 */
	@Override
	public String toString() {
		return String.format( "center:%s radius:%.3f angle:%s posture:%s closed:%s", center(), radius(), range(), posture(), true );
	}
	
	/**
	 * 円を生成します。
	 * @param _center 中心
	 * @param _radius 半径
	 * @param _posture 姿勢
	 */
	private Circle( Point _center, double _radius, TransformMatrix _posture ){
		super(_center, _radius, 2 * Math.PI, _posture);
	}
	
}
