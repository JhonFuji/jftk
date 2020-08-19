package jp.sagalab.jftk.recognition;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import jp.sagalab.jftk.Sigmoid;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.rough.CircularRoughCurve;
import jp.sagalab.jftk.curve.rough.EllipticRoughCurve;
import jp.sagalab.jftk.curve.rough.FreeRoughCurve;
import jp.sagalab.jftk.curve.rough.LinearRoughCurve;
import jp.sagalab.jftk.fragmentation.IdentificationFragment;
import jp.sagalab.jftk.reference.ReferenceModel;
import jp.sagalab.jftk.reference.circular.CircularReferenceModel;
import jp.sagalab.jftk.reference.elliptic.EllipticReferenceModel;

/**
 * 七種の幾何曲線を対象として幾何曲線認識を行います。
 * <p>
 * FSCIでは基本的には七種の幾何曲線(線分、円、円弧、楕円、楕円弧、閉自由曲線、開自由曲線)
 * を対象に幾何曲線認識を行います。<br>
 * もし、認識対象を減らす場合は{@link Recognizable}を implements した新たなクラスを
 * 生成して下さい。
 * </p>
 * * <p>
 * 幾何曲線を認識するときは線形、円形、楕円形のレファレンスモデル({@link ReferenceModel})を生成し、
 * そのレファレンスモデルとファジィスプライン曲線の合致度(可能性、必然性({@link TruthValue}))を評価します。
 * その際、合致度を判定するために使用する評価点列をFMPS(Fuzzy Maching Point Set)と呼びます。
 * FMPSの点数が多いほどより厳密な合致度を求めることができます。<br>
 * </p>
 * @author miwa
 */
abstract class PrimitiveCurveRecognizer implements Recognizable{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _fmpsNum FMPSの点数
	 */
	protected PrimitiveCurveRecognizer( int _fmpsNum ) {
		m_fmpsNum = _fmpsNum;
	}

	@Override
	public RecognitionResult recognize( IdentificationFragment _identificationFragment,
		SplineCurve _fsc, Map<String, Sigmoid> _rule ) {

		ReferenceModel[] referenceModels = constructReferenceModels( _fsc );

		// リファレンスモデル構築
		ReferenceModel linear = referenceModels[0];
		ReferenceModel circular = referenceModels[1];
		ReferenceModel elliptic = referenceModels[2];

		// 各種区間真理値をFMPSにより導出
		TruthValue tvL = _fsc.includedIn( linear, m_fmpsNum );
		TruthValue tvC = _fsc.includedIn( circular, m_fmpsNum );
		TruthValue tvE = _fsc.includedIn( elliptic, m_fmpsNum );
		TruthValue tvCl = _fsc.evaluateAtStart().includedIn( _fsc.evaluateAtEnd() );

		// 幾何曲線ごとのグレード値の導出
		Map<PrimitiveType, Double> result = calculate7CurveGrade( tvL, tvC, tvE, tvCl, _rule );

		PrimitiveType[] types = getSortedPrimitiveTypeList( result );
		PrimitiveType type = types[0];
		if ( type.equals( PrimitiveType.POINT ) ) {
			type = types[1];
		}
		System.out.println( type );
		RecognitionResult recogResult = null;
		switch ( type ) {
			case POINT:
			case LINE:
				recogResult = LinearRecognitionResult.create( LinearRoughCurve.create( linear.getCurve() ), type, result );
				break;
			case CIRCLE:
			case CIRCULAR_ARC:
				recogResult = CircularRecognitionResult.create( CircularRoughCurve.create( circular.getCurve(),
					type == PrimitiveType.CIRCLE, ( (CircularReferenceModel) circular ).getNQuartersType() ),
					type, result );
				break;
			case ELLIPSE:
			case ELLIPTIC_ARC:
				recogResult = EllipticRecognitionResult.create( EllipticRoughCurve.create( elliptic.getCurve(),
					type == PrimitiveType.ELLIPSE, ( (EllipticReferenceModel) elliptic ).getNQuartersType() ),
					type, result );
				break;
			case CLOSED_FREE_CURVE:
			case OPEN_FREE_CURVE:
				// TODO 延長後FSCからの延長前FSCの生成
				recogResult = FreeCurveRecognitionResult.create(
					FreeRoughCurve.create( _identificationFragment.curve(), type == PrimitiveType.CLOSED_FREE_CURVE ),
					type, result );
				break;
		}
		recogResult = recognizeNQuartersType( recogResult, _fsc, _rule );
		return recogResult;
	}

