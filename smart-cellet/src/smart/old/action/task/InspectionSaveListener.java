package smart.old.action.task;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.util.ObjectProperty;
import net.cellcloud.util.Properties;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import smart.old.action.AbstractListener;
import smart.old.api.API;
import smart.old.api.host.HostConfig;
import smart.old.api.host.HostConfigContext;
import smart.old.api.host.ServiceDeskHostConfig;
import smart.old.mast.action.Action;

public class InspectionSaveListener  extends AbstractListener{

	public InspectionSaveListener(Cellet cellet) {
		super(cellet);
	}

	@Override
	public void onAction(ActionDialect action) {
		
		HostConfig  serviceDeskConfig=new ServiceDeskHostConfig();
		HostConfigContext context=new HostConfigContext(serviceDeskConfig);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/").append(API.INSPECTIONSAVE);
		JSONObject json = null;
		String inspectionId=null;
		try {
			json = new JSONObject(action.getParamAsString("data"));
			inspectionId=json.getString("inspectionId");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		url.append("?inspectionId=").append(inspectionId);
		
		// 创建请求
		Request request = this.getHttpClient().newRequest(url.toString());
		System.out.println("保存巡检任务的URL："+url.toString());
		request.method(HttpMethod.GET);
	
		Properties params = new Properties();
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

		JSONObject jo = null;

		switch (response.getStatus()) {
		case HttpStatus.OK_200:
			byte[] bytes = response.getContent();
		
			if (null != bytes) {
				// 获取从 Web 服务器上返回的数据
				String content = new String(bytes, Charset.forName("UTF-8"));
				try {
					jo = new JSONObject(content);
					System.out.println("响应巡检任务保存数据的返回值为：:" + jo);

					// 设置参数
					params.addProperty(new ObjectProperty("data", jo));

					// 响应动作，即向客户端发送 ActionDialect
					this.response(Action.INSPECTIONSAVE, params);

				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				this.reportHTTPError(Action.INSPECTIONSAVE);
			}

			break;
		default:
			Logger.w(IncidentListListener.class,
					"返回响应码：" + response.getStatus());
			jo = new JSONObject();
			try {
				jo.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			// 设置参数
			params.addProperty(new ObjectProperty("data", jo));

			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.INSPECTIONSAVE, params);
			break;
		}
	}

}
