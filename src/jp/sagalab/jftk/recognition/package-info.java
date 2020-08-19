/** 
 * 幾何曲線認識を行うためのクラスやインターフェースを提供します。
 * <p>
 * {@link jp.sagalab.jftk.recognition.RecognitionResult RecognitionResult} 
 * は幾何曲線認識後であるというFSCI上での状態を表現しています。
 * </p>
 * <p>
 * {@link jp.sagalab.jftk.recognition.Recognizable Recognizable} 
 * インタフェースは幾何曲線認識を定義したインタフェースです。
 * FSCIでは幾何曲線認識のアルゴリズムが複数存在します。
 * このインタフェースを使用することでアルゴリズムを容易に切り替えることができます。
 * </p>
 * @see <span>「Java言語で学ぶデザインパターン入門」</span>
 */
package jp.sagalab.jftk.recognition;
