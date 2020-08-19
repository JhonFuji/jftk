package jp.sagalab.jftk.shaper.snapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.TruthValue;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.fragmentation.Fragment;
import jp.sagalab.jftk.fragmentation.IdentificationFragment;
import jp.sagalab.jftk.fragmentation.PartitionFragment;
import jp.sagalab.jftk.recognition.PrimitiveType;
import jp.sagalab.jftk.recognition.RecognitionResult;
import jp.sagalab.jftk.recognition.CircularRecognitionResult;
import jp.sagalab.jftk.recognition.BoundingBox;
import jp.sagalab.jftk.recognition.EllipticRecognitionResult;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.shaper.ShapedResult;
import jp.sagalab.jftk.transform.AffineMatrix;
import jp.sagalab.jftk.transform.SimMatrix;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 重複度により指示されたファジィオブジェクトをスナッピングします。
 * <p>
 * 線形、円形の幾何曲線に対しては相似変換、楕円形の幾何曲線にはアフィン変換を用いて形状を整形します。
 * この際、形状が破綻をきたすような強制的な整形は行いません。
 * そのために認識結果の特徴点を用いて複数のパターンで整形を行い、
 * 認識元のファジィスプライン曲線と整形された幾何曲線との合致度(可能性、必然性( {@link TruthValue}) )
 * からもっとも高いものを選択するという手法をとっています。<br>
 *
 * 整形にはファジィグリッドスナッピングを用います。<br>
 *
 * 現状、全ての整形パターンで合致度が0となった場合は、整形を行わない結果が返ります。
 * </p>
 * <p>
 * 自由曲線の整形についてはサポートしていません。
 * </p>
 * @author miwa
 */
public class CoaxialityObjectSnapper implements FuzzyObjectSnapper{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _gridSnapper グリッドスナッピングを行うためのスナッパー<br>
	 * (単一のファジィ集合のスナッピングのみをサポートしたクラスが有効です)
	 * @return インスタンス
	 * @throws IllegalArgumentException 引数の型にnullが指定された場合
	 */
	public static CoaxialityObjectSnapper create( FuzzyGridSnapper<Point> _gridSnapper ) {
		if ( _gridSnapper == null ) {
			throw new IllegalArgumentException( " GridSnapper is null. " );
		}
		return new CoaxialityObjectSnapper( _gridSnapper );
	}

