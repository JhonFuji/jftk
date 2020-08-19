package jp.sagalab.jftk.curve;

/**
 * 曲線を別の種類の曲線列に変換するためのインタフェースです。
 * @author miwa
 * @param <T> 別の種類の曲線
 */
public interface CurveConvertible<T>{
	
	/**
	 * この曲線を別な種類の曲線列へ変換します。
	 * @return 変換後の曲線列
	 */
	T[] convert();
}
