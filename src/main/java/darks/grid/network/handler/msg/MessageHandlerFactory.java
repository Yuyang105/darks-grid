package darks.grid.network.handler.msg;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import darks.grid.beans.GridMessage;

public final class MessageHandlerFactory
{
	
	private static final Logger log = LoggerFactory.getLogger(MessageHandlerFactory.class);
	
	private static Map<Integer, GridMessageHandler> handlersMap = new ConcurrentHashMap<Integer, GridMessageHandler>();
	
	private static Map<Integer, Constructor<? extends GridMessageHandler>> handlersClassMap = new ConcurrentHashMap<Integer, Constructor<? extends GridMessageHandler>>();
	
	static
	{
		handlersMap.put(GridMessage.MSG_JOIN, new JOIN());
		handlersMap.put(GridMessage.MSG_JOIN_REPLY, new JOIN_REPLY());
		handlersMap.put(GridMessage.MSG_HEART_ALIVE, new HEART_ALIVE());
		handlersMap.put(GridMessage.MSG_HEART_ALIVE_REPLY, new HEART_ALIVE());
		handlersMap.put(GridMessage.MSG_RPC_REQUEST, new RPC_JOB());
		handlersMap.put(GridMessage.MSG_RPC_RESPONSE, new RPC_JOB_REPLY());
	}
	
	private MessageHandlerFactory()
	{
		
	}
	
	private static void addHandlerClass(int type, Class<? extends GridMessageHandler> clazz)
	{
		try
		{
			Constructor<? extends GridMessageHandler> cst = clazz.getConstructor();
			if (cst != null)
				handlersClassMap.put(type, cst);
			else
				throw new NoSuchMethodException("Cannot find constructor of " + clazz);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
	
	public static GridMessageHandler getHandler(GridMessage message)
	{
		GridMessageHandler handler = handlersMap.get(message.getType());
		if (handler == null)
		{
			Constructor<? extends GridMessageHandler> cst = handlersClassMap.get(message.getType());
			if (cst != null)
			{
				try
				{
					handler = cst.newInstance();
				}
				catch (Exception e)
				{
					log.error(e.getMessage(), e);
				}
			}
		}
		return handler;
	}
	
}
