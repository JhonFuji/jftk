package jp.sagalab.jftk.recognition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jp.sagalab.jftk.Sigmoid;

/**
 * 推論規則を表すクラスです。
 * <p>
 * FSCIの推論規則は言語的真理値T(Unitary True), F(Unitary False)を用いたファジィ命題を含みます。
 * 言語的真理値T, Fにはシグモイド関数が用いられています。
 * このクラスは各曲線種(点、線分、円、円弧、楕円、楕円弧、閉自由曲線、開自由曲線)のT, Fを定義しています。
 * </p>
 * <p>
 * 基本的には sampleRule を使用します。
 * sampleRule はファジィニューラルネットを用いた学習により調節されたものです。<br>
 * 調節されていない推論規則で幾何曲線認識を行いたい場合は defaultRule を使用します。
 * </p>
 * @author miwa
 */
public class FuzzyRule {
		
	/**
	 * サンプルのファジィ推論規則を返します。
	 * @return サンプルのファジィ推論規則
	 */
	public static Map<String,Sigmoid> sampleRule() {		
		if ( c_sampleRule == null ) {
			Map<String,Sigmoid > rule = new HashMap<String, Sigmoid>();
			rule.put( "L_L", Sigmoid.create(11.0, -6.7 ) ); 
			rule.put( "C_L", Sigmoid.create(-6.6, 4.0 ) ); rule.put( "C_C", Sigmoid.create( 14.0, -8.3 ) ); rule.put( "C_CL", Sigmoid.create( 11.9, -4.9 ) );
			rule.put( "CA_L", Sigmoid.create(-14.1, 1.8 ) ); rule.put( "CA_C", Sigmoid.create( 12.3, -9.4 ) ); rule.put( "CA_CL", Sigmoid.create( -16.0, 1.8 ) );
			rule.put( "E_L", Sigmoid.create(-6.6, 3.3 ) ); rule.put( "E_C", Sigmoid.create( -21.5, 1.5 ) ); rule.put( "E_E", Sigmoid.create( 10.1, -5.7 ) );	rule.put( "E_CL", Sigmoid.create( 9.7, -4.4 ) );
			rule.put( "EA_L", Sigmoid.create(-6.6, 3.3 ) ); rule.put( "EA_C", Sigmoid.create( -14.0, 3.7 ) ); rule.put( "EA_E", Sigmoid.create( 11.8, -7.8 ) ); rule.put( "EA_CL", Sigmoid.create( -6.6, 1.3 ) );
			rule.put( "FC_L", Sigmoid.create(-6.6, 3.3 ) ); rule.put( "FC_C", Sigmoid.create( -6.6, 3.5 ) ); rule.put( "FC_E", Sigmoid.create( -18.4, 1. ) ); rule.put( "FC_CL", Sigmoid.create( 10.0, -3.3 ) );
			rule.put( "FO_L", Sigmoid.create(-6.6, 3.3 ) ); rule.put( "FO_C", Sigmoid.create( -11.1, 2.8 ) ); rule.put( "FO_E", Sigmoid.create( -11.3, 1.6 ) ); rule.put( "FO_CL", Sigmoid.create( -15.9, 1.4 ) );
			c_sampleRule = rule;
		}
		return Collections.unmodifiableMap( c_sampleRule );
	}
	
	/**
	 * 学習要素を用いないデフォルトのファジィ推論規則を返します。
	 * @return デフォルトのファジィ推論規則
	 */
	public static Map<String,Sigmoid> defaultRule() {
		if ( c_sampleRule == null ) {
			double tsWeight = 6.6;
			double fsWeight = -6.6;
			double tsTheta = -3.3;
			double fsTheta = 3.3;
			Map<String, Sigmoid> rule = new HashMap<String, Sigmoid>();		
			rule.put( "L_L", Sigmoid.create( tsWeight, tsTheta ) ); 
			rule.put( "C_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "C_C", Sigmoid.create( tsWeight, tsTheta ) ); rule.put( "C_CL", Sigmoid.create( tsWeight, tsTheta ) );
			rule.put( "CA_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "CA_C", Sigmoid.create( tsWeight, tsTheta ) ); rule.put( "CA_CL", Sigmoid.create( fsWeight, fsTheta ) );
			rule.put( "E_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "E_C", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "E_E", Sigmoid.create( tsWeight, tsTheta ) );	rule.put( "E_CL", Sigmoid.create( tsWeight, tsTheta ) );
			rule.put( "EA_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "EA_C", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "EA_E", Sigmoid.create( tsWeight, tsTheta ) ); rule.put( "EA_CL", Sigmoid.create( fsWeight, fsTheta ) );
			rule.put( "FC_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FC_C", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FC_E", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FC_CL", Sigmoid.create( tsWeight, tsTheta ) );
			rule.put( "FO_L", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FO_C", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FO_E", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "FO_CL", Sigmoid.create( fsWeight, fsTheta ) );
		
			rule.put( "QUARTER_Q", Sigmoid.create( tsWeight, tsTheta ) ); 
			rule.put( "HALF_H", Sigmoid.create( tsWeight, tsTheta ) ); 
			rule.put( "THREE_QUARTERS_T", Sigmoid.create( tsWeight, tsTheta ) ); 
			rule.put( "GENERAL_Q", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "GENERAL_H", Sigmoid.create( fsWeight, fsTheta ) ); rule.put( "GENERAL_T", Sigmoid.create( fsWeight, fsTheta ) );
			
			c_sampleRule = rule;
		}
		return Collections.unmodifiableMap( c_sampleRule );
	}
	
	private FuzzyRule(){
		throw new UnsupportedOperationException("can not create instance.");
	}
	
	/** サンプルルール */
	private static Map<String,Sigmoid> c_sampleRule;
}
