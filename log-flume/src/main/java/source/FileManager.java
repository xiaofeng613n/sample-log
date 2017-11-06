package source;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.LogFile;
import common.PositionInfo;
import common.TailFileHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xiaofeng on 2017/11/3
 * Description:
 */
public class FileManager
{

	private String positionFile;
	private List<String> watchDirList;

	private RandomAccessFile positionFileAccess;
	private Map<String,PositionInfo> inode2PositionInfo = Maps.newHashMap();
	private Map<String, LogFile> fileMap = Maps.newHashMap();


	public FileManager(String positionFile,List<String> watchDirList) throws IOException
	{
		this.positionFile = positionFile;
		this.watchDirList = watchDirList;

		Path path = Paths.get(positionFile);
		Files.createDirectories(path.getParent());
		this.positionFileAccess = new RandomAccessFile(new File(positionFile),"rw");

		flushPosition(watchDirList);
		loadLogFile();
	}

	public String readLogLine(String inode)
	{
		LogFile logFile = fileMap.get(inode);
		String logLine = logFile.nextLine();
		inode2PositionInfo.get(logFile.getInode()).setOffset(logFile.getOffset());
		return logLine;
	}

	public List<String> getLogFileInodes()
	{
		final Set<String> inodeSet = fileMap.keySet();
		return Lists.newArrayList(inodeSet);
	}

	public void reloadLogFile()
	{
		try
		{
			syncDisk();
			closeAllLogFile();
			flushPosition(watchDirList);
			loadLogFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void loadLogFile()
	{
		for(Map.Entry<String,PositionInfo> entry : inode2PositionInfo.entrySet())
		{
			PositionInfo positionInfo = entry.getValue();
			LogFile logFile = new LogFile(positionInfo.getInode(),positionInfo.getFilename(),positionInfo.getOffset());
			fileMap.put(logFile.getInode(),logFile);
		}
	}

	private void flushPosition(List<String> watchDirs) throws IOException
	{
		int length = (int) positionFileAccess.length();
		byte[] bytes = new byte[length];
		positionFileAccess.seek(0);
		positionFileAccess.read(bytes);

		final String jsonContent = new String(bytes, Charset.forName("utf-8"));
		List<PositionInfo> positionInfoList;
		if( "".equals(jsonContent))
		{
			positionInfoList = Lists.newArrayList();
		}
		else
		{
			Type listType = new TypeToken<ArrayList<PositionInfo>>(){}.getType();
			positionInfoList = new Gson().fromJson(jsonContent,listType);
		}

		// 转map
		inode2PositionInfo = positionInfoList.stream().collect(Collectors.toMap(PositionInfo::getInode,positionInfo -> positionInfo));

		//
		List<String> inodeList = getAllInode(watchDirs);
		for (String inode : inodeList)
		{
			if( !inode2PositionInfo.containsKey(inode))
			{
				final String filename = TailFileHelper.getFileName(inode,this.watchDirList);
				PositionInfo newPostionInfo = new PositionInfo(inode,filename,0);
				inode2PositionInfo.put(inode,newPostionInfo);
			}
		}

		removeNotExistPositionInfo(inodeList,inode2PositionInfo);
	}

	//TODO
	private void removeNotExistPositionInfo(List<String> inodeList, Map<String, PositionInfo> inode2PositionInfo)
	{
		final List<String> historyInodeList = Lists.newArrayList(inode2PositionInfo.keySet());
		final Map<String,String> currentList = inodeList.stream().collect(Collectors.toMap( f ->f ,f -> f));
		List<String> deleteInodeList = Lists.newArrayList();
		for (String inode : historyInodeList )
		{
			if( !currentList.containsKey(inode))
			{
				deleteInodeList.add(inode);
			}
		}
		for (String s : deleteInodeList)
		{
			inode2PositionInfo.remove(s);
		}
	}

	private List<String> getAllInode(List<String> watchDirs)
	{
		final List<String> allInode = new ArrayList<String>();
		for (String dir : watchDirs) {
			final String filterKey = "[^]";//this.getFilterKeyByPath(dir, dirConfig);
			final List<String> nodes = TailFileHelper.getAllInode(dir, filterKey);
			if (nodes != null)
			{
				//logger.info("dir=[" + dir + "],realPath is=[" + TailFileHelper.getRealPath(dir) + "],filterKey=[" + filterKey + "],nodes=" + nodes.toString() + "");
				allInode.addAll(nodes);
			}
		}
		return allInode;
	}

	public void syncDisk() throws IOException
	{
		final String content = new Gson().toJson(inode2PositionInfo.values());
		positionFileAccess.setLength(0);
		positionFileAccess.write(content.getBytes(Charset.forName("utf-8")));
	}

	private void closeAllLogFile() throws IOException
	{
		for( LogFile logFile : fileMap.values())
		{
			logFile.close();
		}
		fileMap.clear();
	}

	public void close() throws IOException
	{
		closeAllLogFile();
		positionFileAccess.close();
	}

}