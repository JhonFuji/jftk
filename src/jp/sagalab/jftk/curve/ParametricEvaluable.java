package jp.sagalab.jftk.curve;

import jp.sagalab.jftk.FuzzySet;

/**
 * あるパラメータ範囲でパラメータによる評価を行うためのインタフェースを提供します。
 * @author miwa
 * @param <T> ファジィ集合
 */
public interface ParametricEvaluable <T extends FuzzySet>{
	
	/** 評価の種類を定義する識別子です。 */
	public static enum EvaluationType {
		/** 等時間間隔評価 */
		TIME,
		/** 等距離間隔評価 */
		DISTANCE
	};

	/**
	 * 指定されたパラメータに対応する点を評価します。
	 * @param _parameter パラメータ 
	 * @return 評価点
	 * @throws OutOfRangeException 指定パラメータが曲線のパラメータ範囲外の場合
	 */
	T evaluateAt( double _parameter ) throws OutOfRangeException;

	/**
	 * パラメータ範囲全体に対応する点列を評価します。
	 * @param _num 評価数
	 * @param _type 評価タイプ
	 * @return 評価点列
	 * @throws OutOfRangeException 評価数が２未満の場合
	 */
	T[] evaluateAll( int _num, EvaluationType _type ) throws OutOfRangeException;

	/**
	 * 始点（パラメータ範囲の開始値に対応する点）を評価します。
	 * @return 始点
	 */
	T evaluateAtStart();

	/**
	 * 終点（パラメータ範囲の終了値に対応する点）を評価します。
	 * @return 終点
	 */
	T evaluateAtEnd();
	
	/**
	 * パラメータの範囲を返します。
	 * @return パラメータの範囲
	 */
	Range range();
}
