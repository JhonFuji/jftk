package jp.sagalab.jftk;

import jp.sagalab.jftk.transform.TransformMatrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.transform.Transformable;

/**
 * マルチファジィ点を表すクラスです。
 * <p>
 * マルチファジィ点は複数のファジィ点の共通集合となります。
 * マルチファジィ点の頂点を表す点のファジネスには、共通集合を包含する
 * 最小包含球の半径が設定されます。
 * </p>
 * @author ishiguro
 */
public class MultiPoint extends FuzzySet implements Transformable<MultiPoint>{
	
	/**
	 * ファジィ点列から多重ファジィ点を生成します。
	 * @param _points 多重ファジィ点を構成するファジィ点列
	 * @return 多重ファジィ点
	 * @throws NullPointerException 引数の型にnullが指定された場合にスローします。
	 */
	public static MultiPoint create(Point[] _points){
		if(Arrays.asList(_points ).indexOf(null) > -1){
			throw new IllegalArgumentException();
		}
		Point[] points = _points;
		return new MultiPoint(points);
	}
	
	/**
	 * ファジィ点から一重の多重ファジィ点を生成します。
	 * @param _point 多重ファジィ点を構成するファジィ点
	 * @return 一重の多重ファジィ点
	 * @throws NullPointerException 引数の型にnullが指定された場合にスローします。
	 */
	public static MultiPoint create(Point _point){
		if ( _point == null ) {
			throw new NullPointerException( "_points is null." );
		}
		Point[] point = new Point[]{ _point };
		return new MultiPoint(point);
	}
	
	/**
	 * マルチファジィ点の要素の個数を返します。
	 * @return マルチファジィ点の個数
	 */
	public final int length() {
		return m_points.length;
	}

	/**
	 * 指定されたマルチファジィ点の要素を返します。
	 * @param _index マルチファジィ点を指定する数
	 * @return ファジィ点
	 */
	public final Point getElement( int _index ) {
		return m_points[_index];
	}

	/**
	 * 構成要素を配列で返します。
	 * @return ファジィ点の配列
	 */
	public final Point[] getMultiPointForArray() {
		return m_points.clone();
	}

	/**
	 * 構成要素をjava.util.List型で返します。
	 * @return ファジィ点のjava.util.List
	 */
	public final List<Point> getMultiPointForList() {
		List<Point> list = new ArrayList<Point>( m_points.length );
		list.addAll( Arrays.asList( m_points ) );
		return list;
	}
	
	/**
	 * マルチファジィ点の頂点を返します。
	 * @return マルチファジィ点の頂点
	 */
	public final Point getVertex(){
		if( m_vertex == null){
			m_vertex = calculateVertex(m_points);
		}
		
		return m_vertex;
	}
	
	/**
	 * 頂点のグレードを返す。
	 * @return 頂点のグレード
	 */
	public double getGrade(){
		return calculateGrade();
	}

	@Override
	public TruthValue includedIn( Point _other ) {
		// マルチファジィ点の個数
		int length = length();
		// マルチファジィ点の要素を持つリストを取得
		List<Point> multiPoint = getMultiPointForList();
		// マルチファジィ点に他方の点を追加し、新しいマルチファジィ点を構成する
		multiPoint.add( _other );
		MultiPoint tmpMultiPoint = new MultiPoint( multiPoint.toArray( new Point[ 0 ] ) );
		// 新たに構成したマルチファジィ点の頂点
		Point tmpMultiPointVertex = tmpMultiPoint.getVertex();
		// 許容誤差を設定
		// TODO 許容誤差をどのような値とするか (現状は1.0E-10に設定)
		double tolerance = 1.0E-10;

		// 可能性値
		double pos = 1.0;
		// ファジィ点がファジィ点に含まれる可能性値の導出方法と同様の考え方で可能性値を求める
		// 新たに構成したマルチファジィ点の各要素と頂点との距離を取り、頂点の位置でのファジィ点のグレードを算出
		// そのグレード値の最小値が可能性値となる
		for ( int i = 0; i < tmpMultiPoint.length(); ++i ) {
			Point point = tmpMultiPoint.m_points[i];
			double tmpPos;
			// ファジィ点と頂点との距離
			double distance = point.distance( tmpMultiPointVertex );
			// ある点がクリスプな点(ファジネスが許容誤差以下)である場合
			if( point.fuzziness() < tolerance ){
				// クリスプな点と頂点の距離が0(許容誤差以下)の場合
				if( distance < tolerance ){
					// クリスプな点なのでグレード値は1
					tmpPos = 1.0;
				}else{
					tmpPos = 0.0;
				}
			}
			// ある点がファジィ点である場合
			else{
				tmpPos = ( point.fuzziness() - distance ) / point.fuzziness();
				// 「ファジネス < 距離」の場合はグレード値が負になるので、その場合は0とする
				tmpPos = Math.max( tmpPos, 0.0 );
			}
			pos = ( tmpPos < pos ) ? tmpPos : pos;
		}

		// マルチファジィ点と、他方の点の補集合との可能性値
		double complementaryPos = 1;
		// 他方の点の補集合
		Point.Complement otherComplement = _other.getComplement();
		// マルチファジィ点と、他方の点の補集合との可能性値の計算
		for ( int i = 0; i < length; ++i ) {
			TruthValue tv = m_points[i].includedIn( otherComplement );
			complementaryPos = Math.min( tv.possibility(), complementaryPos );
			// マルチファジィ点のある点と、マルチファジィ点の別の点との可能性値を計算
			for ( int j = 0; j < i; ++j ) {
				tv = m_points[i].includedIn( m_points[j] );
				complementaryPos = Math.min( tv.possibility(), complementaryPos );
			}
		}

		// 導出された可能性値を1から引く
		// これによって、マルチファジィ点と他方の点との必然性値が計算できる
		double nec = 1 - complementaryPos;

		// 区間真理値のインスタンスを生成
		return TruthValue.create( nec, pos );
	}

