package jp.sagalab.jftk.fragmentation;

import jp.sagalab.jftk.curve.SplineCurve;

/**
 * 楕円弧幾何曲線列にするための最良分割点の探索を行うインタフェースです。
 * @author aburaya
 */
public interface BestDivideParameterSearchAlgorithm{
	
	/**
	 * 同定単位、FSC、探索候補点から最良分割点を探索し、その分割点列を返す。
	 * @param _identificationFragment 同定単位
	 * @param _fsc FSC
	 * @param _searchParameters 探索候補点
	 * @return 最良分割点郡
	 */
	public Double[] getBestDevidedParameters( IdentificationFragment _identificationFragment, 
		SplineCurve _fsc, double[] _searchParameters );
	
}