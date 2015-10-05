package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.codehaus.jettison.json.JSONObject;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;

public class Oauth2Handler extends BasePortalHandler {

	public Oauth2Handler() {
		setUrlFragment("office");
	}

	@Override
	public int doPost(String[] parts, HttpServletRequest req,
			HttpServletResponse res, Session session)
			throws PortalHandlerException {
		return doGet(parts, req, res, session);
	}

	@Override
	public int doGet(String[] parts, HttpServletRequest req,
			HttpServletResponse res, Session session)
			throws PortalHandlerException {
		oAuth2Response(req, res, session);

		return 1;
	}

	void oAuth2Response(HttpServletRequest req, HttpServletResponse response,
			Session session) {
		try {
			OAuthAuthzResponse oar;
			oar = OAuthAuthzResponse.oauthCodeAuthzResponse(req);
			String code = oar.getCode();

			OAuthClientRequest oAuthRequest;

			oAuthRequest = OAuthClientRequest
					.tokenLocation(
							"https://login.microsoftonline.com/727406ac-7068-48fa-92b9-c2d67211bc50/oauth2/token")
					.setGrantType(GrantType.AUTHORIZATION_CODE)
					.setClientId("9437e031-d6c8-44bb-97f8-6af3bfc24bb9")
					.setClientSecret(
							"615bqAeQSc1PuteNx794kevE3MgpfQ3Sb+1aY5Hlqsc=")
					.setRedirectURI(
							"http://sakaiwin.cloudapp.net:8080/portal/office")
					.setCode(code)
					.setParameter("resource", "https://outlook.office365.com")
					.buildBodyMessage();

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			OAuthJSONAccessTokenResponse oAuthResponse;

			oAuthResponse = oAuthClient.accessToken(oAuthRequest,
					OAuth.HttpMethod.POST);

			JsonWebSignature jws = new JsonWebSignature();

			jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
			jws.setCompactSerialization(oAuthResponse.getParam("id_token"));

			JSONObject payload = new JSONObject(jws.getPayload());

			req.setAttribute("eid", payload.get("given_name").toString());
			req.setAttribute("singleSignOn", "TRUE");

			portal.doLogin(req, response, session, null, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
