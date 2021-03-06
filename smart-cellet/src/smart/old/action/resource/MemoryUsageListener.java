package smart.old.action.resource;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.util.ObjectProperty;
import net.cellcloud.util.Properties;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import smart.old.action.AbstractListener;
import smart.old.api.API;
import smart.old.api.RequestContentCapsule;
import smart.old.api.host.HostConfig;
import smart.old.api.host.HostConfigContext;
import smart.old.api.host.MonitorSystemHostConfig;
import smart.old.mast.action.Action;

/**
 * 内存利用率监听器
 * 
 * @author Lianghai Li
 */
public class MemoryUsageListener extends AbstractListener {

	public MemoryUsageListener(Cellet cellet) {
		super(cellet);
	}

	@Override
	public void onAction(ActionDialect action) {
		// 使用同步的方式进行请求
		// 注意：因为onAction方法是由Cell Cloud的action dialect进行回调的
		// 该方法独享一个线程，因此可以在此线程里进行阻塞式的调用
		// 因此，这里可以用同步的方式请求HTTP API

		// 获取参数
		JSONObject json = null;
		long moId = 0;
		int rangeInHour = 0;

		try {
			json = new JSONObject(action.getParamAsString("data"));
			moId = json.getInt("moId");
			rangeInHour = json.getInt("rangeInHour");
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		// URL
		HostConfig memConfig = new MonitorSystemHostConfig();
		HostConfigContext context = new HostConfigContext(memConfig);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/")
				.append(API.MEMORY).append("/").append(moId)
				.append("/fMemRatio/?rangeInHour=").append(rangeInHour);

		// 创建请求
		Request request = this.getHttpClient().newRequest(url.toString());
		request.method(HttpMethod.GET);

		// 填写数据内容
		DeferredContentProvider dcp = new DeferredContentProvider();
		RequestContentCapsule capsule = new RequestContentCapsule();
		capsule.append("moId", moId);
		capsule.append("rangeInHour", rangeInHour);
		dcp.offer(capsule.toBuffer());
		dcp.close();
		request.content(dcp);

		// 发送请求
		ContentResponse response = null;

		try {
			response = request.send();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}

		Properties params = new Properties();
		JSONObject data = null;

		switch (response.getStatus()) {
		case HttpStatus.OK_200:
			byte[] bytes = response.getContent();

			if (null != bytes) {

				// 获取从Web服务器返回的数据
				String content = new String(bytes, Charset.forName("UTF-8"));

				try {
					data = new JSONObject(content);
					// System.out.println("memUsage 源数据：      " + data);
					if ("success".equals(data.get("status"))) {

						if (!"".equals(data.get("dataList"))
								&& data.get("dataList") != null) {
							JSONArray ja = data.getJSONArray("dataList");
							DateFormat df = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss");

							// System.out.println("length" + ja.length());
							for (int i = 0; i < ja.length(); i++) {
								JSONObject jsonData = ja.getJSONObject(i);
								JSONArray ja1 = jsonData.getJSONArray("data");
								JSONArray ja2 = new JSONArray();

								// long memid = jsonData.getLong("mosn");
								for (int j = 0; j < ja1.length(); j++) {
									JSONArray jsonData1 = ja1.getJSONArray(j);
									JSONObject jo = new JSONObject();

									if (null == jsonData1.get(0)
											|| "".equals(jsonData1.get(0))
											|| "null".equals(jsonData1.get(0))
											|| (jsonData1.get(0)).equals(null)) {
										jo.put("usage", 0);

									} else {
										jo.put("usage", Float
												.valueOf((String) jsonData1
														.get(0)));
									}
									jo.put("collectTime",
											df.parse((String) jsonData1.get(1))
													.getTime());
									ja2.put(jo);

									// Double usedPercent = Double
									// .valueOf((String) jsonData1.get(0));
									// long timestamp = df.parse(
									// (String) jsonData1.get(1))
									// .getTime();
									//
									// if (eqType.equals("主机")) {
									// HostManager hm = HostManager
									// .getInstance();
									// hm.addMemoryDetecById(memid,
									// usedPercent, timestamp);
									// }
									// if (eqType.equals("网络设备")) {
									// NetEquipmentManager nem =
									// NetEquipmentManager
									// .getInstance();
									// nem.addMemoryDetecById(memid,
									// usedPercent, timestamp);
									//
									// }

									// System.out.println("memusage__" + ja2);

								}
								jsonData.remove("data");
								jsonData.put("usageData", ja2);
								jsonData.put("mosn", Long.parseLong(jsonData
										.getString("mosn")));
								jsonData.put("kpi", Long.parseLong(jsonData
										.getString("kpi")));
								String s = jsonData.getString("moPath");
								jsonData.put("name", s.split("> ")[1]);

								data.put("data", jsonData);
							}

							data.remove("dataList");
							// data.put("dataList", ja);
							data.put("moId", moId);
							data.put("status", 300);
							data.put("errorInfo", "");
						}
					} else {
						data.remove("dataList");
						data.put("status", 612);
						data.put("dataList", "");
						data.put("errorInfo", "未获取到内存kpi数据");
					}

					System.out.println("memUsageData：      " + data);
					// 设置参数
					params.addProperty(new ObjectProperty("data", data));
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				// 响应动作，即向客户端发送ActionDialect
				// 参数tracker是一次动作的追踪标识符
				this.response(Action.MEMORYUSAGE, params);
			} else {
				this.reportHTTPError(Action.MEMORYUSAGE);
			}
			break;
		default:
			Logger.w(MemoryUsageListener.class, "返回响应码:" + response.getStatus());

			try {
				data = new JSONObject();
				data.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// 设置参数
			params.addProperty(new ObjectProperty("data", data));

			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.MEMORYUSAGE, params);
			break;
		}
	}

}
