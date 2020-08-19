package jp.sagalab.jftk.curve.primitive;

import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 点を表すクラスです。
 * @author warita
 */
public class Point extends Line{

	/**
	 * 点を生成します。
	 * @param _start 始点
	 * @param _end 終点
	 * @return インスタンス
	 */
	public static Point create(jp.sagalab.jftk.Point _start, jp.sagalab.jftk.Point _end){
		return new Point(_start, _end);
	}

	@Override
	public Line invert() {
		return new Point(end(), start());
	}

	@Override
	public Line transform( TransformMatrix _mat ) {
		jp.sagalab.jftk.Point start = start().transform( _mat );
		jp.sagalab.jftk.Point end = end().transform( _mat );
		return new Point(start,end);
	}
	
	@Override
	public boolean isClosed() {
		return true;
	}
	
	/**
	 * 点を生成します。
	 * @param _start 始点
	 * @param _end 終点
	 */
	protected Point( jp.sagalab.jftk.Point _start, jp.sagalab.jftk.Point _end ) {
		super( _start, _end);
	}
	
}
