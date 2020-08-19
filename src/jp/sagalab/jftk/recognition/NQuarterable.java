package jp.sagalab.jftk.recognition;

/**
 * リファレンスモデルの簡約化を行うためのインターフェースです。
 * @author nakajima
 */
public interface NQuarterable {
	/**
	 * 簡約型を返します。
	 * @return 曲線の簡約型
	 */
	NQuartersType	getNQuartersType();
}