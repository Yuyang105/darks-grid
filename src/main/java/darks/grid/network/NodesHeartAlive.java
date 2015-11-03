package darks.grid.network;

import io.netty.channel.ChannelFuture;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import darks.grid.GridRuntime;
import darks.grid.beans.GridComponent;
import darks.grid.beans.GridMessage;
import darks.grid.beans.GridNode;
import darks.grid.beans.GridNodeType;
import darks.grid.beans.meta.HeartAliveMeta;

public class NodesHeartAlive extends GridComponent
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5037403083950340100L;

	private static final Logger log = LoggerFactory.getLogger(NodesHeartAlive.class);
    
    private int expire = 600000;
    
    private int printNodesInterval = 180000;
	
    private long st = System.currentTimeMillis();
	
	public NodesHeartAlive()
	{
		
	}

	@Override
	protected void execute() throws Exception
	{
		Map<String, GridNode> nodesMap = GridRuntime.nodes().getNodesMap();
		log.info("Start to monitor nodes.size:" + nodesMap.size());
		for (Entry<String, GridNode> entry : nodesMap.entrySet())
		{
			GridNode node = entry.getValue();
			if (node.getNodeType() == GridNodeType.TYPE_LOCAL)
			{
			    node.context().getMachineInfo().update();
                continue;
			}
			if (!node.isAlive())
			{
				log.info("Grid node " + node.getId() + " " + node.context().getServerAddress() + " miss alive.");
				GridRuntime.nodes().removeNode(node);
			}
			else
			{
				checkAlive(node);
			}
		}
		if (System.currentTimeMillis() - st > printNodesInterval)
		{
			log.info(GridRuntime.nodes().getNodesInfo());
			st = System.currentTimeMillis();
		}
	}
	
	private void checkAlive(GridNode node)
	{
		boolean valid = true;
		try
		{
			HeartAliveMeta meta = new HeartAliveMeta(GridRuntime.context().getLocalNodeId(), GridRuntime.context());
			GridMessage msg = new GridMessage(meta, GridMessage.MSG_HEART_ALIVE);
			ChannelFuture future = node.getChannel().writeAndFlush(msg).sync();
			valid = future.isSuccess();
		}
		catch (Exception e)
		{
			log.error("Fail to check alive " + node + ". Cause " + e.getMessage());
			valid = false;
		}
		if (!valid)
		{
			GridRuntime.nodes().removeNode(node);
		}
	}

	public int getExpire()
	{
		return expire;
	}

	public void setExpire(int expire)
	{
		this.expire = expire;
	}

	public int getPrintNodesInterval()
	{
		return printNodesInterval;
	}

	public void setPrintNodesInterval(int printNodesInterval)
	{
		this.printNodesInterval = printNodesInterval;
	}
	
}
