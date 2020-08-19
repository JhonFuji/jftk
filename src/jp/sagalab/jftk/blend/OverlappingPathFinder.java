package jp.sagalab.jftk.blend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.sagalab.jftk.Point;
import jp.sagalab.jftk.blend.OverlappingPath.Position;

/**
 * 重複経路を探索するためのクラスです。
 * @author Akira Nishikawa
 */
public class OverlappingPathFinder {
	
	/**
	 * 重複経路を探索します。
	 * @param _existed 既存ファジィ点列
	 * @param _overlapped 重複ファジィ点列
	 * @return 重複経路
	 */
	public static OverlappingPath[] find( Point[] _existed, Point[] _overlapped ) {
		// 重複状態行列(OSM)の生成
		double[][] osm = new double[ _existed.length ][ _overlapped.length ];
		for ( int i = 0; i < _existed.length; ++i ) {
			for ( int j = 0; j < _overlapped.length; ++j ) {
				osm[ i ][ j ] = _existed[ i ].includedIn( _overlapped[ j ] ).possibility();
			}
		}

		// 正順探索
		double[][] wbOSM_A = createWalkBackOSM( osm );
		List<List<Position>> pathsA = searchPaths( wbOSM_A );
		// 逆順探索
		double[][] revOSM = new double[ osm.length ][];
		for ( int i = 0; i < osm.length; ++i ) {
			revOSM[ osm.length - i - 1 ] = osm[ i ];
		}
		double[][] wbOSM_B = createWalkBackOSM( revOSM );
		List<List<Position>> pathsB = searchPaths( wbOSM_B );
		// 逆順の経路はYを逆転
		for ( List<Position> path : pathsB ) {
			for ( int i = 0; i < path.size(); ++i ) {
				path.set( i, new Position( path.get( i ).x(), osm.length - 1 - path.get( i ).y() ) );
			}
		}
		for ( int i = 0; i < revOSM.length; ++i ) {
			revOSM[ revOSM.length - 1 - i ] = wbOSM_B[ i ];
		}
		wbOSM_B = revOSM;

		// 重複経路情報の構築
		OverlappingPath[] olPaths = new OverlappingPath[ pathsA.size() + pathsB.size() ];
		double overlappedAllTime = _overlapped[ _overlapped.length - 1 ].time() - _overlapped[ 0 ].time();
		double overlappedAllLength = Point.length( _overlapped );
		for ( int i = 0; i < pathsA.size(); ++i ) {
			Position[] path = pathsA.get( i ).toArray( new Position[ pathsA.get( i ).size() ] );
			int start = path[ 0 ].x();
			int end = path[ path.length - 1 ].x();
			double possibility = wbOSM_A[ path[ 0 ].y() ][ start ];
			double timeRatio = ( _overlapped[ end ].time() - _overlapped[ start ].time() ) / overlappedAllTime;
			double lengthRatio = Point.length( Arrays.copyOfRange( _overlapped, start, end + 1 ) ) / overlappedAllLength;
			olPaths[ i ] = OverlappingPath.create( path, possibility, timeRatio, lengthRatio );
		}
		for ( int i = 0; i < pathsB.size(); ++i ) {
			Position[] path = pathsB.get( i ).toArray( new Position[ pathsB.get( i ).size() ] );
			int start = path[ 0 ].x();
			int end = path[ path.length - 1 ].x();
			double possibility = wbOSM_B[ path[ 0 ].y() ][ start ];
			double timeRatio = ( _overlapped[ end ].time() - _overlapped[ start ].time() ) / overlappedAllTime;
			double lengthRatio = Point.length( Arrays.copyOfRange( _overlapped, start, end + 1 ) ) / overlappedAllLength;
			olPaths[ i + pathsA.size() ] = OverlappingPath.create( path, possibility, timeRatio, lengthRatio );
		}

		return olPaths;
	}