	@Override
	public ShapedResult snap( RecognitionResult _recognition, GridSpace _grid, SplineCurve _fsc ) {

		List<SnappingCandidate> snappingCandidateList = _recognition.getSnappingCandidateList();
		// スナッピング候補点がない場合はスナッピングしない
		if ( snappingCandidateList.isEmpty() ) {
			RoughCurve model = _recognition.getCurve();
			BoundingBox boundingBox = null;
			//リダクションモデルの時にバウンディングボックスを生成する
			if ( model instanceof NQuarterable ) {
				NQuartersType reductionType = ( (NQuarterable) model ).getNQuartersType();
				if ( reductionType != NQuartersType.GENERAL ) {
					if ( _recognition.getType() == PrimitiveType.CIRCULAR_ARC
						|| _recognition.getType() == PrimitiveType.CIRCLE ) {
						boundingBox = CircularRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
					}
					if ( _recognition.getType() == PrimitiveType.ELLIPTIC_ARC
						|| _recognition.getType() == PrimitiveType.ELLIPSE ) {
						boundingBox = EllipticRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
					}
				}
			}
			return ShapedResult.create( _recognition.getCurve().toPrimitive(),
				new Point[0], new GridSpace[0], boundingBox );
		}

		List<Point[]> snappedPointsList = new ArrayList<Point[]>( snappingCandidateList.size() );
		List<GridSpace[]> gridSpaceList = new ArrayList<GridSpace[]>( snappingCandidateList.size() );

		// 各スナッピング点列をスナッピング
		for ( SnappingCandidate candidate : snappingCandidateList ) {
			Point[] points = candidate.getPoints();
			Point[] pointSet = new Point[points.length];
			GridSpace[] gridSpaceSet = new GridSpace[points.length];
			for ( int i = 0; i < points.length; ++i ) {
				// 最初の要素の点を用いる
				FuzzyGridSnapper.GridPoint gridPoint = m_gridSnapper.snap( _grid, points[i] );
				pointSet[i] = gridPoint.getPoint();
				gridSpaceSet[i] = gridPoint.getGrid();
			}
			gridSpaceList.add( gridSpaceSet );
			snappedPointsList.add( pointSet );
		}

		// 採用するスナッピング情報のインデックスを求める
		RoughCurve model = _recognition.getCurve();
		int snappingIndex = decideSnapModelIndex( model, _fsc,
			snappingCandidateList, snappedPointsList, m_gridSnapper, _grid );

		Point[] snappedPoints = new Point[0];
		GridSpace[] gridSpaces = new GridSpace[0];
		// スナッピング情報のインデックスが存在するとき、曲線整形を行う
		if ( snappingIndex != -1 ) {
			// 法線ベクトルを取得
			Vector snappedNormal = calcSnappedNormal( (QuadraticBezierCurve) model.getCurve(),
				m_gridSnapper, _grid );
			// スナッピングに用いる変換行列を求める
			Point[] snappingPoints = snappingCandidateList.get( snappingIndex ).getPoints();
			snappedPoints = snappedPointsList.get( snappingIndex );
			gridSpaces = gridSpaceList.get( snappingIndex );

			// 曲線整形
			PrimitiveCurve snappedPrimitive = model.toSnappedPrimitive( snappingPoints, snappedPoints, snappedNormal );
			TransformMatrix matrix = TransformMatrix.identity();
			if ( snappingPoints.length == 2 ) {
				Point[] cp = ( (QuadraticBezierCurve) model.getCurve() ).controlPoints();
				Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
				matrix = SimMatrix.createByBeforeAfterPoints( snappingPoints[0], snappingPoints[1],
					normal, snappedPoints[0], snappedPoints[1], snappedNormal );

			} else if ( snappingPoints.length == 3 ) {
				matrix = AffineMatrix.createBy3Points( snappingPoints, snappedPoints );
			}
			BoundingBox box = snappingCandidateList.get( snappingIndex ).getBox();
			if ( box != null ) {
				box = box.transform( matrix );
			}
			return ShapedResult.create( snappedPrimitive, snappedPoints, gridSpaces, box );
		} else {
			//スナッピング候補があったが、スナッピング結果がノースナッピングになってしまった場合
			//リダクションモデルの時にバウンディングボックスを生成する
			BoundingBox boundingBox = null;
			if ( model instanceof NQuarterable ) {
				PrimitiveType primitiveType = _recognition.getType();
				NQuartersType reductionType = ( (NQuarterable) model ).getNQuartersType();
				if ( reductionType != NQuartersType.GENERAL ) {
					if ( primitiveType == PrimitiveType.CIRCULAR_ARC || primitiveType == PrimitiveType.CIRCLE ) {
						boundingBox = CircularRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
					}
					if ( primitiveType == PrimitiveType.ELLIPTIC_ARC || primitiveType == PrimitiveType.ELLIPSE ) {
						boundingBox = EllipticRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
					}
				}
			}
			return ShapedResult.create(
				model.toPrimitive(), snappedPoints, gridSpaces, boundingBox );
		}
	}

	@Override
	public Map<Fragment, ShapedResult> snap( Map<Fragment, RecognitionResult> _recognitions, List<Fragment> _fragments, GridSpace _gridSpace ) {

		int connectedSize = _fragments.size();
		Map<Fragment, ShapedResult> result = new HashMap<Fragment, ShapedResult>();
		for ( int i = 0; i < connectedSize; ++i ) {
			Fragment fragment = _fragments.get( i );
			if ( fragment.getClass() == IdentificationFragment.class ) {
				PartitionFragment startPartitionFrag = null;
				PartitionFragment endPartitionFrag = null;
				if ( i > 0 ) {
					startPartitionFrag = (PartitionFragment) _fragments.get( i - 1 );
				}
				if ( i < connectedSize - 1 ) {
					endPartitionFrag = (PartitionFragment) _fragments.get( i + 1 );
				}
				IdentificationFragment identificationFragment = (IdentificationFragment) fragment;
				SplineCurve extendsFsc = identificationFragment.extendCurve(
					startPartitionFrag, endPartitionFrag );
				ShapedResult shapedResult = snap( _recognitions.get( fragment ), _gridSpace, extendsFsc );
				result.put( fragment, shapedResult );
			}
		}
		return result;
	}

