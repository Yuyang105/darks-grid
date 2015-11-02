package darks.grid.network;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import darks.grid.GridRuntime;
import darks.grid.beans.meta.JoinMeta;
import darks.grid.config.GridConfiguration;
import darks.grid.utils.ThreadUtils;

public class GridNetworkCenter
{
	
	private GridMessageServer messageServer;
	
	private Map<String, Map<SocketAddress, JoinMeta>> waitJoin = new ConcurrentHashMap<String, Map<SocketAddress, JoinMeta>>();
	
	private Object mutex = new Object();
	
	private ThreadLocal<GridMessageClient> clientLocal = new ThreadLocal<>();
	
	public GridNetworkCenter()
	{
		
	}
	
	public boolean initialize(GridConfiguration config)
	{
		messageServer = GridNetworkBuilder.buildMessageServer(config);
		if (messageServer == null)
			return false;
		GridRuntime.context().setServerAddress(getBindAddress());
		GridRuntime.nodes().addLocalNode(messageServer.getChannel());
		return true;
	}
	
	public void destroy()
	{
		messageServer.destroy();
	}
	
	public boolean tryJoinAddress(InetSocketAddress address)
	{
		GridMessageClient client = clientLocal.get();
		if (client == null)
		{
			client = new GridMessageClient(ThreadUtils.getThrealPool());
			client.initialize();
			clientLocal.set(client);
		}
		return client.connect(address);
	}

	public int addWaitJoin(String nodeId, JoinMeta meta)
	{
		synchronized (mutex)
		{
			Map<SocketAddress, JoinMeta> channelMap = waitJoin.get(nodeId);
			if (channelMap == null)
			{
				channelMap = new ConcurrentHashMap<>();
				waitJoin.put(nodeId, channelMap);
			}
			meta.setJoinTime(System.currentTimeMillis());
			channelMap.put(meta.getChannel().remoteAddress(), meta);
			return channelMap.size();
		}
	}
	
	public synchronized Map<SocketAddress, JoinMeta> getWaitJoin(String nodeId)
	{
		synchronized (mutex)
		{
			Map<SocketAddress, JoinMeta> channelMap = waitJoin.get(nodeId);
			if (channelMap == null)
			{
				channelMap = new ConcurrentHashMap<>();
				waitJoin.put(nodeId, channelMap);
			}
			return channelMap;
		}
	}
	
	public InetSocketAddress getBindAddress()
	{
		if (messageServer == null)
			return null;
		return messageServer.getAddress();
	}
	
}
