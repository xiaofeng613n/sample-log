import org.apache.flume.node.Application;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
public class LogTailSourceTest
{
	public static void main(String[] args)
	{
		String[] conf = new String[]{"-nagent1","-no-reload-conf",
				"-fD:\\IdeaSpace\\sample-log\\log-flume\\src\\test\\resources\\logTailSource-test.conf"};

		Application.main(conf);
	}
}