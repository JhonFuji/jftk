package jp.sagalab.jftk.curve;

/**
 * 向きを反転させるために必要なインタフェースです。
 * @author miwa
 * @param <T> 反転させるオブジェクト
 */
public interface Invertible<T> {
	
	/**
	 * このオブジェクトの向きを反転させます。
	 * @return 反転したオブジェクト
	 */
	public T invert();
}