	/**
	 * 指定されたファジィスプライン曲線から三種類の仮説モデルを生成します．
	 *
	 * @param _fsc 認識対象のファジィスプライン曲線
	 * @return 線形，円形，楕円形を概形した仮説モデル群
	 */
	abstract ReferenceModel[] constructReferenceModels( SplineCurve _fsc );

	/**
	 * 指定された曲線種のn/4仮説モデルを生成します．
	 * このメソッドは指定される曲線種が"円弧"もしくは"楕円弧"の場合のみ処理を行います．
	 * 1/4形状，2/4形状，3/4形状の三種類を生成します．
	 *
	 * @param _type 曲線種
	 * @param _fsc 認識対象のファジィスプライン曲線
	 * @return n/4形状を概形した仮説モデル群
	 */
	abstract ReferenceModel[] constructReductionModels( PrimitiveType _type, SplineCurve _fsc );

/**
 * 認識された曲線クラスを基にサブ曲線クラスへと細分化して認識します．
 * 指定される曲線クラスが"円弧"もしくは"楕円弧"の場合のみ細分化処理を行います．
 * 
 * @param _recogResult 認識された曲線クラス
 * @param _fsc 認識対象のファジィスプライン曲線
 * @param _rule 推論規則
 * @return 認識結果
 */
	private RecognitionResult recognizeNQuartersType( RecognitionResult _recogResult, SplineCurve _fsc, Map<String, Sigmoid> _rule ) {

		PrimitiveType primType = _recogResult.getType();

		switch ( primType ) {
			case LINE:
			case CIRCLE:
			case ELLIPSE:
			case OPEN_FREE_CURVE:
			case CLOSED_FREE_CURVE:
				return _recogResult;

			case CIRCULAR_ARC:
			case ELLIPTIC_ARC:
				ReferenceModel[] model = constructReductionModels( primType, _fsc );
				ReferenceModel quarter = model[0];
				ReferenceModel half = model[1];
				ReferenceModel threeQuarters = model[2];

				// 各種区間真理値をFMPSにより導出
				TruthValue tvQ = _fsc.includedIn( quarter, m_fmpsNum );
				TruthValue tvH = _fsc.includedIn( half, m_fmpsNum );
				TruthValue tvT = _fsc.includedIn( threeQuarters, m_fmpsNum );

				// 幾何曲線ごとのグレード値の導出
				Map<NQuartersType, Double> result = calculateSubcurveGrade( tvQ, tvH, tvT, _rule );
				NQuartersType[] types = getSortedNQuartersTypeList( result );
				NQuartersType type = types[0];

				//fourQuartersは未実装
				Map<NQuartersType, ReferenceModel> curveMap = new EnumMap<NQuartersType, ReferenceModel>( NQuartersType.class );
				curveMap.put( NQuartersType.QUARTER, quarter );
				curveMap.put( NQuartersType.HALF, half );
				curveMap.put( NQuartersType.THREE_QUARTERS, threeQuarters );

//				System.out.println( type );

				if ( type == NQuartersType.GENERAL ) {
					return _recogResult;
				} else {
					if ( primType == PrimitiveType.CIRCULAR_ARC ) {
						return CircularRecognitionResult.create( CircularRoughCurve.create( curveMap.get( type ).getCurve(), false, type ),
							PrimitiveType.CIRCULAR_ARC, _recogResult.getGradeList() );
					} else {
						if ( primType == PrimitiveType.ELLIPTIC_ARC ) {
							return EllipticRecognitionResult.create( EllipticRoughCurve.create( curveMap.get( type ).getCurve(), false, type ),
								PrimitiveType.ELLIPTIC_ARC, _recogResult.getGradeList() );
						}
					}
				}
		}

		return null;
	}

