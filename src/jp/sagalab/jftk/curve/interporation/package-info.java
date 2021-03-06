/**
 * ベジェ曲線補間やスプライン曲線補間を行うためのクラスを提供します。
 * <p>
 * 内挿、外挿処理はファジィスプライン曲線を生成する際に、端点のファジネスの暴れを減らす
 * ことを目的としています。基本的にそれ以外の用途で使用することはありません。
 * </p>
 * <p>
 * このパッケージに含まれる全てのクラスはインスタンスの生成を禁止しています。
 * static なメソッドを使用するのみとなります。
 * </p>
 */
package jp.sagalab.jftk.curve.interporation;