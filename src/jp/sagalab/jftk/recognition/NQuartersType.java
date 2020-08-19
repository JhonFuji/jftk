package jp.sagalab.jftk.recognition;

/**
 * ファジィ楕円弧の種類を表す識別子です。
 * <p>
 * ファジィ楕円弧にはn/4形の４種類と一般形の１種類があります。
 * </p>
 * @author miwa
 */
public enum NQuartersType {
	/** 1 / 4 */
	QUARTER,
	/** 1 / 2 */
	HALF,
	/** 3 / 4 */
	THREE_QUARTERS,
	/** 4 / 4 */
	FOUR_QUARTERS,
	/** 一般形 */
	GENERAL
}
