package jp.sagalab.jftk.transform;

/**
 * 相似変換を行うためのインタフェースです。
 * @author miwa
 * @param <T> 相似変換の対象オブジェクト
 */
public interface Transformable<T> {
	
	/**
	 * 相似変換を行います。
	 * @param _matrix 変換行列
	 * @return 変換後のオブジェクト
	 */
	T transform(TransformMatrix _matrix);
}