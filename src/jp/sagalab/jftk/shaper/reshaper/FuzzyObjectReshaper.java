package jp.sagalab.jftk.shaper.reshaper;

import jp.sagalab.jftk.shaper.ShapedResult;

/**
 * ファジィオブジェクトをリシェイプするインタフェースです。
 * @author yamamoto
 */
public interface FuzzyObjectReshaper{
	
	/**
	 * ファジィオブジェクトをリシェイプします。
	 * @return リシェイプ結果
	 */
	public ShapedResult reshape();
	
}