	/**
	 * 推論規則を用いて各幾何曲線のグレードを求めます。
	 * @param _l 線形性
	 * @param _c 円形性
	 * @param _e 楕円形性
	 * @param _cl 閉曲線性
	 * @param _rules ファジィ推論ルール
	 * @return 幾何曲線種とそのグレード値のペアマップ
	 */
	private static Map<PrimitiveType, Double> calculate7CurveGrade( TruthValue _l,
		TruthValue _c, TruthValue _e, TruthValue _cl, Map<String, Sigmoid> _rules ) {

		Map<PrimitiveType, Double> grades = new EnumMap<PrimitiveType, Double>( PrimitiveType.class );

		// 線形性の可能性値・必然性値
		double linearPos = _l.possibility(), linearNec = _l.necessity();
		// 円形性の可能性値・必然性値
		double circularPos = _c.possibility(), circularNec = _c.necessity();
		// 楕円形性の可能性値・必然性値
		double ellipticPos = _e.possibility(), ellipticNec = _e.necessity();
		// 閉曲線性の可能性値・必然性値
		double closedPos = _cl.possibility(), closedNec = _cl.necessity();

		// 線分のグレード
		double lL = _rules.get( "L_L" ).calculate( linearPos );
		grades.put( PrimitiveType.LINE, and( lL, lL ) );

		// 円のグレード
		double cL = _rules.get( "C_L" ).calculate( linearNec );
		double cC = _rules.get( "C_C" ).calculate( circularPos );
		double cCL = _rules.get( "C_CL" ).calculate( closedPos );
		grades.put( PrimitiveType.CIRCLE, and( cL, cC, cCL ) );

		// 円弧のグレード
		double caL = _rules.get( "CA_L" ).calculate( linearNec );
		double caC = _rules.get( "CA_C" ).calculate( circularPos );
		double caCL = _rules.get( "CA_CL" ).calculate( closedNec );
		grades.put( PrimitiveType.CIRCULAR_ARC, and( caL, caC, caCL ) );

		// 楕円のグレード
		double eL = _rules.get( "E_L" ).calculate( linearNec );
		double eC = _rules.get( "E_C" ).calculate( circularNec );
		double eE = _rules.get( "E_E" ).calculate( ellipticPos );
		double eCL = _rules.get( "E_CL" ).calculate( closedPos );
		grades.put( PrimitiveType.ELLIPSE, and( eL, eC, eE, eCL ) );

		// 楕円弧のグレード
		double eaL = _rules.get( "EA_L" ).calculate( linearNec );
		double eaC = _rules.get( "EA_C" ).calculate( circularNec );
		double eaE = _rules.get( "EA_E" ).calculate( ellipticPos );
		double eaCL = _rules.get( "EA_CL" ).calculate( closedNec );
		grades.put( PrimitiveType.ELLIPTIC_ARC, and( eaL, eaC, eaE, eaCL ) );

		// 閉じた自由曲線のグレード
		double fcL = _rules.get( "FC_L" ).calculate( linearNec );
		double fcC = _rules.get( "FC_C" ).calculate( circularNec );
		double fcE = _rules.get( "FC_E" ).calculate( ellipticNec );
		double fcCL = _rules.get( "FC_CL" ).calculate( closedPos );
		grades.put( PrimitiveType.CLOSED_FREE_CURVE, and( fcL, fcC, fcE, fcCL ) );

		// 開いた自由曲線のグレード
		double foL = _rules.get( "FO_L" ).calculate( linearNec );
		double foC = _rules.get( "FO_C" ).calculate( circularNec );
		double foE = _rules.get( "FO_E" ).calculate( ellipticNec );
		double foCL = _rules.get( "FO_CL" ).calculate( closedNec );
		grades.put( PrimitiveType.OPEN_FREE_CURVE, and( foL, foC, foE, foCL ) );

		return grades;
	}