	/**
	 * スナッピングに使用するスナッピング前後のスナッピング点とグリッドの番号を取得します。
	 * @param _model ラフ曲線
	 * @param _fsc ファジィスプライン曲線
	 * @param _snappingCandidateList スナッピング候補リスト
	 * @param _snappedPoints スナッピング後のスナッピング点
	 * @param _grid グリッド
	 * @param _sigmoidTrue 言語的真理値「真」
	 * @param _sigmoidFalse 言語的真理値「偽」
	 * @return 番号
	 */
	private static int decideSnapModelIndex( RoughCurve _model, SplineCurve _fsc,
		List<SnappingCandidate> _snappingCandidateList, List<Point[]> _snappedPoints,
		FuzzyGridSnapper<Point> _snapper, GridSpace _grid ) {
		//スナッピングする点列の選択
		Vector snappedNormal = calcSnappedNormal( (QuadraticBezierCurve) _model.getCurve(), _snapper, _grid );
		int snapModelIndex = selectSnapingPointsIndex( _model, _snappingCandidateList, _snappedPoints, snappedNormal, _fsc );
		return snapModelIndex;
	}

	/**
	 * スナッピングを行う点列を選択します。
	 * @param _curve 対象の曲線
	 * @param _snappingCandidateList スナッピング候補リスト
	 * @param _snappedPoints スナッピング後の点列リスト
	 * @param _snappedNormal スナッピング後の法線ベクトル
	 * @param _fsc ファジィスプライン曲線
	 * @return スナッピングを行う点列のインデックス
	 * @throws UnsupportedOperationException 点列の要素数が2点、3点ではない場合
	 */
	private static int selectSnapingPointsIndex( RoughCurve _curve,
		List<SnappingCandidate> _snappingCandidateList, List<Point[]> _snappedPoints,
		Vector _snappedNormal, SplineCurve _fsc ) {
		int size = _snappingCandidateList.size();
		QuadraticBezierCurve[] snappedModels = new QuadraticBezierCurve[size];

		for ( int i = 0; i < size; ++i ) {
			Point[] snappingPoints = _snappingCandidateList.get( i ).getPoints();
			Point[] snappedPoints = _snappedPoints.get( i );
			QuadraticBezierCurve snappedModel = _curve.toSnappedModel( snappingPoints, snappedPoints, _snappedNormal );
			snappedModels[i] = snappedModel;
		}

		// 重複度スナッピングの閾値
		// TODO 重複度スナッピングの閾値をFSCIParameterで定義する
		double multiplicityThreshold = 0.1;

		//FSCとの重複度から最も可能性値の高いスナッピング結果を求める
		double maxPossibility = -1.0;
		int index = -1;
		for ( int i = 0; i < snappedModels.length; ++i ) {
			if ( snappedModels[i] != null ) {
				//TODO fmps点数をFSCIParameterで定義する
				TruthValue tv = _fsc.includedIn( snappedModels[i], 20 );
				double possibility = tv.possibility();
				if ( maxPossibility < possibility ) {
					maxPossibility = possibility;
					index = i;
				}
			}
		}

		if ( maxPossibility < multiplicityThreshold ) {
			index = -1;
		}

		return index;
	}

	/**
	 * 目標法線ベクトルを求めます。
	 * @param _curve 対象の曲線
	 * @param _snapper グリッド情報
	 * @param _grid グリッド格子点のファジネスの拡大縮小率
	 * @return スナッピング後の法線ベクトル
	 */
	private static Vector calcSnappedNormal( QuadraticBezierCurve _curve,
		FuzzyGridSnapper<Point> _snapper, GridSpace _grid ) {
		Point[] cp = _curve.controlPoints();
		Point[] snappedCP = new Point[cp.length];
		for ( int i = 0; i < cp.length; ++i ) {
			snappedCP[i] = _snapper.snap( _grid, cp[i] ).getPoint();
		}
		Vector n = Vector.createNormal( cp[0], cp[1], cp[2] );

		Vector snappedNormal = Vector.createNormal( snappedCP[0], snappedCP[1], snappedCP[2] );
		if ( Double.isInfinite( 1 / snappedNormal.length() ) ) {
			snappedNormal = n;
		}

		return snappedNormal;
	}

	private CoaxialityObjectSnapper( FuzzyGridSnapper<Point> _gridSnapper ) {
		m_gridSnapper = _gridSnapper;
	}

	/** グリッドスナッピングを行うためのスナッパー */
	private final FuzzyGridSnapper<Point> m_gridSnapper;
}
