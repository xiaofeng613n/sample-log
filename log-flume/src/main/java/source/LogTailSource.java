package source;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiao on 2017/11/1.
 */
public class LogTailSource extends AbstractSource implements Configurable,EventDrivenSource
{
	private final Logger logger = LoggerFactory.getLogger(LogTailSource.class);

	private String positionFile;

	private List<String> watchDirList;

	//监控目录列表
	private Map<String, ImmutableMap<String, String>> dir2Properties = new HashMap<String, ImmutableMap<String, String>>();

	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	public void configure(Context context)
	{
		logger.info("");
		positionFile = context.getString("positionFile");
		final String watchDirs = context.getString("watchDirs");
		final String[] dirArray = watchDirs.split(",");
		for (String dir : dirArray)
		{
			final ImmutableMap<String,String> subProperties = context.getSubProperties("watchDirs." + dir + ".");
			String path = subProperties.get("path");
			if(Strings.isNullOrEmpty(path) || "/".equals(path))
			{
				return;
			}
			dir2Properties.put(path, subProperties);
		}
	}

	@Override
	public synchronized void start()
	{
		super.start();
		PositionFileManager positionFileManager = null;
		try {
			positionFileManager = new PositionFileManager(positionFile, Lists.newArrayList(dir2Properties.keySet()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		LogReaderWorker logReaderWorker = new LogReaderWorker(positionFileManager,getChannelProcessor());
		LogWatcher logWatcher = new LogWatcher(positionFileManager,logReaderWorker);

		/*timer.scheduleAtFixedRate(()->{


		},1000,3000, TimeUnit.MICROSECONDS);*/

		try {
			//logWatcher.reloadLogFiles();

			logReaderWorker.fireAllFileReadEvent();
			positionFileManager.syncDisk();
			logWatcher.reloadLogFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logReaderWorker.start();

	}

	@Override
	public synchronized void stop()
	{
		super.stop();
	}

	public static class Task implements Runnable
	{


		@Override
		public void run()
		{

		}
	}
}