	private static Map<NQuartersType, Double> calculateSubcurveGrade( TruthValue _q,
		TruthValue _h, TruthValue _t, Map<String, Sigmoid> _rules ) {

		Map<NQuartersType, Double> grades = new EnumMap<NQuartersType, Double>( NQuartersType.class );

		// 線形性の可能性値・必然性値
		double quarterPos = _q.possibility(), quarterNec = _q.necessity();
		// 円形性の可能性値・必然性値
		double halfPos = _h.possibility(), halfNec = _h.necessity();
		// 楕円形性の可能性値・必然性値
		double threeQuartersPos = _t.possibility(), threeQuartersNec = _t.necessity();

		// 1/4形状のグレード
		double qQ = _rules.get( "QUARTER_Q" ).calculate( quarterPos );
		grades.put( NQuartersType.QUARTER, qQ );

		// 2/4形状のグレード
		double hH = _rules.get( "HALF_H" ).calculate( halfPos );
		grades.put( NQuartersType.HALF, hH );

		// 3/4形状のグレード
		double tT = _rules.get( "THREE_QUARTERS_T" ).calculate( threeQuartersPos );
		grades.put( NQuartersType.THREE_QUARTERS, tT );

		// 開いた自由曲線のグレード
		double geQ = _rules.get( "GENERAL_Q" ).calculate( quarterNec );
		double geH = _rules.get( "GENERAL_H" ).calculate( halfNec );
		double geT = _rules.get( "GENERAL_T" ).calculate( threeQuartersNec );
		grades.put( NQuartersType.GENERAL, and( geQ, geH, geT ) );

		return grades;
	}

	private static PrimitiveType[] getSortedPrimitiveTypeList( final Map<PrimitiveType, Double> _grades ) {
		TreeMap<PrimitiveType, Double> map = new TreeMap<PrimitiveType, Double>( new Comparator<PrimitiveType>(){
			@Override
			public int compare( PrimitiveType _primitiveType1, PrimitiveType _primitiveType2 ) {
				double typeValue1 = _grades.get( _primitiveType1 );
				double typeValue2 = _grades.get( _primitiveType2 );
				if ( typeValue1 < typeValue2 ) {
					return 1;
				} // 同じグレード値の場合はより単純な幾何曲線クラスを優先
				else if ( typeValue1 == typeValue2 ) {
					return _primitiveType1.compareTo( _primitiveType2 );
				} else {
					return -1;
				}
			}
		} );
		map.putAll( _grades );
		PrimitiveType[] primitiveTypeList = map.navigableKeySet().toArray( new PrimitiveType[map.size()] );

		return primitiveTypeList;
	}

	private static NQuartersType[] getSortedNQuartersTypeList( final Map<NQuartersType, Double> _grades ) {
		TreeMap<NQuartersType, Double> map = new TreeMap<NQuartersType, Double>( new Comparator<NQuartersType>(){
			@Override
			public int compare( NQuartersType _nQuartersType1, NQuartersType _nQuartersType2 ) {
				double typeValue1 = _grades.get( _nQuartersType1 );
				double typeValue2 = _grades.get( _nQuartersType2 );
				if ( typeValue1 < typeValue2 ) {
					return 1;
				} // 同じグレード値の場合はより単純な幾何曲線クラスを優先
				else if ( typeValue1 == typeValue2 ) {
					return _nQuartersType1.compareTo( _nQuartersType2 );
				} else {
					return -1;
				}
			}
		} );
		map.putAll( _grades );
		NQuartersType[] nQuartersTypeList = map.navigableKeySet().toArray( new NQuartersType[map.size()] );

		return nQuartersTypeList;
	}

	/**
	 * 論理積を行います。
	 * @param _a 値A
	 * @param _b 値B
	 * @return AとBの論理積値
	 */
	private static double and( double _a, double _b ) {
		return Math.min( _a, _b );
	}

	private static double and( double _a, double _b, double _c ) {
		return Math.min( Math.min( _a, _b ), _c );
	}

	private static double and( double _a, double _b, double _c, double _d ) {
		return Math.min( Math.min( _a, _b ), Math.min( _c, _d ) );
	}

	/** FMPSの点数 */
	private final int m_fmpsNum;
}
