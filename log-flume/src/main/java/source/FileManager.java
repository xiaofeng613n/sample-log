package source;

import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

/**
 * Created by xiaofeng on 2017/11/3
 * Description:
 */
public class FileManager
{

	private String positionFile;

	private RandomAccessFile positionFileAccess;

	private List<PositionInfo> positionInfoList;

	/*inode,LogFile*/
	private Map<String,LogFile> fileMap;


	public LogFile getLogFile(String inode)
	{
		return fileMap.get(inode);
	}
}