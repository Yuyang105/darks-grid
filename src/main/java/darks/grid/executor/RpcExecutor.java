package darks.grid.executor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import darks.grid.GridException;
import darks.grid.GridRuntime;
import darks.grid.beans.GridRpcBean;
import darks.grid.beans.MethodResult;
import darks.grid.config.MethodConfig;
import darks.grid.config.MethodConfig.ResponseType;
import darks.grid.executor.task.GridRpcTask;
import darks.grid.executor.task.TaskResultListener;
import darks.grid.executor.task.rpc.MethodJob;
import darks.grid.executor.task.rpc.MethodJobReply;
import darks.grid.executor.task.rpc.MethodRequest;
import darks.grid.utils.ReflectUtils;
import darks.grid.utils.ThreadUtils;


public class RpcExecutor extends GridExecutor
{
    
    private static final Logger log = LoggerFactory.getLogger(RpcExecutor.class);
	
	static Map<String, GridRpcBean> rpcMap = new ConcurrentHashMap<>();

    public static boolean registerMethod(Method method, Class<?> targetClass, Object targetObject)
    {
        String uniqueName = ReflectUtils.getMethodUniqueName(method);
        return registerMethod(uniqueName, method.getName(), targetClass, targetObject, method);
    }
    
    public static boolean registerMethod(String methodName, Class<?>[] types, Class<?> targetClass, Object targetObject)
    {
        Class<?> clazz = targetObject == null ? targetClass : targetObject.getClass();
        Method method = ReflectUtils.getDeepMethod(clazz, methodName, types);
        if (method == null)
            throw new GridException("Cannot find method " + methodName + " " + Arrays.toString(types));
        String uniqueName = ReflectUtils.getMethodUniqueName(method);
        return registerMethod(uniqueName, methodName, targetClass, targetObject, method);
    }

	public static boolean registerMethod(String uniqueName, Class<?> targetClass, Object targetObject)
	{
		return registerMethod(uniqueName, uniqueName, targetClass, targetObject, null);
	}
    
    public static boolean registerMethod(String uniqueName, String methodName, Class<?> targetClass, Object targetObject)
    {
        return registerMethod(uniqueName, methodName, targetClass, targetObject, null);
    }
	
	public static boolean registerMethod(String uniqueName, String methodName, Class<?> targetClass, 
	            Object targetObject, Method method)
	{
		if (!rpcMap.containsKey(uniqueName))
		{
			GridRpcBean bean = new GridRpcBean(methodName, targetClass, targetObject, method);
			rpcMap.put(uniqueName, bean);
			return true;
		}
		else
		{
			return false;
		}
	}

    public static MethodResult callMethod(String uniqueName, Object[] params, MethodConfig config) {
        Class<?>[] types = ReflectUtils.getObjectClasses(params);
        return callMethod(uniqueName, params, types, config);
    }

    public static MethodResult callMethod(String uniqueName, Object[] params, Class<?>[] types, MethodConfig config)
	{
	    if (config == null)
	        config = new MethodConfig();
	    config.fixType();
	    MethodRequest request = new MethodRequest(uniqueName, params, types, config);
	    GridRpcTask task = new GridRpcTask(request);
	    FutureTask<MethodResult> future = GridRuntime.tasks().executeTask(task);
	    if (config.getResponseType() == ResponseType.NONE)
	        return new MethodResult();
	    try
        {
	        if (config.getTimeoutSeconds() <= 0)
	            return future.get();
	        else
	            return future.get(config.getTimeoutSeconds(), TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return MethodResult.fail("Fail to call method " + uniqueName + ". Cause " + e.getMessage());
        }
	}

    public static void asyncCallMethod(String uniqueName, Object[] params, MethodConfig config,
                                       TaskResultListener<MethodResult> listener)
    {
        Class<?>[] types = ReflectUtils.getObjectClasses(params);
        asyncCallMethod(uniqueName, params, types, config, listener);
    }
    
    public static void asyncCallMethod(String uniqueName, Object[] params, Class<?>[] types, MethodConfig config,
                        TaskResultListener<MethodResult> listener)
    {
        if (config == null)
            config = new MethodConfig();
        config.fixType();
        MethodRequest request = new MethodRequest(uniqueName, params, types, config);
        GridRpcTask task = new GridRpcTask(request, listener);
        ThreadUtils.submitTask(task);
    }
	
	public static MethodJobReply executeMethod(MethodJob job)
	{
		MethodJobReply rep = new MethodJobReply(job);
		String uniqueName = job.getUniqueName();
		GridRpcBean bean = rpcMap.get(uniqueName);
		if (bean == null)
			return rep.failed(MethodJobReply.ERR_NO_METHOD, "Cannot find method " + uniqueName);
		Object obj = null;
		obj = bean.getTargetObject();
		if (obj == null)
		{
			if (bean.getTargetClass() == null)
			{
			    return rep.failed(MethodJobReply.ERR_INVALID_OBJANDCLASS, 
			        "Invalid target object and class which is null.");
			}
			obj = ReflectUtils.newInstance(bean.getTargetClass());
	        if (obj == null)
	            return rep.failed(MethodJobReply.ERR_INSTANCE_CLASS_FAIL, "Fail to instance class " + bean.getTargetClass());
		}
		try
        {
		    Method method = bean.getMethod();
		    if (method == null)
		    {
		        synchronized (bean)
                {
		            method = bean.getMethod();
		            if (method == null)
		            {
    	                Class<?>[] paramsTypes = job.getTypes();
    	                if (paramsTypes == null)
    	                    paramsTypes = ReflectUtils.getObjectClasses(job.getParams());
    	                method = ReflectUtils.getDeepMethod(obj.getClass(), bean.getMethodName(), paramsTypes);
    	                bean.setMethod(method);
		            }
                }
		    }
	        if (method == null)
                return rep.failed(MethodJobReply.ERR_GET_CLASS_METHOD, 
                    "Fail to get deep method " + bean.getMethodName() + " from " + obj.getClass() + " [" + Arrays.toString(job.getTypes()) + "]");
	        if (!method.isAccessible())
	        	method.setAccessible(true);
	        Object retObj = ReflectUtils.invokeMethod(obj, method, job.getParams());
	        rep.setResult(retObj);
	        return rep;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return rep.failed(MethodJobReply.ERR_INVOKE_EXCEPTION, 
                "Fail to invoke method " + uniqueName + ". Cause " + e.getMessage());
        }
	}
}
