package darks.grid.config;

import java.io.Serializable;

public class GridConfiguration implements Serializable
{

	private static final long serialVersionUID = -7262144727436897611L;
	
	private NetworkConfig networkConfig = new NetworkConfig();
	
	private ComponentConfig componentConfig = new ComponentConfig();
	
	private String clusterName = "DEFAULT";
	
	public GridConfiguration()
	{
		
	}

	public String getClusterName()
	{
		return clusterName;
	}

	public void setClusterName(String clusterName)
	{
		this.clusterName = clusterName;
	}

    public NetworkConfig getNetworkConfig()
    {
        return networkConfig;
    }
    
    public ComponentConfig getComponentConfig()
    {
        return componentConfig;
    }

    @Override
    public String toString()
    {
        return "GridConfiguration [networkConfig=" + networkConfig + ", clusterName="
            + clusterName + "]";
    }
    
	
}