	/**
	 * 逆探索OSMを生成します。
	 * @param _osm OSM(重複状態行列)
	 * @return 逆探索OSM
	 */
	private static double[][] createWalkBackOSM( double[][] _osm ) {
		// 逆探索OSM
		int rowSize = _osm.length;
		int columnSize = _osm[ 0 ].length;
		double[][] wbOSM = new double[ rowSize ][ columnSize ];

		// まず最後の一行をコピー
		System.arraycopy( _osm, rowSize - 1, wbOSM, rowSize - 1, 1 );
		// 右端二列のコピーと逆探
		for ( int i = rowSize - 2; i >= 0; --i ) {
			// 右端は代入
			wbOSM[ i ][ columnSize - 1 ] = _osm[ i ][ columnSize - 1 ];
			// 右端から二番目は逆探
			double right = _osm[ i ][ columnSize - 1 ];
			double rightDown = _osm[ i + 1 ][ columnSize - 1 ];
			double down = _osm[ i + 1 ][ columnSize - 2 ];
			double adjoiningMax = Math.max( Math.max( right, rightDown ), down );
			wbOSM[ i ][ columnSize - 2 ] = Math.min( adjoiningMax, _osm[ i ][ columnSize - 2 ] );
		}

		// その他の逆探
		for ( int i = rowSize - 2; i >= 0; --i ) {
			for ( int j = columnSize - 3; j >= 0; --j ) {
				if ( _osm[ i ][ j ] > 0 ) {
					double adjoiningMax = Math.max( wbOSM[ i ][ j + 1 ], wbOSM[ i + 1 ][ j ] );
					if ( adjoiningMax > 0 ) {
						wbOSM[ i ][ j ] = Math.min( _osm[ i ][ j ], adjoiningMax );
					}
				}
			}
		}

		return wbOSM;
	}

	/**
	 * 経路を探索します。
	 * @param _wbOSM 逆探索OSM(重複状態行列)
	 * @return 経路
	 */
	private static List<List<Position>> searchPaths( double[][] _wbOSM ) {
		int rowSize = _wbOSM.length;
		int columnSize = _wbOSM[ 0 ].length;

		// 経路探索開始位置の検出
		List<Position> starts = new ArrayList<Position>();
		double pre = 0;
		double now = _wbOSM[ 0 ][ 0 ];
		for ( int i = 0; i < rowSize; ++i ) {
			double post = i + 1 < rowSize ? _wbOSM[ i + 1 ][ 0 ] : 0;
			if ( pre < now && now >= post ) {
				starts.add( new Position( 0, i ) );
			}
			pre = now;
			now = post;
		}
		pre = _wbOSM[ 0 ][ 0 ];
		now = _wbOSM[ 0 ][ 1 ];
		for ( int i = 1; i < columnSize; ++i ) {
			double post = i + 1 < columnSize ? _wbOSM[ 0 ][ i + 1 ] : 0;
			if ( pre < now && now >= post ) {
				starts.add( new Position( i, 0 ) );
			}
			pre = now;
			now = post;
		}

		// 経路探索
		List<List<Position>> paths = new ArrayList<List<Position>>();
		for ( Position start : starts ) {
			Position p = start;
			List<Position> path = new ArrayList<Position>();
			path.add( p );

			// 外縁部に到着するまでループ
			while ( p.x() + 1 < columnSize && p.y() + 1 < rowSize ) {
				// 隣接する一番大きな値のマスに移動
				double right = _wbOSM[ p.y() ][ p.x() + 1 ];
				double rightDown = _wbOSM[ p.y() + 1 ][ p.x() + 1 ];
				double down = _wbOSM[ p.y() + 1 ][ p.x() ];
				if ( right > down ) {
					p = rightDown >= right ? new Position( p.x() + 1, p.y() + 1 ) : new Position( p.x() + 1, p.y() );
				} else {
					p = rightDown >= down ? new Position( p.x() + 1, p.y() + 1 ) : new Position( p.x(), p.y() + 1 );
				}
				// 現在位置を経路に格納
				path.add( p );
			}
			// 検出経路に追加
			paths.add( path );
		}

		return paths;
	}
	
	private OverlappingPathFinder(){
		throw new UnsupportedOperationException("can not create instance.");
	}
}