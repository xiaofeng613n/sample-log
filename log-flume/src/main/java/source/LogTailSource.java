package source;

import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;

/**
 * Created by xiao on 2017/11/1.
 */
public class LogTailSource extends AbstractSource implements Configurable,EventDrivenSource
{
	@Override
	public void configure(Context context)
	{

	}

	@Override
	public synchronized void start()
	{
		super.start();
	}

	@Override
	public synchronized void stop()
	{
		super.stop();
	}
}
