package source;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
public class PositionFileManager
{
	private RandomAccessFile randomAccessFile;

	@Getter
	private List<PositionInfo> positionInfoList;

	private Map<String,PositionInfo> inode2PositionInfo;
	private List<String> watchDirs;

	public void updatePosition(String inode, long offset)
	{
		for (PositionInfo positionInfo : positionInfoList)
		{
			if( positionInfo.getInode().equals(inode))
			{
				positionInfo.setOffset(offset);
				break;
			}
		}
	}

	public void syncDisk() throws IOException
	{
		final String content = new Gson().toJson(positionInfoList);
		randomAccessFile.setLength(0);
		randomAccessFile.write(content.getBytes(Charset.forName("utf-8")));
	}

	public void reloadDirFiles() throws IOException
	{
		syncDisk();

		reloadPositionInfo();

		syncDisk();
	}

	private void reloadPositionInfo() throws IOException
	{
		int length = (int) randomAccessFile.length();
		byte[] bytes = new byte[length];
		randomAccessFile.seek(0);
		randomAccessFile.read(bytes);

		final String jsonContent = new String(bytes, Charset.forName("utf-8"));
		if( "".equals(jsonContent))
		{
			positionInfoList = Lists.newArrayList();
		}
		else
		{
			Type listType = new TypeToken<ArrayList<PositionInfo>>(){}.getType();
			positionInfoList = new Gson().fromJson(jsonContent,listType);
		}

		final List<String> allInode = getAllInode(this.watchDirs);

		if( allInode == null || allInode.size() ==0 )
		{
			return;
		}

		for (String inode : allInode)
		{
			if( ! inode2PositionInfo.containsKey(inode) )
			{
				PositionInfo newPostionInfo = new PositionInfo(inode,getFileName(inode,this.watchDirs),0);
				inode2PositionInfo.put(inode,newPostionInfo);
			}
			//
		}
		removePositionInfoIfFileNodeNotExist(positionInfoList,allInode);
	}

	private String getFileName(String inode, List<String> directorys) {
		for (String dir : directorys) {
			final String fileName = TailFileHelper.getFileName(dir, inode);
			if (fileName != null) {
				return fileName;
			}
		}
		return null;
	}
	private void removePositionInfoIfFileNodeNotExist(List<PositionInfo> positionInfoList, List<String> allInode)
	{
	}

	private List<String> getAllInode(List<String> watchDirs)
	{
		return null;
	}
}