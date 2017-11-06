package common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by xiaofeng on 2017/11/2
 * Description:
 */
@Data
@AllArgsConstructor
public class PositionInfo
{
	private String inode;
	private String filename;
	private long offset;
}