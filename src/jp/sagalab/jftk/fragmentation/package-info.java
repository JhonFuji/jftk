/**
 * ファジィフラグメンテーション法を行うために必要なクラスやインタフェースを提供します。
 * <p>
 * {@link jp.sagalab.jftk.fragmentation.Fragment Fragment} 
 * クラスはファジィフラグメンテーション法によって分割された単位を定義します。
 * フラグメントはそのフラグメントの状態をクラス(
 * {@link jp.sagalab.jftk.fragmentation.IdentificationFragment IdentificationFragment},
 * {@link jp.sagalab.jftk.fragmentation.PartitionFragment PartitionFragment} )
 * として表現しています。
 * {@link jp.sagalab.jftk.fragmentation.Fragment Fragment} クラスはFSCI上での状態を表現しています。
 * </p>
 * <p>
 * {@link jp.sagalab.jftk.fragmentation.FuzzyFragmentation FuzzyFragmentation} 
 * インタフェースはファジィフラグメンテーション法を定義したインタフェースです。
 * FSCIではファジィフラグメンテーション法のアルゴリズムが複数存在します。
 * このインタフェースを使用することでアルゴリズムを容易に切り替えることができます。
 * </p>
 * @see <span>「Java言語で学ぶデザインパターン入門」</span>
 */
package jp.sagalab.jftk.fragmentation;
