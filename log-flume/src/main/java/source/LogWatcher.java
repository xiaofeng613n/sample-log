package source;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
@Slf4j
public class LogWatcher
{
	private PositionFileManager positionFileManager;
	private LogReaderWorker logReaderWorker;

	public LogWatcher(PositionFileManager positionFileManager, LogReaderWorker logReaderWorker)
	{
		this.positionFileManager = positionFileManager;
		this.logReaderWorker = logReaderWorker;
	}

	public void reloadLogFiles() throws IOException
	{
		this.logReaderWorker.setReloadingFile(true);

		this.positionFileManager.reloadDirFiles();

		resetTailFiles();

		this.logReaderWorker.setReloadingFile(false);

	}

	/**
	 * 重新设置tailFile，
	 * 1.把offset信息刷入磁盘，
	 * 2.关闭老的tailfile
	 * 3.重新添加tailfile
	 **/
	private void resetTailFiles() throws IOException
	{
		//1.更新offset
		final List<PositionInfo> curPositionInfos = this.logReaderWorker.getPositionInfo();
		for (PositionInfo info : curPositionInfos) {
			this.positionFileManager.updatePosition(info.getInode(), info.getOffset());
		}
		this.positionFileManager.syncDisk();

		//2.关闭旧的文件
		this.logReaderWorker.closeAllFile();

		//3.重新加入文件.
		final List<PositionInfo> positionInfos = this.positionFileManager.getPositionInfoList();
		for (PositionInfo info : positionInfos) {
			String inode = info.getInode();
			LogFile tailFile = new LogFile(inode, info.getFilename(), info.getOffset());
			this.logReaderWorker.addNewFile(inode, tailFile);
		}
	}
}