/**
 * スナッピングを行うためのクラスやインタフェースを提供します。
 * <p>
 * {@link jp.sagalab.jftk.shaper.ShapedResult SnappedResult} 
 * はスナッピング後の認識結果を表現しています。
 * </p>
 * <p>
 * {@link jp.sagalab.jftk.shaper.snapper.FuzzyObjectSnapper FuzzyObjectSnapper} 
 * インタフェースは幾何曲線整形を定義したインタフェースです。
 * FSCIでは幾何曲線整形のアルゴリズムが複数存在します。
 * このインタフェースを使用することでアルゴリズムを容易に切り替えることができます。
 * </p>
 * <p>
 * {@link jp.sagalab.jftk.shaper.snapper.FuzzyGridSnapper FuzzyGridSnapper} 
 * インタフェースはファジィグリッドスナッピングを定義したインタフェースです。
 * FSCIではファジィグリッドスナッピングのアルゴリズムが複数存在します。
 * このインタフェースを使用することでアルゴリズムを容易に切り替えることができます。
 * </p>
 */
package jp.sagalab.jftk.shaper.snapper;