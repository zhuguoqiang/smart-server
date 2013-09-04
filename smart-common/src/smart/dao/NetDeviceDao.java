package smart.dao;

import java.util.List;

import smart.bean.CPU;
import smart.bean.CPUPerc;
import smart.bean.Memory;
import smart.bean.MemoryDetection;
import smart.bean.NetDevice;
import smart.bean.NetInterface;
import smart.bean.NetInterfaceStat;

/**
 * 交换机DAO
 */
public interface NetDeviceDao {

	/**
	 * 返回交换机的ID列表
	 * 
	 * @return
	 */
	public List<Long> getNetDeviceIdList();

	/**
	 * 返回指定ID的交换机
	 * 
	 * @param id
	 * @return
	 */
	public NetDevice getNetDeviceById(long id);

	/**
	 * 返回交换机的列表
	 * 
	 * @return
	 */
	public List<NetDevice> getNetDevicesList();

	/**
	 * 返回指定ID的CPU列表
	 * 
	 * @param id
	 * @return
	 */
	public List<CPU> getCPUsByNdevId(long id);

	/**
	 * 返回指定ID的CPU
	 * 
	 * @param id
	 * @return
	 */
	public CPU getCPUByNdevId(long id);

	/**
	 * 返回指定CPU id的CPU利用率列表
	 * 
	 * @param id
	 * @return
	 */
	public List<CPUPerc> getPercsByNdevId(long id);

	/**
	 * 返回指定时间戳的CPU利用率
	 * 
	 * @param time
	 * @return
	 */
	public CPUPerc getCPUPercByNdevId(long id, long timestamp);

	/**
	 * 返回指定ID的内存
	 * 
	 * @param id
	 * @return
	 */
	public Memory getMemoryByNdevId(long id);

	/**
	 * 返回指定内存Id的监测信息列表
	 * 
	 * @param id
	 * @return
	 */
	public List<MemoryDetection> getMemoryDetecsByNdevId(long id);

	/**
	 * 返回指定时间戳的内存监测信息
	 * 
	 * @param id
	 * @return
	 */
	public MemoryDetection getMemoryDetecByNdevId(long id, long timestamp);

	/**
	 * 返回指定ID的网络接口列表
	 * 
	 * @param id
	 * @return
	 */

	public List<NetInterface> getNetInterfacesByNdevId(long id);

	/**
	 * 返回指定ID的网络接口
	 * 
	 * @param id
	 * @return
	 */
	public NetInterface getNetInterfaceByNdevId(long id);

	/**
	 * 返回指定接口 ID的网络接口采集信息列表
	 * 
	 * @param id
	 * @return
	 */
	public List<NetInterfaceStat> getInterfaceStatsByNdevId(long id);

	/**
	 * 返回指定时间戳的网络接口监测信息
	 * 
	 * @param id
	 * @return
	 */
	public NetInterfaceStat getInterfaceStatByNdevId(long id, long timestamp);

}