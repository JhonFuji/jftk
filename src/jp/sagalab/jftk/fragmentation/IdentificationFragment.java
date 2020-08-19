package jp.sagalab.jftk.fragmentation;

import java.util.Arrays;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.curve.Range;
import jp.sagalab.jftk.curve.SplineCurve;
import jp.sagalab.jftk.transform.TransformMatrix;

/**
 * 同定単位フラグメントを表すクラスです。
 * <p>
 * 同定単位フラグメントはファジィフラグメンテーション法により
 * 移動状態であると判断されたフラグメントです。
 * このクラスはフラグメントの状態をクラスとして表現しています。
 * </p>
 * @author yamaguchi
 */
public class IdentificationFragment extends Fragment {

	/**
	 * 同定単位フラグメントを生成します。
	 * @param _fsc ファジィスプライン曲線
	 * @return 同定フラグメント
	 * @throws IllegalArgumentException Spline曲線がnullの場合
	 */
	public static IdentificationFragment create( SplineCurve _fsc ){
		if( _fsc == null ){
			throw new IllegalArgumentException("_fsc is null");
		}
		return new IdentificationFragment( _fsc );
	}
	
	@Override
	public IdentificationFragment invert() {
		return new IdentificationFragment( curve().invert() );
	}

	@Override
	public IdentificationFragment transform( TransformMatrix _mat ) {
		return new IdentificationFragment( curve().transform( _mat ) );
	}

	/**
	 * この同定単位フラグメントを構成するファジィスプライン曲線を指定された停止点へ延長します。
	 * @param _startingPartitionFrag 始点側の区切りフラグメント
	 * @param _endingPartitionFrag 終点側の区切りフラグメント
	 * @return 区切り点へ延長したファジィスプライン曲線
	 * @see "修士論文「手書き図形認識法FSCIにおける幾何曲線間の連結性に関する研究」大吉 孝明"
	 */
	public SplineCurve extendCurve( PartitionFragment _startingPartitionFrag, PartitionFragment _endingPartitionFrag ){
		SplineCurve fsc = curve();
		Point startingPartitionPoint = _startingPartitionFrag != null ? _startingPartitionFrag.body() : null;
		Point endingPartitionPoint = _endingPartitionFrag != null ? _endingPartitionFrag.body() : null;
		int degree = fsc.degree();
		Range range = fsc.range();

		// 区切りフラグメントがある場合、定義域の末端に多重節点挿入
		if ( _startingPartitionFrag != null ) {
			for ( int i = 0; i < degree; ++i ) {
				fsc = fsc.insertKnot( range.start() );
			}
		}
		if ( _endingPartitionFrag != null ) {
			for ( int i = 0; i < degree; ++i ) {
				fsc = fsc.insertKnot( range.end() );
			}
		}

		// 延長後の節点を算出
		double[] knots = fsc.knots();
		// 定義域の始端に対応する節点列延長前節点列のインデックス
		int knotsStartIndex = 0;
		if ( _startingPartitionFrag != null ) {
			knotsStartIndex = fsc.searchKnotNum( range.start(), degree, knots.length - degree );
			knotsStartIndex -= degree;
		}
		// 定義域の終端に対応する節点列延長前節点列のインデックス
		int knotsEndIndex = knots.length - 1;
		if ( _endingPartitionFrag != null ) {
			knotsEndIndex = fsc.searchKnotNum( range.end(), degree, knots.length - degree );
			if ( range.end() < knots[knots.length - degree] ) {
				--knotsEndIndex;
			}
		}

		// 定義域の範囲に対応した節点列の長さ
		int length = knotsEndIndex - knotsStartIndex + 1;

		// 新たに節点を節点挿入する回数(始点側)
		// 始点側に区切りフラグメントがあれば次数回、なければ０回になる
		int addNumToStart = startingPartitionPoint != null ? degree : 0;

		// 新たに節点を節点挿入する回数(終点側)
		// 終点側に区切りフラグメントがあれば次数回、０回になる
		int addNumToEnd = endingPartitionPoint != null ? degree : 0;

		// 延長後の節点列
		// 延長前の節点列から始側インデックス?終点側インデックス分の節点列をコピー
		double[] extendedKnots = new double[ addNumToStart + length + addNumToEnd ];
		System.arraycopy( knots, knotsStartIndex, extendedKnots, addNumToStart, length );

		// 区切り点へ延長する場合、新しく節点を作る→新しい節点は元の節点間隔で作る
		// 節点間隔 付加節点を除いた節点列で、(節点の最大値-最小値)／(節点数)　で求まる
		double knotInterval = ( knots[knotsEndIndex - ( degree - 1 )] - knots[knotsStartIndex + ( degree - 1 )] )
			/ ( length - 2 * ( degree - 1 ) );

		// 始点側に区切り点がある場合、延長後の節点の先頭に新たらしく節点を作る
		Arrays.fill( extendedKnots, 0, addNumToStart, extendedKnots[addNumToStart] - knotInterval );

		// 終点側に区切り点がある場合、延長後の節点の最後に新しく節点を作る
		Arrays.fill( extendedKnots, extendedKnots.length - addNumToEnd, extendedKnots.length,
			extendedKnots[extendedKnots.length - addNumToEnd - 1] + knotInterval );

		// 延長前の制御点列
		Point[] cp = fsc.controlPoints();
		// 延長後の制御点列
		Point[] extendedCP = new Point[ extendedKnots.length - degree + 1 ];
		// 延長前の定義域に対応した制御点をコピー
		System.arraycopy( cp, knotsStartIndex, extendedCP, addNumToStart, length - ( degree - 1 ) );

		// 始点側に区切り点があった場合、
		// 延長後FSCの制御点列の先頭次数個分が線分となるように制御点を設定
		if ( startingPartitionPoint != null ) {
			// 区切り点
			extendedCP[0] = startingPartitionPoint;
			// 区切り点と始点側端点の内分計算(線形)
			for ( int i = 1; i < degree; ++i ) {
				extendedCP[i] = extendedCP[0].internalDivision( extendedCP[degree], i, degree - i );
			}
		}
		// 終点側に区切り点があった場合、
		// 延長後FSCの制御点列の最後次数個が線分となるように制御点を設定
		if ( endingPartitionPoint != null ) {
			// 区切り点
			extendedCP[extendedCP.length - 1] = endingPartitionPoint;
			// 区切り点と終点側端点の内分計算(線形)
			for ( int i = 1; i < degree; ++i ) {
				extendedCP[ ( extendedCP.length - 1 ) - ( addNumToEnd - i )] =
					extendedCP[ ( extendedCP.length - 1 ) - addNumToEnd].internalDivision( extendedCP[ extendedCP.length - 1], i, degree - i );
			}
		}

		// 延長後のFSCの描画範囲
		double startOfExtendedRange = startingPartitionPoint != null ? extendedKnots[0] : range.start();
		double endOfExtendedRange = endingPartitionPoint != null ? extendedKnots[extendedKnots.length - 1] : range.end();
		Range extendedRange = Range.create( startOfExtendedRange, endOfExtendedRange );

		// 延長後のFSC (次数, 制御点列, 節点列, 描画範囲)
		SplineCurve extendedFSC = SplineCurve.create( degree, extendedCP, extendedKnots, extendedRange );

		return extendedFSC;
	}

	private IdentificationFragment( SplineCurve _fsc ) {
		super( _fsc );
	}
}