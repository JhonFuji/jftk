package jp.sagalab.jftk.recognition;

/**
 * 幾何曲線の種類を表す識別子です。
 * @author nakajima
 */
public enum PrimitiveType {
	/** 点 */
	POINT,
	/** 線分 */
	LINE,
	/** 円 */
	CIRCLE,
	/** 円弧 */
	CIRCULAR_ARC,
	/** 楕円 */
	ELLIPSE,
	/** 楕円弧 */
	ELLIPTIC_ARC,
	/** 閉自由曲線 */
	CLOSED_FREE_CURVE,
	/** 開自由曲線 */
	OPEN_FREE_CURVE
}