	@Override
	public MultiPoint transform( TransformMatrix _mat ) {
		// マルチファジィ点を構成する要素数
		int length = length();
		// 変換を行った後のマルチファジィ点
		Point[] points = new Point[ length ];

		for ( int i = 0; i < length; ++i ) {
			points[i] = getElement( i ).transform( _mat );
		}

		return new MultiPoint( points );
	}

	@Override
	public TruthValue includedIn( MultiPoint _other ) {
		// TODO マルチファジィ点がマルチファジィ点に含まれる可能性値、必然性値の計算方法は今後必要になる可能性あり?
		throw new UnsupportedOperationException();
	}

	@Override
	protected Point support( Vector _vector ) {
		// TODO マルチファジィ点におけるサポート写像については現段階において未定義
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public FuzzySetType getFuzzySetType() {
		return FuzzySetType.MULTI;
	}

	/**
	 * マルチファジィ点の各要素における頂点を求めます。
	 * @return 頂点
	 */
	private static Point calculateVertex(Point[] _points) {

		// 頂点
		Point vertex = _points[ 0 ];
		// 頂点のx座標
		double vertexX = vertex.x();
		// 頂点のy座標
		double vertexY = vertex.y();
		// 頂点のz座標
		double vertexZ = vertex.z();
		// 頂点のファジネス
		double fuzziness = vertex.fuzziness();
		// 探索移動量 (倍率)
		double move = 0.5;
		// 探索時での最遠点番号
		int apocenterNum = 0;
		// 探索時での最遠点までの最大距離
		double maxDistance;

		// moveが一定の倍率より小さくなるまで探索を行う
		while ( move > 1.0e-8 ) {
			// 現在のmoveで探索を行う
			for ( int t = 0; t < 50; ++t ) {
				// 変数の初期化
				maxDistance = 0;
				for ( int i = 0; i < _points.length; ++i ) {
					if ( vertex.modifiedDistance( _points[i] ) > maxDistance ) {
						maxDistance = vertex.modifiedDistance( _points[i] );
						apocenterNum = i;
					}
				}
				// 頂点座標の計算
				vertexX += ( _points[apocenterNum].x() - vertex.x() ) * move;
				vertexY += ( _points[apocenterNum].y() - vertex.y() ) * move;
				vertexZ += ( _points[apocenterNum].z() - vertex.z() ) * move;
				fuzziness += ( _points[apocenterNum].fuzziness()- vertex.fuzziness()) * move;
				// vertexのインスタンスを再生成
				vertex = Point.createXYZTF( vertexX, vertexY, vertexZ, Double.NaN, fuzziness );
			}
			move = move / 2.0;
		}

		return vertex;
	}
		
	/**
	 * マルチファジィ点の頂点のグレードを求める。
	 * @return 頂点のグレード
	 */
	private double calculateGrade() {
		double maxGrade = 1;
		
		Point[] points = getMultiPointForArray();

		for (Point point: points){
			double distance = point.distance( getVertex() );
			double fuzziness = point.fuzziness();
			double grade;
			if ( Double.isInfinite( fuzziness ) ) {
				grade = 1;
			} else {
				grade = Math.max( ( fuzziness - distance ) / fuzziness, 0 );
			}
		
			if ( Double.isNaN( grade ) ) {
				grade = 1;
			}
			
			if(grade < maxGrade){
				maxGrade = grade;
			}
		}

		return maxGrade;
	}
	
	/**
	 * この MultiPoint と指定された Object が等しいかどうかを比較します。
	 * @param obj この MultiPoint と比較される Object
	 * @return 指定された Object が、このオブジェクトと構成要素、要素数がまったく同じ MultiPoint である限りtrue
	 */
	@Override
	public boolean equals(Object obj){
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final MultiPoint other = (MultiPoint) obj;
		if( !Arrays.equals( m_points, other.m_points)){
			return false;
		}

		return m_points.length == other.m_points.length;
	}

	/**
	 * この MultiPoint のハッシュコードを返します。
	 * @return この MultiPoint のハッシュコード
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 23 * hash + Arrays.deepHashCode( this.m_points );
		return hash;
	}

	/**
	 * この MultiPoint の文字列表現を返します。
	 * @return 要素数と各構成要素を表す String
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append( "size:[ " ).append( m_points.length ).append( "]\n" );
		for ( Point p : m_points ) {
			builder.append( p.toString() );
		}

		return builder.toString();
	}

	/**
	 * 多重ファジィ点を生成します。
	 * @param _points 多重ファジィ点を構成するファジィ点列
	 */
	private MultiPoint( Point[] _points ) {
		m_points = _points;
		m_vertex = null;
	}
	
	/** マルチファジィ点の構成要素 */
	private final Point[] m_points;
	
	/** 頂点 */
	private Point m_vertex;
}