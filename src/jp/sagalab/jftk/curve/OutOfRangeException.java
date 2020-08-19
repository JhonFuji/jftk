package jp.sagalab.jftk.curve;

/**
 * 範囲外に対して何か操作をしようとしたときにスローされるチェック例外です。
 * @author Akira Nishikawa
 */
public class OutOfRangeException extends RuntimeException {

	/**
	 * このクラスのインスタンスを生成します。
	 * @param cause 例外
	 */
	public OutOfRangeException( Throwable cause ) {
		super( cause );
	}

	/**
	 * このクラスのインスタンスを生成します。
	 * @param message メッセージ
	 * @param cause 例外
	 */
	public OutOfRangeException( String message, Throwable cause ) {
		super( message, cause );
	}

	/**
	 * このクラスのインスタンスを生成します。
	 * @param message メッセージ
	 */
	public OutOfRangeException( String message ) {
		super( message );
	}

	/**
	 * このクラスのインスタンスを生成します。
	 */
	public OutOfRangeException() {
	}
	
	private static final long serialVersionUID = -8707608776943720591L;
}
