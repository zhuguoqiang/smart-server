package smart.old.action.resource;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

/**
 * 磁盘已用大小监听器
 * 
 * @author Lianghai Li
 */
public class DiskUsedListener extends AbstractListener {

	public DiskUsedListener(Cellet cellet) {
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
		// int currentIndex = 0;
		// int pageSize = 10;
		try {
			json = new JSONObject(action.getParamAsString("data"));
			moId = json.getInt("moId");
			rangeInHour = json.getInt("rangeInHour");
			// currentIndex = json.getInt("currentIndex");
			// pageSize = json.getInt("pageSize");
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		HostConfig cpuConfig = new MonitorSystemHostConfig();
		HostConfigContext context = new HostConfigContext(cpuConfig);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/")
				.append(API.DISKKPI).append("/").append(moId).append("/")
				.append("fUsedSize/?rangeInHour=").append(rangeInHour);

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
		switch (response.getStatus()) {

		case HttpStatus.OK_200:
			byte[] bytes = response.getContent();
			if (null != bytes) {

				// 获取从Web服务器返回的数据
				String content = new String(bytes, Charset.forName("UTF-8"));
				System.out.println("content   " + content);
				String cont = "{'dataList':[{'moPath':'LENOVO磁盘(D:)','kpiName':'使用空间','data':[['56','2013-12-25 10:50:00']],'mosn':'998000244','kpi':'40002'},{'moPath':'LENOVO磁盘(E:)','kpiName':'使用空间','data':[['95','2013-12-25 10:50:00']],'mosn':'998000245','kpi':'40002'},{'moPath':'LENOVO磁盘(C:)','kpiName':'使用空间','data':[['18','2013-12-25 10:50:00']],'mosn':'998000243','kpi':'40002'}],'status':'success'}";
				try {
					data = new JSONObject(cont);
					// System.out.println("diskUsed 源数据：" + data);
					if ("success".equals(data.get("status"))) {
						// if (!"".equals(data.get("data"))
						// && data.get("data") != null) {
						JSONArray ja = data.getJSONArray("dataList");

						DateFormat df = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss");
						JSONArray jaData = new JSONArray();

						for (int i = 0; i < ja.length(); i++) {
							JSONObject jo = new JSONObject();
							JSONObject joDisk = ja.getJSONObject(i);
							JSONArray jaUsedList = joDisk.getJSONArray("data");
							for (int j = 0; j < jaUsedList.length(); j++) {
								JSONArray jaUsed = jaUsedList.getJSONArray(j);
								for (int k = 0; k < jaUsed.length(); k++) {

									if (null == jaUsed.get(0)
											|| "".equals(jaUsed.get(0))
											|| "null".equals(jaUsed.get(0))
											|| (jaUsed.get(0)).equals(null)) {
										jo.put("used", 0);
									} else {
										jo.put("used", Long
												.parseLong((String) jaUsed
														.get(0)));
									}
									jo.put("collectTime",
											df.parse((String) jaUsed.get(1))
													.getTime());
								}
							}

							jo.put("moPath", joDisk.getString("moPath"));
							jo.put("kpiName", joDisk.getString("kpiName"));
							jo.put("kpi",
									Long.parseLong(joDisk.getString("kpi")));
							jo.put("mosn",
									Long.parseLong(joDisk.getString("mosn")));

							jaData.put(jo);
						}

						data.remove("dataList");
						data.put("dataList", jaData);
						data.put("moId", moId);
						data.put("status", 300);
						data.put("errorInfo", "");

						// } else {
						// data.remove("data");
						// data.put("status", 602);
						// data.put("config", "");
						// data.put("errorInfo", "未获取到主机配置数据");
						// }
					} else {
						data.put("status", 624);
						data.put("errorInfo", "未获取到相关kpi数据");
					}

					System.out.println("diskUsed ：      " + data);
					// 设置参数
					params.addProperty(new ObjectProperty("data", data));
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				// 响应动作，即向客户端发送ActionDialect
				// 参数tracker是一次动作的追踪标识符
				this.response(Action.DISKUSED, params);
			} else {
				this.reportHTTPError(Action.DISKUSED);
			}
			break;
		default:
			Logger.w(DiskUsedListener.class, "返回响应码:" + response.getStatus());

			try {
				data = new JSONObject();
				data.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// 设置参数
			params.addProperty(new ObjectProperty("data", data));

			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.DISKUSED, params);
			break;
		}
	}

}
