package jp.sagalab.jftk.shaper.snapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.Vector;
import jp.sagalab.jftk.curve.QuadraticBezierCurve;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.curve.primitive.PrimitiveCurve;
import jp.sagalab.jftk.curve.rough.RoughCurve;
import jp.sagalab.jftk.fragmentation.Fragment;
import jp.sagalab.jftk.fragmentation.IdentificationFragment;
import jp.sagalab.jftk.fragmentation.PartitionFragment;
import jp.sagalab.jftk.recognition.BoundingBox;
import jp.sagalab.jftk.recognition.CircularRecognitionResult;
import jp.sagalab.jftk.recognition.EllipticRecognitionResult;
import jp.sagalab.jftk.recognition.NQuarterable;
import jp.sagalab.jftk.recognition.NQuartersType;
import jp.sagalab.jftk.recognition.PrimitiveType;
import jp.sagalab.jftk.recognition.RecognitionResult;
import jp.sagalab.jftk.shaper.ShapedResult;
import jp.sagalab.jftk.transform.SimMatrix;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 単一のレファレンスモデルによる認識結果の始終点をスナッピングすることで整形します。
 * <p>
 * 閉曲線、自由曲線の整形についてはサポートしておらずノースナッピングを返します。
 * </p>
 * @author yoshikawa
 */
public class EndPointsObjectSnapper implements FuzzyObjectSnapper{

	/**
	 * このクラスのインスタンスを生成します。
	 * @param _gridSnapper グリッドスナッピングを行うためのスナッパー<br>
	 * (単一のファジィ集合のスナッピングのみをサポートしたクラスが有効です)
	 * @return インスタンス
	 * @throws IllegalArgumentException 引数の型にnullが指定された場合
	 */
	public static EndPointsObjectSnapper create( FuzzyGridSnapper<Point> _gridSnapper ) {
		if ( _gridSnapper == null ) {
			throw new IllegalArgumentException( " GridSnapper is null. " );
		}
		return new EndPointsObjectSnapper( _gridSnapper );
	}

	@Override
	public ShapedResult snap( RecognitionResult _recognition, GridSpace _grid, SplineCurve _fsc ) {
		RoughCurve model = _recognition.getCurve();
		BoundingBox box = null;
		PrimitiveType type = _recognition.getType();

		// 閉曲線、自由曲線の場合、端点スナッピングは行わない
		if ( type == PrimitiveType.CIRCLE || type == PrimitiveType.ELLIPSE
			|| type == PrimitiveType.OPEN_FREE_CURVE
			|| type == PrimitiveType.CLOSED_FREE_CURVE ) {
			return ShapedResult.create( model.toPrimitive(), new Point[0], new GridSpace[0], box );
		}

		// リダクションモデルの時にバウンディングボックスを生成する
		if ( model instanceof NQuarterable ) {
			NQuartersType reductionType = ( (NQuarterable) model ).getNQuartersType();
			if ( reductionType != NQuartersType.GENERAL ) {
				if ( _recognition.getType() == PrimitiveType.CIRCULAR_ARC ) {
					box = CircularRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
				}
				if ( _recognition.getType() == PrimitiveType.ELLIPTIC_ARC ) {
					box = EllipticRecognitionResult.createBoundingBox( (QuadraticBezierCurve) model.getCurve(), reductionType );
				}
			}
		}

		// スナッピング前の端点
		Point[] snappingPoints = new Point[]{ model.getStart(), model.getEnd() };

		// 法線ベクトルを取得
		Vector snappedNormal = calcSnappedNormal( (QuadraticBezierCurve) model.getCurve(),
			m_gridSnapper, _grid );

		// スナッピングされた点列
		Point[] snappedPoints = new Point[2];

		// スナッピングされた解像度
		GridSpace[] snappedGrid = new GridSpace[2];

		// 各スナッピング点列をスナッピング
		for ( int i = 0; i < snappingPoints.length; ++i ) {
			FuzzyGridSnapper.GridPoint gridPoint = m_gridSnapper.snap( _grid, snappingPoints[i] );
			Point snappedPoint = gridPoint.getPoint();
			// オブジェクトスナッピングにより幾何曲線が一点に縮退することを防ぐ
			snappedPoints[i] = snappedPoint.move( Math.random() * 2.0e-14 - 1.0e-14, Math.random() * 2.0e-14 - 1.0e-14, 0.0 );
			snappedGrid[i] = gridPoint.getGrid();
		}

		// 曲線整形
		PrimitiveCurve snappedPrimitive = model.toSnappedPrimitive( snappingPoints, snappedPoints, snappedNormal );

		// バウンディングボックスがあれば変換する
		if ( box != null ) {
			Point[] cp = ( (QuadraticBezierCurve) model.getCurve() ).controlPoints();
			Vector normal = Vector.createNormal( cp[0], cp[1], cp[2] );
			TransformMatrix matrix = SimMatrix.createByBeforeAfterPoints( snappingPoints[0], snappingPoints[1],
				normal, snappedPoints[0], snappedPoints[1], snappedNormal );

			box = box.transform( matrix );
		}

		return ShapedResult.create( snappedPrimitive, snappedPoints, snappedGrid, box );
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
			Point snappedPoint = _snapper.snap( _grid, cp[i] ).getPoint();
			// オブジェクトスナッピングにより幾何曲線が一点に縮退することを防ぐ
			snappedCP[i] = snappedPoint.move( Math.random() * 2.0e-14 - 1.0e-14, Math.random() * 2.0e-14 - 1.0e-14, 0.0 );
		}
		Vector n = Vector.createNormal( cp[0], cp[1], cp[2] );

		Vector snappedNormal = Vector.createNormal( snappedCP[0], snappedCP[1], snappedCP[2] );
		if ( Double.isInfinite( 1 / snappedNormal.length() ) ) {
			snappedNormal = n;
		}

		return snappedNormal;
	}

	private EndPointsObjectSnapper( FuzzyGridSnapper<Point> _gridSnapper ) {
		m_gridSnapper = _gridSnapper;
	}

	/** グリッドスナッピングを行うためのスナッパー */
	private final FuzzyGridSnapper<Point> m_gridSnapper;
}
