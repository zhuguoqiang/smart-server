package smart.old.bean;

import smart.old.entity.AbstractEntity;

/**
 * 网络设备
 */
public class NetEquipment extends AbstractEntity {

	private static final long serialVersionUID = -6703502500894715521L;

	// 设备名
	private String name;
	// 设备类型
	private String type;
	
	public NetEquipment(long id) {
		super(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
