package sink;

import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xiaofeng on 2017/11/1
 * Description:
 */
public class ConsoleDebugSink extends AbstractSink implements Configurable
{
	private static final Logger logger = LoggerFactory.getLogger(ConsoleDebugSink.class);

	@Override
	public void configure(Context context)
	{
		logger.info("configure");
	}

	@Override
	public Status process() throws EventDeliveryException
	{
		Status result = Status.READY;
		Channel channel = getChannel();
		Transaction transaction = channel.getTransaction();
		Event event = null;

		try
		{
			transaction.begin();
			event = channel.take();
			if( event != null)
			{
				logger.info("EVENT:{},body:{}",event,new String(event.getBody()));
			}
			else
			{
				result = Status.BACKOFF;
			}
			transaction.commit();
		}
		catch (Exception e)
		{
			transaction.rollback();
		}
		finally {
			transaction.close();
		}
		return result;
	}
}