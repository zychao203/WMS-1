package com.ken.wms.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.ken.wms.controller.Enum.AccountStatus;

/**
 * 自定义用户授权拦截器
 * 
 * @author Ken
 *
 */
public class SecurityInterceptor implements HandlerInterceptor {

	private static Logger log = Logger.getLogger("application");

	private static final String DEFAULT_URI = "/WEB-INF/jsp/login.jsp";

	@Autowired
	private SecurityService securityService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// 获得 Session
		HttpSession session = request.getSession();

		// 获取用户的信息
		String accountStatus = (String) session.getAttribute("account_status");
		String role = (String) session.getAttribute("role");
		if (accountStatus == null || accountStatus.equals(AccountStatus.SIGNOUT.toString()) || role == null) {
			redirectToDefault(request, response);
			return false;
		}

		// 解析 URL
		String roleName = null;
		String modelName = null;
		String methodName = null;
		String url = request.getRequestURI();
		String[] strs = url.trim().split("/");
		if (strs.length == 2)
			return true;
		else if (strs.length >= 4) {
			roleName = strs[2];
			modelName = strs[3];
			methodName = strs[4];
		} else {
			log.debug("URL:" + url + ";redirect");
			redirectToDefault(request, response);
			return false;
		}
		log.debug("request URL:" + url + ";roleName:" + roleName + ";modeName:" + modelName + ";methodName:"
				+ methodName);

		// 用户权限验证
		// 登陆状态
		if (accountStatus.equals(AccountStatus.SIGNIN.toString())) {
			// role 角色验证
			if (securityService.isRole(role, roleName)) {
				String urlRequest = "/" + modelName + "/" + methodName;
				// URL 请求权限验证
				if (securityService.isAuthorise(role, urlRequest)) {
					String requestURL = url.replaceFirst("/" + roleName, "");
					// 请求重定向
					log.debug("authorise success->requestURL:" + requestURL);
					request.getRequestDispatcher(requestURL).forward(request, response);
				}
			}
		}

		redirectToDefault(request, response);
		return false;

		// log.debug("receive a new request");
		//
		// // 获得 session
		// HttpSession session = request.getSession();
		//
		// // 获取用户的账户类型以及账户状态
		// String accountStatus = (String)
		// session.getAttribute("account_status");
		// String accountType = (String) session.getAttribute("account_type");
		//
		// // 解析URL
		// String[] URI = request.getRequestURI().trim().split("/");
		// String URIRoot = URI[2];
		// String uriTarget = null;
		// if (URIRoot.equals(URI_PREFIX_SYATEMADMIN))
		// uriTarget = URI_PREFIX_SYATEMADMIN;
		// else if (URIRoot.equals(URI_PREFIX_COMMONSADMIN))
		// uriTarget = URI_PREFIX_COMMONSADMIN;
		//
		// if (accountStatus != null &&
		// accountStatus.equals(AccountStatus.SIGNIN.toString())) {
		// if (accountType != null && (uriTarget.equals(URI_PREFIX_SYATEMADMIN)
		// && accountType.equals(AccountType.SYSTEMADMIN.toString())
		// || uriTarget.equals(URI_PREFIX_COMMONSADMIN) &&
		// accountType.equals(AccountType.COMMONSADMIN.toString())))
		// return true;
		// }
		//
		// // 重定向
		// redirect(request, response);
		// return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * 重定向 判断请求是否为AJAX 或者是普通请求，采取不同的重定向方式
	 * 
	 * @param request
	 * @param response
	 */
	private void redirectToDefault(HttpServletRequest request, HttpServletResponse response) {
		// AJAX 重定向
		if (request.getHeader("x-requested-with") != null
				&& request.getHeader("x-requested-with").equalsIgnoreCase("XMLHttpRequest")) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} else {
			// 普通重定向
			try {
				request.getRequestDispatcher(DEFAULT_URI).forward(request, response);
			} catch (ServletException e) {
			} catch (IOException e) {
			}
		}
	}
}
