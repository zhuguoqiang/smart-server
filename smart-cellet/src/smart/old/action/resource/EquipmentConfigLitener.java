package smart.old.action.resource;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.util.ObjectProperty;
import net.cellcloud.util.Properties;
import smart.old.action.AbstractListener;
import smart.old.api.API;
import smart.old.api.RequestContentCapsule;
import smart.old.api.host.HostConfig;
import smart.old.api.host.HostConfigContext;
import smart.old.api.host.MonitorSystemHostConfig;
import smart.old.mast.action.Action;

public class EquipmentConfigLitener extends AbstractListener {

	public EquipmentConfigLitener(Cellet cellet) {
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
		try {
			json = new JSONObject(action.getParamAsString("data"));
			moId = json.getInt("moId");
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		System.out.println("参数1: " + moId);

		HostConfig cpuConfig = new MonitorSystemHostConfig();
		HostConfigContext context = new HostConfigContext(cpuConfig);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/")
				.append(API.EQUIPMENTCONFIG).append("/").append(moId);

		// 创建请求
		Request request = this.getHttpClient().newRequest(url.toString());
		request.method(HttpMethod.GET);

		// 填写数据内容
		DeferredContentProvider dcp = new DeferredContentProvider();
		RequestContentCapsule capsule = new RequestContentCapsule();
		capsule.append("moId", moId);
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
		JSONObject config = null;
		switch (response.getStatus()) {
		case HttpStatus.OK_200:
			byte[] bytes = response.getContent();
			if (null != bytes) {
				// 获取从Web服务器返回的数据
				String content = new String(bytes, Charset.forName("UTF-8"));

				try {
					data = new JSONObject(content);
					// if ("success".equals(data.get("status"))) {
					if (!"".equals(data.get("data"))
							&& data.get("data") != null) {
						JSONArray ja = data.getJSONArray("data");

						config = new JSONObject();
						JSONArray jaIf = new JSONArray();
						JSONArray jaDisk = new JSONArray();
						JSONArray jaCpu = new JSONArray();
						JSONArray jaFs = new JSONArray();
						JSONArray jaMem = new JSONArray();
						// JSONArray jaCpu = new JSONArray();
						JSONArray jaBoard = new JSONArray();
						for (int i = 0; i < ja.length(); i++) {
							JSONObject jo = ja.getJSONObject(i);
							JSONArray ja1 = jo.getJSONArray("attr");
							JSONObject jsob = new JSONObject();
							for (int j = 0; j < ja1.length(); j++) {
								JSONArray ja2 = ja1.getJSONArray(j);
								String key = (String) ja2.get(0);
								Object value = ja2.get(1);

								jsob.put(key, value);

							}

							jo.remove("attr");
							jo.put("attr", jsob);

							if ("主机 ".equals(data.getString("moPath")
									.split(">")[0])) {

								if ("接口".equals(ja.getJSONObject(i).getString(
										"type"))) {
									if (jaIf.length() < 10) {
										jaIf.put(jo);
									}
									config.put("interfaceConfig", jaIf);
								} else if ("磁盘".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaDisk.length() < 10) {
										jaDisk.put(jo);
									}
									config.put("diskConfig", jaDisk);
								} else if ("CPU".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaCpu.length() < 10) {
										jaCpu.put(jo);
									}
									config.put("cpuConfig", jaCpu);
								} else if ("文件系统".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaFs.length() < 10) {
										jaFs.put(jo);
									}
									config.put("filesysConfig", jaFs);
								}

							} else if ("网络 ".equals(data.getString("moPath")
									.split(">")[0])) {

								if ("接口".equals(ja.getJSONObject(i).getString(
										"type"))) {
									if (jaIf.length() < 10) {
										jaIf.put(jo);
									}
									config.put("interfaceConfig", jaIf);
								} else if ("CPU".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaCpu.length() < 10) {
										jaCpu.put(jo);
									}
									config.put("cpuConfig", jaCpu);
								} else if ("内存".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaMem.length() < 10) {
										jaMem.put(jo);
									}
									config.put("memoryConfig", jaMem);
								} else if ("Board".equals(ja.getJSONObject(i)
										.getString("type"))) {
									if (jaBoard.length() < 10) {
										jaBoard.put(jo);
									}
									config.put("BoardConfig", jaBoard);
								}

							}

						}

						data.put("config", config);
						data.remove("data");
					}
					// } else {
					// data.put("errorInfo", "未获取到相关kpi数据");
					// }

					System.out.println("data：      " + data);
					// 设置参数
					params.addProperty(new ObjectProperty("data", data));
				} catch (JSONException e) {
					e.printStackTrace();
				}

				// 响应动作，即向客户端发送ActionDialect
				// 参数tracker是一次动作的追踪标识符
				this.response(Action.EQUIPMENTCONFIG, params);
			} else {
				this.reportHTTPError(Action.EQUIPMENTCONFIG);
			}
			break;
		default:
			Logger.w(EquipmentConfigLitener.class,
					"返回响应码:" + response.getStatus());

			try {
				data = new JSONObject();
				data.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// 设置参数
			params.addProperty(new ObjectProperty("data", data));

			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.EQUIPMENTCONFIG, params);
			break;
		}
	}
}
