package smart.old.action.task;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
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
import smart.old.api.host.ServiceDeskHostConfig;
import smart.old.mast.action.Action;

public class ProblemListListener  extends AbstractListener{

	public ProblemListListener(Cellet cellet) {
		super(cellet);
	}

	@Override
	public void onAction(ActionDialect action) {


		// 使用同步方式进行请求。
		// 因为 onAction 方法是由 Cell Cloud 的 action dialect 进行回调的，
		// 该方法独享一个线程，因此可以在此线程里进行阻塞式的调用。
		// 因此，可以用同步方式请求 HTTP API 。

		// URL
		HostConfig  serviceDeskConfig=new ServiceDeskHostConfig();
		HostConfigContext context=new HostConfigContext(serviceDeskConfig);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/").append(API.PROBLEMLIST);
		System.out.println("获取问题列表的URL:" + url.toString());
		JSONObject json = null;
		String start=null;
		String limit=null;
		String filterId=null;
		String token=null;
		try {
			 json = new JSONObject(action.getParamAsString("data"));
			 start=json.getString("currentIndex");
			 limit=json.getString("pagesize");
			 filterId=json.getString("filterId");
			 token=json.getString("token");
			 
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// 创建请求
		Request request = this.getHttpClient().newRequest(url.toString());
		request.method(HttpMethod.GET);
		
		DeferredContentProvider dcp = new DeferredContentProvider();
		RequestContentCapsule capsule = new RequestContentCapsule();
		capsule.append("start", start);
		capsule.append("limit", limit);
		capsule.append("filterId", filterId);
		capsule.append("token", token);
		dcp.offer(capsule.toBuffer());
		dcp.close();
		request.content(dcp);

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
					//仅供开发阶段测试使用，运行阶段请删除
					java.util.logging.Logger logger=java.util.logging.Logger.getLogger("smart-cellet");
					logger.info(" 问题任务列表：" + jo);

					// 设置参数
					params.addProperty(new ObjectProperty("data", jo));

					this.response(Action.PROBLEMLIST, params);

				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				this.reportHTTPError(Action.PROBLEMLIST);
			}

			break;
		default:
			Logger.w(ProblemListListener.class,	"返回响应码：" + response.getStatus());
			jo = new JSONObject();
			try {
				jo.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// 设置参数
			params.addProperty(new ObjectProperty("data", jo));

			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.PROBLEMLIST, params);
			break;
		}
	
		
	}

}
