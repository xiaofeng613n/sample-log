package source;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiao on 2017/11/1.
 */
public class LogTailSource extends AbstractSource implements Configurable,EventDrivenSource
{
	private final Logger logger = LoggerFactory.getLogger(LogTailSource.class);

	private String positionFile;

	//监控目录列表
	private Map<String, ImmutableMap<String, String>> dir2Properties = Maps.newHashMap();

	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	private FileManager fileManager;

	public void configure(Context context)
	{
		logger.info("LogTailSource:configure:load conf start");
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
		logger.info("LogTailSource:configure:load conf end");
	}

	@Override
	public synchronized void start()
	{
		super.start();
		try
		{
			fileManager = new FileManager(positionFile, Lists.newArrayList(dir2Properties.keySet()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		LogReaderWorker logReaderWorker = new LogReaderWorker(getChannelProcessor(),fileManager);


		timer.scheduleAtFixedRate(()->{
			logger.info("start:");
			logReaderWorker.setReloadingFile(true);
			fileManager.reloadLogFile();
			logReaderWorker.fireAllFileReadEvent();
			logReaderWorker.setReloadingFile(false);
			logger.info("end:");

		},5,10, TimeUnit.SECONDS);


		logReaderWorker.start();

	}

	@Override
	public synchronized void stop()
	{
		super.stop();
		try
		{
			fileManager.close();
		}
		catch (IOException e)
		{

		}
	}
}
