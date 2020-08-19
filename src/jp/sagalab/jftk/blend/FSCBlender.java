package jp.sagalab.jftk.blend;

import jp.sagalab.jftk.curve.SplineCurve;

/**
 * ある曲線と他方の曲線を融合するためのインタフェースです。
 * <p>
 * 曲線の融合にあたり、重複部分を検出するために、ファジィスプライン曲線を用いています。
 * 融合はスプライン曲線に対して行っており、融合ができない場合にはnullを返しています。
 * 既存の曲線の末尾に重ね書く場合、
 * 既存の曲線の先頭に重ね書く場合、
 * 既存の曲線全体に対して重ね書く場合、
 * 既存の曲線の一部に対して重ね書く場合以外はサポートしていません。
 * </p>
 * @author yamaguchi
 */
public interface FSCBlender {
	
	/**
	 * この曲線と他方の曲線を融合します。
	 * @param _thisSplineCurve この曲線
	 * @param _otherSplineCurve 他方の曲線
	 * @param _weightA この曲線の重み
	 * @param _weightB 他方の曲線の重み
	 * @return 両曲線を融合した結果の曲線
	 */
	public SplineCurve createBlendedFsc( SplineCurve _thisSplineCurve, SplineCurve _otherSplineCurve, double _weightA, double _weightB );
}