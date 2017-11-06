package common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by xiao on 2017/11/1.
 */
@Data
@Slf4j
public class LogFile
{
	private static final Logger logger = LoggerFactory.getLogger(LogFile.class);

	private static final int Enter_Ascii = 10;

	/**
	 * 标识新的一行开始的字符ascii编码
	 */
	public int newLineStartPrefixAscii = 91;

	/**
	 * 是否使用默认的换行符
	 */
	private boolean useDefaultLineStart = true;


	public Charset charset;

	private int maxLineSize = 1000;

	private String inode;
	private String filename;
	private String parentPath;
	private long offset;
	private BufferedRandomAccessFile randomAccessFile;
	private volatile boolean closed = false;

	private byte[] tempByteArray = new byte[maxLineSize];

	public LogFile(String inode, String filename, long offset) {
		try {
			this.inode = inode;
			this.filename = filename;
			this.offset = offset;
			this.randomAccessFile = new BufferedRandomAccessFile(filename, "r");
			this.randomAccessFile.seek(offset);

			//获取目录名称..
			// final int index = filename.lastIndexOf("/");
			final int index = filename.lastIndexOf("\\");
			this.parentPath = filename.substring(0, index);
		} catch (IOException e) {
			//ErrorLogCollector.errorLog(logger, "LogCategoryTailFile construtor catch Exception", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void close() throws IOException {
		closed = true;
		if (this.randomAccessFile != null) {
			this.randomAccessFile.close();
		}
	}

	public String nextLine() {
		try {
			if (useDefaultLineStart) {
				return readLineDefault();
			} else {
				return readLine();
			}
		} catch (Throwable t) {
			//ErrorLogCollector.errorLog(logger, "readLine catch Exception.fileName=[" + this.filename + "],", t);
		}
		return null;
	}

	private String readLineDefault() throws IOException {
		if (closed) {
			return null;
		}

		final String s = randomAccessFile.readLineUseDefault();
		offset = randomAccessFile.getFilePointer();
		return s;
	}

	private String readLine() throws IOException {
		int c;
		int readChars = 0;

		while (!closed && ((c = randomAccessFile.read()) != -1)) {

			if (readChars >= maxLineSize - 5) {
				//ErrorLogCollector.errorLog(logger, "line length exceed maxLength.[" + TailDirConfig.maxLineSize + "],fileName=[" + this.filename + "],line content=[" + new String(tempByteArray, 0, TailDirConfig.maxLineSize % 500, charset == null ? Charset.forName("utf-8") : charset) + "]", null);
				randomAccessFile.seek(randomAccessFile.getFilePointer() - 1);
				break;
			}

			//读到回车
			if (c == Enter_Ascii) {
				tempByteArray[readChars++] = (byte) c;
				//再往后读取一个字节
				c = randomAccessFile.read();
				if (c == -1) {
					break;
				} else if (c == newLineStartPrefixAscii) {    //retreat one step
					randomAccessFile.seek(randomAccessFile.getFilePointer() - 1);
					break;
				} else if (c == Enter_Ascii) {
					randomAccessFile.seek(randomAccessFile.getFilePointer() - 1);
				} else {
					tempByteArray[readChars++] = (byte) c;
				}
			} else {
				tempByteArray[readChars++] = (byte) c;
			}
		}


		if (readChars > 0) {
			offset = randomAccessFile.getFilePointer();
			return new String(tempByteArray, 0, readChars, charset == null ? Charset.forName("utf-8") : charset);
		} else {
			return null;
		}
	}

	public String getInode() {
		return inode;
	}


	public String getFilename() {
		return filename;
	}


	public long getOffset() {
		return offset;
	}


	public String getParentPath() {
		return parentPath;
	}

	public void setUseDefaultLineStart(boolean useDefaultLineStart) {
		this.useDefaultLineStart = useDefaultLineStart;
		logger.info("useDefaultLineStart=[{}]", this.useDefaultLineStart);
	}
}
