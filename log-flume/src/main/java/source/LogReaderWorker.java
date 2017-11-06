package source;


import com.google.common.collect.Maps;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;


import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
@Slf4j
public class LogReaderWorker extends Thread
{
	private volatile boolean isRunning = true;

	@Setter
	private volatile boolean isReloadingFile = false;

	private BlockingQueue<String> readEventQueue = new LinkedBlockingQueue<>();

	private ChannelProcessor channelProcessor;

	private FileManager fileManager;


	public LogReaderWorker(ChannelProcessor channelProcessor, FileManager fileManager)
	{
		this.channelProcessor = channelProcessor;
		this.fileManager = fileManager;
	}

	@Override
	public void run()
	{
		while (isRunning)
		{

			/*List<String> inodeList = fileManager.getLogFileInodes();
			for (String inode : inodeList)
			{
				readLogs(inode);
			}
			fileManager.reloadLogFile();*/
			try {
				String inode = readEventQueue.take();
				readLogs(inode);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private void readLogs(String inode)
	{
		int lineCounter = 0;
		while( true )
		{
			if( isReloadingFile)
			{
				log.info("isReloadingFile:{]" ,isReloadingFile);
				break;
			}

			String content = fileManager.readLogLine(inode);
			if( content ==null || "".equals(content))
			{
				break;
			}
			Map<String,String> headers = Maps.newHashMap();
			headers.put("a","b");
			Event event = EventBuilder.withBody(content.getBytes(),headers);
			this.channelProcessor.processEvent(event);

			System.err.println("content:" + content);
			if( lineCounter ++ > 4)
			{
				break;
			}
		}
	}


	public void fireAllFileReadEvent()
	{
		List<String> inodeList = fileManager.getLogFileInodes();
		inodeList.stream().forEach( c-> readEventQueue.offer(c));
	}


}