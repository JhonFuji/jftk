/**
 * レファレンスモデルを定義するクラスやレファレンスモデル生成を定義したインタフェースを
 * 提供します。
 * <p>
 * {@link jp.sagalab.jftk.reference.ReferenceModelGenerator ReferenceModelGenerator}
 * インタフェースはレファレンスモデル生成を定義したインタフェースです。
 * FSCIではレファレンスモデル生成のアルゴリズムが複数存在します。
 * このインタフェースを使用することでアルゴリズムを容易に切り替えることができます。
 * また，レファレンスモデルはn/4形状に分割された典型的な形状を表現することができます。
 * </p>
 */
package jp.sagalab.jftk.reference;
