package de.timmeey.anoBitT.peerGroup;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.timmeey.anoBitT.peerGroup.Member.PeerGroupMember;
import de.timmeey.anoBitT.tor.KeyPair;
import de.timmeey.libTimmeey.networking.communicationClient.HTTPRequestService;

public class PeerGroup {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(PeerGroup.class);

	private static final int MAX_TIMEOUT_FOR_UPDATE_REPLIES = 15;
	transient private static KeyPair keyPair;
	transient private static HTTPRequestService requestService;
	transient private static int port;

	private String name;
	private final UUID groupId;
	private Set<PeerGroupMember> members = new HashSet<PeerGroupMember>();
	transient private final Map<String, PeerGroupApplicationOffer> offers = new ConcurrentHashMap<String, PeerGroupApplicationOffer>();

	public PeerGroup(String name) {
		Preconditions.checkNotNull(keyPair);
		Preconditions.checkNotNull(requestService);
		Preconditions.checkNotNull(port);

		this.groupId = UUID.randomUUID();
		this.name = name;
		this.addMember(keyPair.getOnionAddress(), keyPair.getPublicKey());
	}

	/**
	 * Get the unique name of the group
	 * 
	 * @return the unique group name
	 */
	public UUID getUUID() {
		return this.groupId;
	}

	/**
	 * Gets the humanReadable Name of the group
	 * 
	 * @return the human readable name of the group
	 */
	public String getHumanReadableGroupName() {
		return this.name;
	}

	/**
	 * Returns all the current member
	 * 
	 * @return current member
	 */
	public Set<PeerGroupMember> getMembers() {
		return this.members;
	}

	// /**
	// * Returns the time of the last List update
	// *
	// * @return the update time
	// */
	// public long getLastUpdateTime() {
	// }

	/**
	 * Gets a specific group member by his onion address, or null if not found
	 * 
	 * @param onionAddress
	 *            the onion address of the wanted user
	 * @return the specified user or null
	 */
	public Optional<PeerGroupMember> getMember(String onionAddress) {
		return members.stream()
				.filter(m -> m.getOnionAddress().equals(onionAddress))
				.findFirst();
	}

	/**
	 * Adds a Member to a PeerGroup.
	 * 
	 * @param member
	 *            The new member
	 * @return the peerGroup with the added member
	 */
	public PeerGroup addMember(String onionAddress, PublicKey pubKey) {
		PeerGroupMember newMember = new PeerGroupMember(pubKey, onionAddress,
				this, null);
		synchronized (this) {
			LOGGER.info("Adding single group member: {}",
					newMember.getOnionAddress());
			this.members.add(newMember);
			return this;
		}

	}

	/**
	 * Updates the PeerGroup by querying other members for updated information
	 * Intended to be called while starting the application
	 * 
	 * @return A Future with the updated PeerGroup
	 */
	public PeerGroup updateGroupMembers() {
		synchronized (this) {
			LOGGER.debug("Updating groupMember");
			this.updateOnlineStats();

			List<Future<PeerGroupUpdateResponse>> memberFutureLists = members
					.stream().map(m -> getUpdatedMemberListFromMember(m))
					.collect(Collectors.toList());

			List<Set<PeerGroupMember>> memberLists = Lists.newArrayList();

			long waitUntil = System.currentTimeMillis()
					+ MAX_TIMEOUT_FOR_UPDATE_REPLIES * 1000;
			for (Future<PeerGroupUpdateResponse> updateRequest : memberFutureLists) {
				try {
					LOGGER.trace("Waiting for updare request response");
					Set<PeerGroupMember> tmpSet = updateRequest
							.get(waitUntil - System.currentTimeMillis(),
									TimeUnit.MILLISECONDS).getPeerGroup()
							.getMembers();
					memberLists.add(tmpSet);
				} catch (InterruptedException | ExecutionException
						| TimeoutException e) {
					updateRequest.cancel(true);
					LOGGER.info(
							"Could not get response from Member for new PeerGroupMember list. Exception was: {}",
							e.getMessage());
				}
			}
			this.members = resolvePossibleConflicts(memberLists);

			return this;
		}
	}

