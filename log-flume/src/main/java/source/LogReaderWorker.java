package source;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Setter;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
public class LogReaderWorker extends Thread
{
	private volatile boolean isRunning = true;

	@Setter
	private volatile boolean isReloadingFile = false;


	private BlockingQueue<String> readEventQueue = new LinkedBlockingQueue<>();

	private ChannelProcessor channelProcessor;

	//private PositionFileManager positionFileManager;

	//private Map<String,LogFile> filemap = Maps.newConcurrentMap();


	private FileManager fileManager;

	public LogReaderWorker(PositionFileManager positionFileManager,ChannelProcessor channelProcessor)
	{
		this.positionFileManager = positionFileManager;
		this.channelProcessor = channelProcessor;
	}

	@Override
	public void run()
	{
		while (isRunning)
		{
			try
			{
				final String inode = readEventQueue.take();
				//final LogFile logFile = filemap.get(inode);
				final LogFile logFile = fileManager.getLogFile(inode);
				if( logFile == null)
				{
					continue;
				}
				doRead(logFile);
				this.positionFileManager.updatePosition(logFile.getInode(),logFile.getOffset());
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void doRead(LogFile logFile)
	{
		final String content = logFile.nextLine();
		Event event = EventBuilder.withBody(content.getBytes());
		this.channelProcessor.processEvent(event);
	}

	public void fireAllFileReadEvent()
	{
		final Set<String> inodeSet = this.filemap.keySet();
		List<String> inodeList = Lists.newArrayList(inodeSet);
		inodeList.stream().forEach( c-> readEventQueue.offer(c));
	}

	public List<PositionInfo> getPositionInfo() {
		List<PositionInfo> infoList = new ArrayList<PositionInfo>();
		final Collection<LogFile> values = filemap.values();
		for (LogFile file : values) {
			PositionInfo info = new PositionInfo(file.getInode(), file.getFilename(), file.getOffset());
			infoList.add(info);
		}
		return infoList;
	}

	public void closeAllFile() throws IOException
	{

		try {
			final Collection<LogFile> values = filemap.values();
			for (LogFile file : values) {
				file.close();
			}
		} finally {
			filemap.clear();
		}
	}

	public void addNewFile(String inode, LogFile tailFile) {
		if (!filemap.containsKey(inode)) {

			this.filemap.put(inode, tailFile);

			final String parentPath = tailFile.getParentPath();
			final Map<String, Object> map = Maps.newHashMap();//this.dir2Config.get(parentPath);
			if (map != null) {

				final String useDefaultLineStart = (String) map.get("config.useDefaultLineStart");
				if (useDefaultLineStart != null && useDefaultLineStart.trim().equalsIgnoreCase("false")) {
					tailFile.setUseDefaultLineStart(false);
				}

				String newLineStart = (String) map.get("config.newLineStart");
				int asiic = 91; //默认是 "["开头
				if (!Strings.isNullOrEmpty(newLineStart)) {
					newLineStart = newLineStart.trim();
					asiic = newLineStart.codePointAt(0);
				}
				tailFile.newLineStartPrefixAscii = asiic;


				String charsetStr = (String) map.get("config.charset");
				if (Strings.isNullOrEmpty(charsetStr)) {
					charsetStr = "utf-8";
				} else {
					try {
						Charset.forName(charsetStr);
					} catch (Throwable e) {
						//ErrorLogCollector.errorLog(logger, "not suppert charset=" + charsetStr, e);
						charsetStr = "utf-8";
					}
				}

				tailFile.charset = Charset.forName(charsetStr);
			}
		}
	}
}