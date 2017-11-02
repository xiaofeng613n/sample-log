package source;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiao on 2017/11/1.
 */
public class LogTailSource extends AbstractSource implements Configurable,EventDrivenSource
{
	private final Logger logger = LoggerFactory.getLogger(LogTailSource.class);

	private String positionFile;

	private ExecutorService executorService = Executors.newScheduledThreadPool(1);

	public void configure(Context context)
	{
		logger.info("");
	}

	@Override
	public synchronized void start()
	{
		super.start();
		PositionFileManager positionFileManager = new PositionFileManager();
		LogReaderWorker logReaderWorker = new LogReaderWorker(positionFileManager,getChannelProcessor());
		LogWatcher logWatcher = new LogWatcher(positionFileManager,logReaderWorker);

		/*executorService.submit(()->{
			try
			{
				logReaderWorker.fireAllFileReadEvent();
				positionFileManager.syncDisk();
				logWatcher.reloadLogFiles();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		});
*/
//		try
//		{
//			logReaderWorker.fireAllFileReadEvent();
//			positionFileManager.syncDisk();
//			logWatcher.reloadLogFiles();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//
//		logReaderWorker.start();
		getChannelProcessor().processEvent(new Event()
		{
			@Override
			public Map<String, String> getHeaders()
			{
				return null;
			}

			@Override
			public void setHeaders(Map<String, String> map)
			{

			}

			@Override
			public byte[] getBody()
			{
				return "xx".getBytes();
			}

			@Override
			public void setBody(byte[] bytes)
			{

			}
		});
	}

	@Override
	public synchronized void stop()
	{
		super.stop();
	}
}