	private Future<PeerGroupUpdateResponse> getUpdatedMemberListFromMember(
			PeerGroupMember member) {
		PeerGroupUpdateRequest request = new PeerGroupUpdateRequest(member,
				this.keyPair, this.groupId);
		return this.requestService.send(request, request.getResponseType(),
				port);
	}

	/**
	 * Updates the online status of all group Members bz asking them for their
	 * IP Intended to be called on startup or periodically
	 * 
	 * @return A future with the updated peerGroup
	 */
	public PeerGroup updateOnlineStats() {
		LOGGER.debug("Updating ups for all member of group {}", name);
		members.stream().parallel().forEach(PeerGroupMember::updateIpAddress);
		return this;
	}

	/**
	 * Due to the restriction that there can't be removals of members, the most
	 * recent list is the union of all received lists
	 * 
	 * @param memberLists
	 * @return the most recent member list
	 */

	private Set<PeerGroupMember> resolvePossibleConflicts(
			List<Set<PeerGroupMember>> memberLists) {
		// return
		// memberLists.stream().max(Comparator.comparing(List::size)).get();
		Set<PeerGroupMember> newMembers = memberLists.stream()
				.flatMap(ml -> ml.stream()).collect(Collectors.toSet());
		newMembers.addAll(this.members);
		return members;
	}

	public boolean isAuthMapFromAuthorizedMember(Map<String, String> authMap) {
		boolean result = false;
		Optional<PeerGroupMember> member = this
				.getMember(authMap.get("sender"));
		if (member.isPresent()) {
			LOGGER.trace("Found member {} specified in authMap", member.get()
					.getOnionAddress());
			result = keyPair.verifyAuthMap(authMap, member.get());
		} else {
			LOGGER.info("Could not find member specified in authMap");

		}
		LOGGER.debug("isAuthMapFromAuthorizedMember resulted in {}", result);
		return result;
	}

	public PeerGroupApplicationOffer createApplicationOffer() {
		PeerGroupApplicationOffer offer = new PeerGroupApplicationOffer(180,
				this);
		System.out.println(offer.getSecretOneTimePassword());
		offers.put(offer.getSecretOneTimePassword(), offer);
		return offer;
	}

	public PeerGroupApplicationOffer getApplicationOfferForOneTimePassword(
			String oneTimePassword) {
		LOGGER.trace(String.format(
				"Looking for ApplicationOffer with key %s in group %s",
				oneTimePassword, this.groupId));
		return offers.get(oneTimePassword);
	}

	public boolean hasApplicationOfferForOneTImePassword(String oneTimePassword) {
		return offers.containsKey(oneTimePassword);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("PeerGroup [name=%s, groupId=%s, members=%s, keyPair=%s, port=%s]",
						name, groupId, members, keyPair, port);
	}

	public static void setRequestService(HTTPRequestService requestService) {
		Preconditions.checkNotNull(requestService);
		if (PeerGroup.requestService == null) {
			PeerGroup.requestService = requestService;
			PeerGroupMember.setRequestService(requestService);
		} else {
			PeerGroup.LOGGER.error("requestService was already set");
		}
	}

	public static void setKeyPair(KeyPair keyPair) {
		Preconditions.checkNotNull(keyPair);
		if (PeerGroup.keyPair == null) {
			PeerGroup.keyPair = keyPair;
			PeerGroupMember.setKeyPair(keyPair);
		} else {
			PeerGroup.LOGGER.error("keyPair was already set");
		}
	}

	public static void setPort(int port) {
		Preconditions.checkNotNull(port);
		if (PeerGroup.port == 0) {
			PeerGroup.port = port;
			PeerGroupMember.setPort(port);
		} else {
			PeerGroup.LOGGER.error("port was already set");
		}
	}
}
