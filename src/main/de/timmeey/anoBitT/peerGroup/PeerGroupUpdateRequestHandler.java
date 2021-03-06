package de.timmeey.anoBitT.peerGroup;

import java.util.Optional;
import java.util.UUID;

import de.timmeey.anoBitT.tor.KeyPair;
import de.timmeey.libTimmeey.networking.communicationServer.HttpContext;
import de.timmeey.libTimmeey.networking.communicationServer.HttpHandler;
import de.timmeey.libTimmeey.networking.communicationServer.HTTPResponse.ResponseCode;

public class PeerGroupUpdateRequestHandler implements HttpHandler {
	private final PeerGroupManager peerGroupManager;

	public PeerGroupUpdateRequestHandler(PeerGroupManager peerGroupManager) {
		super();
		this.peerGroupManager = peerGroupManager;
	}

	@Override
	public HttpContext handle(HttpContext context) {

		PeerGroupUpdateRequest request = context
				.getPayload(PeerGroupUpdateRequest.class);
		UUID groupUUID = request.peerGroupUuid();
		Optional<PeerGroup> requestedGroup = peerGroupManager
				.getPeerGroupByUUID(groupUUID);

		if (requestedGroup.isPresent()) {
			boolean isAuthorized = requestedGroup.get()
					.isAuthMapFromAuthorizedMember(
							request.getAuthenticationMap());
			if (isAuthorized) {
				context.setResponse(new PeerGroupUpdateResponse(requestedGroup
						.get()));
				context.setResponseCode(ResponseCode.SUCCESS);
			} else {
				context.setResponseCode(ResponseCode.AUTH_FAILURE);
			}
		} else {
			context.setResponseCode(ResponseCode.FAILURE);

		}
		return context;
	}
}
